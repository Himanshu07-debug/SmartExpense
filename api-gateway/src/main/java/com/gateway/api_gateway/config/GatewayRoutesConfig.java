package com.gateway.api_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

import java.net.URI;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.filter.FilterFunctions.removeRequestHeader;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path;

@Configuration
public class GatewayRoutesConfig {

    @Bean
    public RouterFunction<ServerResponse> customGatewayRoutes() {
        return RouterFunctions.route()
                .add(route("auth-route")
                        .route(path("/auth/v1/**"), http())
                        .before(uri(URI.create("http://localhost:8080")))
                        .build())
                .add(route("user-route")
                        .route(path("/user/v1/**"), http())
                        .before(uri(URI.create("http://localhost:8081")))
                        .build())
                .add(route("expense-route")
                        .route(path("/expense/v1/**"), http())
                        .before(uri(URI.create("http://localhost:8082")))
                        .build())
                .add(route("ds-route")
                        .route(path("/ds/v1/**"), http())
                        .before(uri(URI.create("http://localhost:8010")))
                        .filter(removeRequestHeader("Upgrade"))
                        .filter(removeRequestHeader("HTTP2-Settings"))
                        .build())
                .build();
    }
}