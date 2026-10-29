package com.fitness.gateway.user;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutingConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Explicitly route to Activity Service via Eureka
                .route("activity-service", r -> r.path("/api/activities", "/api/activities/**")
                        .uri("lb://activity-service"))

                // Explicitly route to User Service via Eureka
                .route("user-service", r -> r.path("/api/users/**")
                        .uri("lb://user-service"))

                // Explicitly route to AI Service via Eureka
                .route("ai-service", r -> r.path("/api/recommendations/**")
                        .uri("lb://ai-service"))
                .build();
    }
}