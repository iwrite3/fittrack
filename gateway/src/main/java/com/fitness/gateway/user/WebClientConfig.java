package com.fitness.gateway.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    @Bean
    @LoadBalanced
    public WebClient.Builder webClientBuilder(){
        return WebClient.builder();
    }
    @Bean
    public WebClient userServiceWebClient(WebClient.Builder webClientBuilder,
            @Value("${USER_SERVICE_URL:http://USER-SERVICE}") String userServiceUrl) {
        // local: Eureka name. Cloud Run: the https URL of user-service
        return webClientBuilder.baseUrl(userServiceUrl).build();
    }
}
