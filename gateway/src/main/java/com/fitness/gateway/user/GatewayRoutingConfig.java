package com.fitness.gateway.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutingConfig {

    @Bean
    // Locally the defaults (lb://...) use Eureka. On Cloud Run the *_SERVICE_URL variables hold the https URLs.
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder,
            @Value("${ACTIVITY_SERVICE_URL:lb://activity-service}") String activityUrl,
            @Value("${USER_SERVICE_URL:lb://user-service}") String userUrl,
            @Value("${AI_SERVICE_URL:lb://ai-service}") String aiUrl) {
        return builder.routes()
                // Explicitly route to Activity Service via Eureka
                .route("activity-service", r -> r.path("/api/activities", "/api/activities/**")
                        .uri(activityUrl))

                // Explicitly route to User Service via Eureka
                .route("user-service", r -> r.path("/api/users/**")
                        .uri(userUrl))

                // Explicitly route to AI Service via Eureka
                .route("ai-service", r -> r.path("/api/recommendations/**")
                        .uri(aiUrl))
                .build();
    }
}