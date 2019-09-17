package io.hardt.k8s.util;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class RouteConfig {

    @Bean
    public RouterFunction<ServerResponse> route(PropagationHandler indexHandler) {

        return RouterFunctions
                .route(RequestPredicates.GET("/"), indexHandler::index)
                .andRoute(RequestPredicates.GET("/eoj"), indexHandler::eoj);


    }

}
