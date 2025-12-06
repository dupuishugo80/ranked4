# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

The `auth-service` is part of the **Ranked4** microservices architecture, a multiplayer Connect Four game platform. This service handles authentication and authorization using JWT-based access and refresh tokens.

**Technology Stack:**
- Java 21
- Spring Boot 3.5.7
- Spring Security with JWT
- PostgreSQL (database)
- Kafka (event publishing)
- Maven (build tool)

## Architecture Context

This service is one of several microservices in the Ranked4 system:
- **auth-service** (this): Port 8081 - Authentication and JWT token management
- **gateway-service**: Port 8080 - API Gateway with centralized JWT validation, adds `X-User-Id` header
- **userprofile-service**: Port 8082 - User profile and statistics
- **game-service**: Port 8083 - Game logic and WebSocket server
- **matchmaking-service**: Port 8084 - Queue management with Redis
- **shop-service**: Port 8085 - In-game shop functionality

All services share a single PostgreSQL database (`ranked4_db`) and communicate via Kafka for async events.

## Build & Run Commands

### Local Development (requires PostgreSQL and Kafka running)
```bash
mvn clean install
mvn spring-boot:run
```

### Build Only
```bash
mvn clean package
```

### Build Docker Image
```bash
docker build -t auth-service .
```

### Run with Docker Compose (from project root)
```bash
cd ..
docker-compose up --build auth-service
```

### Run Full Stack
```bash
cd ..
docker-compose up --build
```

## Key Architectural Patterns

### JWT Token Flow
1. **Registration**: User registers → User entity saved → `UserRegisteredEvent` published to Kafka topic `user-registered`
2. **Login**: Credentials validated → Access token (1h expiry) + Refresh token (7d expiry) generated
3. **Token Refresh**: Refresh token validated → New access token issued, same refresh token returned
4. **Logout**: Refresh token deleted from database

### Security Configuration
- Public endpoints: `/api/auth/**` (register, login, refresh, logout)
- JWT filter validates tokens on protected endpoints
- Stateless session management (no server-side sessions)
- BCrypt password encoding
- Password validation: min 8 chars, uppercase, lowercase, digit, special character

### Database Schema
- **User table**: id (UUID), username, email, password (encrypted), roles (JSON array)
- **RefreshToken table**: id, token (UUID), user_id (FK), expiry_date

### Kafka Integration
- Publishes `UserRegisteredEvent` to topic `user-registered` when new users register
- Event contains: userId, username, email
- Used by userprofile-service to create initial user profile

## Configuration Files

### application.properties
Key configurations:
- `server.port=8080` (internal port, mapped to 8081 externally via Docker)
- `spring.datasource.url=jdbc:postgresql://postgres:5432/ranked4_db`
- `jwt.secret` - Must match gateway-service for token validation
- `jwt.access-token-expiration=3600000` (1 hour in ms)
- `jwt.refresh-token-expiration=604800000` (7 days in ms)
- `spring.kafka.bootstrap-servers=kafka:29092`

### Docker Environment
When running in Docker:
- Profile: `docker`
- Database host: `postgres` (container name)
- Kafka host: `kafka:29092` (internal Kafka listener)
- Redis host: `redis`

## Important Implementation Details

### Package Structure
```
com.ranked4.auth.auth_service.auth
├── controller/       - REST endpoints
├── dto/             - Request/Response objects
├── model/           - JPA entities (User, RefreshToken)
├── repository/      - Spring Data JPA repos
├── security/        - JWT filter, SecurityConfig, JwtService
├── service/         - AuthService (business logic)
└── util/            - KafkaProducerConfig
```

### Critical Components

**JwtService** (`auth/security/JwtService.java`):
- Generates and validates JWT tokens
- Extracts user info from tokens
- Uses HS256 algorithm with shared secret

**AuthService** (`auth/service/AuthService.java`):
- Implements UserDetailsService for Spring Security
- Handles registration with validation
- Manages login/logout flows
- Publishes Kafka events on user registration
- One refresh token per user (old ones deleted on new login)

**SecurityConfig** (`auth/security/SecurityConfig.java`):
- Configures security filter chain
- Disables CSRF (stateless API)
- Adds JWT filter before UsernamePasswordAuthenticationFilter

**GlobalExceptionHandler** (`auth/security/GlobalExceptionHandler.java`):
- Centralized exception handling for validation errors
- Returns standardized error responses

## Testing

Currently no test files exist. When adding tests:
- Place in `src/test/java/com/ranked4/auth/auth_service/`
- Run tests: `mvn test`
- Run specific test: `mvn test -Dtest=TestClassName`

## Common Development Workflow

1. Ensure JWT secret matches between auth-service and gateway-service
2. Database schema updates via `spring.jpa.hibernate.ddl-auto=update` (auto-applied on startup)
3. Kafka must be running before starting the service (or registration will fail)
4. All services should be run via docker-compose for full functionality
5. Frontend accessible at `http://localhost:4200`
6. Gateway routes all requests, direct service access at `http://localhost:8081` for debugging

## Service Dependencies

Required before startup:
- PostgreSQL (healthy): Database for User and RefreshToken entities
- Redis (healthy): Used by other services, not directly by auth-service
- Kafka: Required for publishing UserRegisteredEvent

Services that depend on auth-service:
- gateway-service: Validates tokens, must share same JWT secret
- All other services: Indirectly via gateway authentication
