package io.hardt.k8s.util;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;


@Configuration
@ConfigurationProperties(prefix = "io.hardt.propagationsidecar")
public class PropagationSidecarConfig {

    private List<String> headersViaSecret;
    private Map<String, String> headers;
    private Map<String, String> params;
    private int sequenceNumber;
    private String siblingHost = "127.0.0.1";
    private int siblingPort = 8080;
    private String siblingPath;
    private String siblingScheme = "http";


    public List<String> getHeadersViaSecret() {
        return headersViaSecret;
    }

    public void setHeadersViaSecret(List<String> headersViaSecret) {
        this.headersViaSecret = headersViaSecret;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public String getSiblingHost() {
        return siblingHost;
    }

    public void setSiblingHost(String siblingHost) {
        this.siblingHost = siblingHost;
    }

    public int getSiblingPort() {
        return siblingPort;
    }

    public void setSiblingPort(int siblingPort) {
        this.siblingPort = siblingPort;
    }

    public String getSiblingPath() {
        return siblingPath;
    }

    public void setSiblingPath(String siblingPath) {
        this.siblingPath = siblingPath;
    }

    public String getSiblingScheme() {
        return siblingScheme;
    }

    public void setSiblingScheme(String siblingScheme) {
        this.siblingScheme = siblingScheme;
    }

    public PropagationSidecarPojo toPojo() {
        return new PropagationSidecarPojo(getHeadersViaSecret(), getHeaders(), getParams(), getSequenceNumber());
    }



}
