package com.ranked4.gateway.gateway_service.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    @Autowired
    private JwtService jwtService;

    private static final List<String> publicPaths = List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh",
            "/api/auth/logout",
            "/ws/**"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (isWebSocketUpgrade(request)) {
            return chain.filter(exchange);
        }

        boolean isPublicPath = publicPaths.stream().anyMatch(path::startsWith);
        if (isPublicPath) {
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return this.onError(exchange, HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(7);

        if (!jwtService.validateToken(token)) {
            return this.onError(exchange, HttpStatus.UNAUTHORIZED);
        }

        String userId = jwtService.getUserIdFromToken(token);

        String userIdHeader = (userId != null) ? userId : null;

        if (userIdHeader == null) {
            return this.onError(exchange, HttpStatus.UNAUTHORIZED);
        }

        List<String> roles = jwtService.getRolesFromToken(token);

        String rolesHeader = roles.stream().collect(Collectors.joining(","));

        ServerHttpRequest modifiedRequest = request.mutate()
                .header("X-User-Id", userIdHeader)
                .header("X-User-Roles", rolesHeader)
                .build();

        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }

    private boolean isWebSocketUpgrade(ServerHttpRequest request) {
        String upgrade = request.getHeaders().getFirst("Upgrade");
        String connection = request.getHeaders().getFirst("Connection");
        
        return "websocket".equalsIgnoreCase(upgrade) && 
               connection != null && 
               connection.toLowerCase().contains("upgrade");
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -1;
    }
}