# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is the **userprofile-service** microservice for Ranked4, a Connect Four game with ELO ranking. It manages user profiles, statistics, ELO ratings, and customization items.

Part of a larger microservices architecture including:
- auth-service (JWT authentication)
- game-service (game logic, WebSocket)
- matchmaking-service (queue management)
- shop-service (in-game purchases)
- gateway-service (API gateway with JWT validation)
- ranked4-frontend (Angular 19)

## Technology Stack

- Java 21
- Spring Boot 3.5.7
- PostgreSQL (shared database: ranked4_db)
- Apache Kafka (event-driven communication)
- Docker
- Maven build system

## Build and Run Commands

### Local Development
```bash
# Build the project
./mvnw clean package

# Run without tests
./mvnw clean package -DskipTests

# Run the application locally
./mvnw spring-boot:run

# Run tests (when available)
./mvnw test
```

### Docker
```bash
# Build Docker image
docker build -t userprofile-service .

# Run entire stack from parent directory
cd ..
docker-compose up --build

# Run only userprofile-service
docker-compose up userprofile-service
```

The service runs on port 8080 (mapped to 8082 externally in docker-compose).

## Architecture

### Security Model
- No internal authentication (delegates to gateway-service)
- Receives `X-User-Id` header from gateway after JWT validation
- Spring Security configured with CSRF disabled and stateless sessions
- Admin endpoints require `X-User-Roles` header containing "ROLE_ADMIN"

### Event-Driven Design
The service consumes Kafka events and updates profiles accordingly:

1. **user.registered** → Creates new user profile with default values (ELO: 1200, Gold: 0)
2. **game.finished** → Updates ELO, statistics, and gold rewards for ranked games

### Core Domain Model

**UserProfile** entity tracks:
- User identification (userId UUID from auth-service)
- Display name and avatar
- ELO rating (default 1200)
- Game statistics (wins, losses, draws, gamesPlayed)
- Gold currency
- Disc customizations (owned and equipped)

**DiscCustomization** entity for cosmetic items that users can purchase and equip.

### Key Components

**Controllers:**
- `UserProfileController`: REST endpoints for profile access, leaderboard, admin operations
- `DiscCustomController`: Customization management

**Services:**
- `UserProfileService`: Profile CRUD, leaderboard, gold transactions, disc management
- `EloCalculService`: ELO calculation using K-factor of 30, processes game.finished events
- `DiscCustomService`: Disc customization operations

**Kafka Consumers:**
- `KafkaConsumerService`: Handles user.registered events
- `EloCalculService`: Handles game.finished events (only for ranked games)

### Database
Shared PostgreSQL database (ranked4_db) with tables:
- user_profiles
- disc_customization
- user_owned_discs (junction table)

JPA configured with `ddl-auto=update` for automatic schema updates.

## Key Endpoints

- `GET /api/profiles/me` - Get current user profile (requires X-User-Id)
- `GET /api/profiles/{userId}` - Get any user profile
- `GET /api/profiles/leaderboard` - Top 10 players by ELO
- `GET /api/profiles/adminUserList` - Paginated user list (admin only)
- `POST /api/profiles/fullprofilesbyids` - Batch profile retrieval
- `POST /api/profiles/debit-gold` - Debit gold from user account
- `GET /api/disccustom/all` - List all disc customizations
- `POST /api/disccustom/add-to-user` - Add disc to user inventory

## Important Conventions

### Event Processing
- Only ranked games (origin="RANKED") update ELO and statistics
- Gold rewards: 30 on win, 15 on draw
- ELO uses standard formula with K-factor=30

### Error Handling
- `GlobalExceptionHandler` centralizes exception handling
- Returns appropriate HTTP status codes (404 for not found, 402 for insufficient funds, 403 for access denied)

### Data Transfer
- Separate DTOs for different use cases:
  - `UserProfileDTO`: Public profile view
  - `MyUserProfileDTO`: Extended view with owned items (for profile owner)
  - `LeaderboardEntryDTO`: Includes rank calculation

### Inter-Service Communication
- WebClient (WebFlux) used for synchronous HTTP calls to other services
- Kafka for asynchronous event-driven updates
- Services communicate via internal Docker network (app-network)

## Environment Configuration

Default local development uses Docker network hostnames:
- Database: `postgres:5432`
- Kafka: `kafka:29092`

Override with environment variables or application.properties for different environments.

## Connection to Wider System

This service is typically accessed through the gateway-service at `http://localhost:8080` which handles:
- JWT token validation
- Adding X-User-Id and X-User-Roles headers
- Routing to internal services

Direct access in Docker: `http://userprofile-service:8080`
