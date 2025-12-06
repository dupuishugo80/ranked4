# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Gateway Service Overview

The gateway service is a Spring Cloud Gateway application that acts as the API gateway for the Ranked4 microservices architecture. It handles routing, JWT authentication, CORS configuration, and request forwarding to backend services.

## Architecture

### Technology Stack
- **Framework**: Spring Boot 3.5.7 with Spring Cloud Gateway (WebFlux-based)
- **Java Version**: 21
- **Authentication**: JWT (using jjwt 0.13.0)
- **Server Port**: 8080 (external), routes to internal services

### Core Components

**Security Layer** (`src/main/java/com/ranked4/gateway/gateway_service/security/`):
- `JwtAuthenticationFilter`: Global filter that validates JWT tokens and extracts user information
  - Runs with highest priority (order = -1)
  - Bypasses public paths: `/api/auth/**`, `/ws/**`
  - Extracts `userId` and `roles` from JWT and forwards as headers (`X-User-Id`, `X-User-Roles`) to downstream services
  - Handles WebSocket upgrade requests specially (no authentication)
- `JwtService`: JWT parsing and validation service
  - Secret key configured via `jwt.secret` property
  - Extracts username, userId, and roles claims from tokens

**Configuration** (`src/main/java/com/ranked4/gateway/gateway_service/config/`):
- `GatewayConfig`: Configures global filters and CORS
  - CORS allows `http://localhost:4200` (dev) and `https://ranked4.socoolmen.me` (prod)
  - Registers JWT filter as global filter

### Service Routes (defined in application.properties)

The gateway routes requests to these backend services:
- `auth-service:8080` → `/api/auth/**`
- `userprofile-service:8080` → `/api/profiles/**`, `/api/discs/**`
- `game-service:8080` → `/api/game/**`, `/api/gifs/**`
- `game-service:8080` (WebSocket) → `/ws/**`
- `matchmaking-service:8080` → `/api/matchmaking/**`, `/api/private-matches/**`
- `shop-service:8080` → `/api/shop/**`

All routes use internal Docker network hostnames (e.g., `http://auth-service:8080`).

### Request Flow
1. Client sends request to gateway (port 8080)
2. CORS filter applies
3. JWT authentication filter runs:
   - Public paths bypass authentication
   - Protected paths require valid JWT in `Authorization: Bearer <token>` header
   - User context (`X-User-Id`, `X-User-Roles`) is injected into request headers
4. Request routed to appropriate backend service
5. Cookie headers removed via default filter

## Development Commands

### Build and Run
```bash
# Build with Maven
mvn clean package

# Run locally
mvn spring-boot:run

# Run with Docker Compose (from parent directory)
docker-compose up gateway-service

# Rebuild and run all services
docker-compose up --build
```

### Testing
```bash
# Run tests
mvn test

# Note: Test file was deleted (GatewayServiceApplicationTests.java)
# Tests need to be recreated if adding test coverage
```

## Configuration Notes

- JWT secret is stored in `application.properties` (should be externalized for production)
- Debug logging enabled for Spring Cloud Gateway and Reactor Netty
- Default filter removes `Cookie` header from all requests
- WebSocket connections bypass JWT authentication to allow game service to handle WS auth

## Working with Routes

When adding new service routes:
1. Add route configuration in `application.properties` following the pattern:
   ```properties
   spring.cloud.gateway.server.webflux.routes[N].id=service-route
   spring.cloud.gateway.server.webflux.routes[N].uri=http://service-name:8080
   spring.cloud.gateway.server.webflux.routes[N].predicates[0]=Path=/api/path/**
   ```
2. Update `JwtAuthenticationFilter.publicPaths` if the route should bypass authentication
3. Ensure the target service is configured in docker-compose.yml with correct network settings

## Security Considerations

- All protected endpoints require valid JWT tokens
- Gateway extracts and forwards user context to backend services via headers
- Backend services should trust `X-User-Id` and `X-User-Roles` headers (only gateway should set these)
- CORS is configured at gateway level; backend services don't need CORS configuration
