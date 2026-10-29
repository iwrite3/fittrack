package com.fitness.gateway;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import com.fitness.gateway.user.RegisterRequest;
import com.fitness.gateway.user.UserService;

import java.nio.charset.StandardCharsets;

@Component
@Slf4j
@RequiredArgsConstructor
public class KeycloakUserSyncFilter implements WebFilter {
    private final UserService userService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
        String token = exchange.getRequest().getHeaders().getFirst("Authorization");
        RegisterRequest registerRequest = token != null ? getUserDetails(token) : null;

        if (userId == null && registerRequest != null) {
            userId = registerRequest.getKeycloakId();
        }
        if (userId != null && token != null) {
            String finalUserId = userId;
            log.info("User sync check: keycloakId={}, email={}", finalUserId,
                    registerRequest != null ? registerRequest.getEmail() : null);

            Mono<Void> sync = userService.validateUser(finalUserId)
                    .flatMap(exist -> {
                        if (!exist && registerRequest != null) {
                            log.info("User not found in user-service, registering {}", registerRequest.getEmail());
                            return userService.registerUser(registerRequest).then();
                        }
                        log.info("User already exists (or no details to register), skipping sync");
                        return Mono.<Void>empty();
                    });

            return sync
                    .thenReturn(true)
                    .onErrorResume(e -> {
                        log.error("User sync FAILED for keycloakId={}: {}", finalUserId, e.getMessage(), e);
                        return writeError(exchange, e).thenReturn(false);
                    })
                    .flatMap(ok -> {
                        if (!ok) {
                            return Mono.<Void>empty();
                        }
                        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                                .header("X-User-ID", finalUserId)
                                .build();
                        return chain.filter(exchange.mutate().request(mutatedRequest).build());
                    });
        }
        return chain.filter(exchange);
    }

    // Returns a readable JSON error (instead of a blank 500) when the user sync fails.
    private Mono<Void> writeError(ServerWebExchange exchange, Throwable e) {
        var response = exchange.getResponse();
        response.setStatusCode(HttpStatus.BAD_GATEWAY);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String msg = String.valueOf(e.getMessage())
                .replace("\\", " ").replace("\"", "'").replace("\n", " ").replace("\r", " ");
        byte[] bytes = ("{\"error\":\"User sync failed\",\"detail\":\"" + msg + "\"}")
                .getBytes(StandardCharsets.UTF_8);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }

    private RegisterRequest getUserDetails(String token) {
        try {
            String tokenWithoutBearer = token.replace("Bearer ", "").trim();
            SignedJWT signedJWT = SignedJWT.parse(tokenWithoutBearer);
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            RegisterRequest registerRequest = new RegisterRequest();
            registerRequest.setEmail(claims.getStringClaim("email"));
            registerRequest.setKeycloakId(claims.getStringClaim("sub"));
            registerRequest.setPassword("dummy@123123");
            registerRequest.setFirstName(claims.getStringClaim("given_name"));
            registerRequest.setLastName(claims.getStringClaim("family_name"));
            return registerRequest;
        } catch (Exception e) {
            log.error("Could not read user details from token", e);
            return null;
        }
    }
}
