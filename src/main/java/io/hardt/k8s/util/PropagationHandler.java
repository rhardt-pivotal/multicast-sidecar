package io.hardt.k8s.util;


import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.util.UriComponentsBuilder;
import org.yaml.snakeyaml.Yaml;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientResponse;

import java.net.URI;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
public class PropagationHandler
        implements ApplicationListener<EnvironmentChangeEvent> {

    @Autowired
    private PropagationSidecarConfig sidecarConfig;

    @Value("${spring.application.name}")
    private String appName;

    @Autowired
    private HttpClient httpClient;

    @Autowired
    private ApplicationContext context;

    private KubernetesClient kubeClient;

    private Expression serialNumberExpression;

    private Map<String, Expression> headerExpressionCache, paramExpressionCache;

    private ExpressionParser expressionParser;


    public PropagationHandler() {
        expressionParser = new SpelExpressionParser();
        serialNumberExpression = expressionParser.parseExpression(" io.hardt.propagationsidecar.sequenceNumber");
        kubeClient = new DefaultKubernetesClient();
        headerExpressionCache = new HashMap<>();
        paramExpressionCache = new HashMap<>();
    }





    private static final Log log = LogFactory.getLog(PropagationHandler.class);


    public Mono<ServerResponse> index(ServerRequest request) {
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromObject(sidecarConfig.toPojo()));
    }

    public Mono<ServerResponse> eoj(ServerRequest req) {
        //grab all the secret headers and put them in their secrets

        if (sidecarConfig.getHeadersViaSecret() != null){
            sidecarConfig.getHeadersViaSecret().stream().forEach(
                    headerName -> {
                        String secretName = appName+"-"+headerName;
                        req.headers().header(headerName).stream().forEach(
                                headerValue -> {
                                    Secret secret = kubeClient.secrets().withName(secretName).get();
                                    if (secret == null) {
                                        secret = new SecretBuilder().withNewMetadata()
                                                .withName(secretName)
                                                .endMetadata()
                                                .withType("generic")
                                                .build();
                                    }
                                    if(secret.getStringData() == null) {
                                        secret.setStringData(new HashMap<>());
                                    }
                                    secret.getStringData().put(headerName, headerValue);
                                    kubeClient.secrets().createOrReplace(secret);
                                }
                        );
                    }
            );
        }

        //grab the config map and deserialize to an object
        ConfigMap cm = kubeClient.configMaps().withName(appName).get();
        String yamlStr = cm.getData().get("application.yml");
        Yaml yaml = new Yaml();
        Object cmObject = yaml.load(yamlStr);


        EvaluationContext context = new StandardEvaluationContext(cmObject);
        context.getPropertyAccessors().add(new MapAccessor());

        //increment the serial number to guarantee that something changes
        int sn = Integer.parseInt( serialNumberExpression.getValue(context).toString());
        sn = sn + 1;
        serialNumberExpression.setValue(context, sn);

        //update all the headers
        sidecarConfig.getHeaders().keySet().stream().forEach(
                headerName -> {
                    req.headers().header(headerName).stream().forEach(
                            headerValue -> {
                                Expression headerExpression = buildHeaderExpression(headerName);
                                headerExpression.setValue(context, headerValue);
                            }
                    );
                }
        );

        //update all the params
        sidecarConfig.getParams().keySet().stream().forEach(
                paramName -> {
                    String paramVal = req.queryParam(paramName).get();
                    if (paramVal != null) {
                        Expression paramExpression = buildParamExpression(paramName);
                        paramExpression.setValue(context, paramVal);
                    }
                }
        );



        String newYaml = yaml.dump(cmObject);
        cm.getData().put("application.yml", newYaml);

        kubeClient.configMaps().createOrReplace(cm);

        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromObject(cmObject));

    }

    private Expression buildHeaderExpression(String headerName) {
        if(!headerExpressionCache.containsKey(headerName)){
            headerExpressionCache.put(headerName, expressionParser.parseExpression("io.hardt.propagationsidecar.headers['"+headerName+"']"));
        }
        return headerExpressionCache.get(headerName);
    }

    private Expression buildParamExpression(String paramName) {
        if(!paramExpressionCache.containsKey(paramName)){
            paramExpressionCache.put(paramName, expressionParser.parseExpression("io.hardt.propagationsidecar.params['"+paramName+"']"));
        }
        return paramExpressionCache.get(paramName);
    }

    @Override
    public void onApplicationEvent(EnvironmentChangeEvent ece) {

        //quite a bummer, at this point all the header vals aren't updated, so we
        //have to pull the config map and create a context from that.  Bummer.
        ConfigMap cm = kubeClient.configMaps().withName(appName).get();
        String yamlStr = cm.getData().get("application.yml");
        Yaml yaml = new Yaml();
        Object cmObject = yaml.load(yamlStr);
        EvaluationContext context = new StandardEvaluationContext(cmObject);
        context.getPropertyAccessors().add(new MapAccessor());




        log.info("******************** App Event:  "+ece);
        log.info("Calling sibling process on : "+
                sidecarConfig.getSiblingScheme()+
                "://"+
                sidecarConfig.getSiblingHost()+
                ":"+
                sidecarConfig.getSiblingPort()+
                sidecarConfig.getSiblingPath());

        Map<String, String> headers = new HashMap<>();

        if (sidecarConfig.getHeadersViaSecret() != null){
            sidecarConfig.getHeadersViaSecret().stream().forEach(
                    headerName -> {
                        String secretName = appName+"-"+headerName;
                        Secret s = kubeClient.secrets().withName(secretName).get();
                        headers.put(headerName, new String(Base64.getDecoder().decode(s.getData().get(headerName))));
                    }
            );
        }

        sidecarConfig.getHeaders().keySet().stream().forEach(
                headerName -> {
                    Expression headerExpression = buildHeaderExpression(headerName);
                    headers.put(headerName, headerExpression.getValue(context).toString());
                }
        );

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        sidecarConfig.getParams().keySet().stream().forEach(
                paramName -> {
                    Expression paramExpression = buildParamExpression(paramName);
                    params.put(paramName, Collections.singletonList(paramExpression.getValue(context).toString()));
                }
        );




        URI uri = UriComponentsBuilder.newInstance()
                .scheme(sidecarConfig.getSiblingScheme())
                .host(sidecarConfig.getSiblingHost())
                .port(sidecarConfig.getSiblingPort())
                .path(sidecarConfig.getSiblingPath())
                .queryParams(params).build().toUri();

        log.info("URI:  "+uri);

        HttpClientResponse resp  = httpClient.baseUrl(uri.toString()).port(sidecarConfig.getSiblingPort()).headers(
                reqHeaders -> {
                    headers.entrySet().stream().forEach(
                            entry -> reqHeaders.set(entry.getKey(), headers.get(entry.getKey()))
                    );
                }
        ).get().responseSingle((res, content) -> Mono.just(res)).block();

        log.info("GOT RESPONSE: "+resp);
        log.info("status: "+resp.status().code()+" : "+resp.status().reasonPhrase());

    }


//    public Mono<ServerResponse> updateSecret() {
//
//    }



}
