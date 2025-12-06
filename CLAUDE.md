# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Ranked4** is a multiplayer Connect Four game platform with ELO-based competitive matchmaking. Built as a microservices architecture with Spring Boot backend services, Angular frontend, real-time WebSocket gameplay, and event-driven communication via Kafka.

## System Architecture

### Microservices Overview

The system consists of 6 backend services, 1 API gateway, and 1 frontend application:

**Infrastructure Services:**
- **gateway-service** (Port 8080) - Spring Cloud Gateway with JWT validation, CORS, and request routing. Adds `X-User-Id` and `X-User-Roles` headers to authenticated requests.

**Core Services:**
- **auth-service** (Port 8081) - JWT-based authentication with access/refresh tokens, user registration, Kafka event publishing
- **userprofile-service** (Port 8082) - User profiles, ELO ratings, statistics, gold currency, disc customizations
- **game-service** (Port 8083) - Game logic, move validation, WebSocket server for real-time gameplay
- **matchmaking-service** (Port 8084) - ELO-based ranked matchmaking and private lobby system using Redis queues
- **shop-service** (Port 8085) - Product catalog and purchase transactions

**Frontend:**
- **ranked4-frontend** (Port 4200) - Angular 19 SPA with JWT authentication, WebSocket integration, and admin panel

### Supporting Infrastructure

- **PostgreSQL** (Port 5432) - Shared database `ranked4_db` for all services
- **Redis** (Port 6379) - Matchmaking queues and session management
- **Kafka + Zookeeper** (Port 9092) - Event streaming for inter-service communication
  - Topics: `user-registered`, `match.found`, `game.finished`, `player.disconnected`

## Build and Run Commands

### Full Stack Deployment

```bash
# Build and start entire application (frontend + all backend services + infrastructure)
docker-compose up --build
```

This single command builds and runs the complete Ranked4 platform. The frontend will be available at `http://localhost:4200` and the gateway at `http://localhost:8080`.

### Development Commands

```bash
# Stop all services
docker-compose down

# Clean restart (removes volumes)
docker-compose down -v && docker-compose up --build

# View logs for specific service
docker-compose logs -f game-service

# Start only infrastructure (for local service development)
docker-compose up postgres redis kafka zookeeper
```

### Individual Service Development (Java/Maven)

```bash
# Build a service
cd <service-name>
./mvnw clean package

# Run locally (requires infrastructure running)
./mvnw spring-boot:run

# Build without tests
./mvnw clean package -DskipTests

# Run tests
./mvnw test
```

### Frontend Development

```bash
cd ranked4-frontend

# Install dependencies
npm install

# Start dev server (http://localhost:4200)
npm start

# Build for production
npm run build

# Run tests
npm test
```

## Key Architectural Patterns

### Authentication Flow
1. User registers → auth-service creates user → Publishes `user-registered` event
2. userprofile-service consumes event → Creates profile with default ELO (1200) and gold (0)
3. User logs in → Receives access token (1h) + refresh token (7d)
4. All requests go through gateway → JWT validated → `X-User-Id` header added
5. Backend services trust `X-User-Id` header (gateway is the security boundary)

### Game Flow
1. Player joins matchmaking → matchmaking-service queues in Redis
2. Match found (±200 ELO) → Publishes `match.found` event to Kafka
3. game-service consumes event → Creates game → Sends WebSocket notification
4. Players connect via WebSocket → Real-time move exchange
5. Game ends → Publishes `game.finished` event
6. userprofile-service updates ELO, stats, and awards gold for ranked games

### WebSocket Architecture
- Connection: `ws://localhost:8080/ws` (proxied through gateway)
- Protocol: STOMP over WebSocket
- Game topics: `/topic/game/{gameId}`
- Matchmaking notifications, move broadcasts, game state updates

### Event-Driven Communication (Kafka)

**Published Events:**
- `user-registered` (auth-service) → userprofile-service creates profile
- `match.found` (matchmaking-service) → game-service creates game
- `game.finished` (game-service) → userprofile-service updates ELO
- `player.disconnected` → matchmaking-service removes from queue

## Technology Stack

**Backend:**
- Java 21
- Spring Boot 3.5.7
- Spring Cloud Gateway 4.3.2 (gateway only)
- Spring Security with JWT (jjwt 0.13.0)
- Spring Data JPA (PostgreSQL)
- Spring Kafka
- Spring WebSocket (STOMP)
- Maven

**Frontend:**
- Angular 19.2
- TypeScript 5.7
- RxJS 7.8
- STOMP.js for WebSocket
- jwt-decode

**Infrastructure:**
- PostgreSQL 15
- Redis 7
- Apache Kafka 7.4.0
- Docker & Docker Compose

## Database Schema

All services share the PostgreSQL database `ranked4_db`:

**auth-service tables:**
- `users` - Authentication credentials, roles
- `refresh_tokens` - Active refresh tokens

**userprofile-service tables:**
- `user_profiles` - Display names, ELO, statistics, gold
- `disc_customization` - Cosmetic items
- `user_owned_discs` - User inventory

**game-service tables:**
- `games` - Game state, grid serialization, player IDs
- `moves` - Move history

**shop-service tables:**
- `products` - Catalog items
- `purchases` - Transaction records

## Configuration Management

### Environment Profiles
Services use Spring profiles (`docker`, default for local):
- Database host: `postgres` (Docker) vs `localhost`
- Kafka brokers: `kafka:29092` (Docker) vs `localhost:9092`
- Redis host: `redis` (Docker) vs `localhost`

### Critical Shared Configuration
**JWT Secret:** Must match between auth-service and gateway-service for token validation.

### Service URLs (Docker Network)
```
gateway-service:8080
auth-service:8080 (external 8081)
userprofile-service:8080 (external 8082)
game-service:8080 (external 8083)
matchmaking-service:8080 (external 8084)
shop-service:8080 (external 8085)
```

## Development Workflow

### Typical Development Session
1. Start infrastructure: `docker-compose up postgres redis kafka zookeeper`
2. Start gateway: `cd gateway && mvn spring-boot:run`
3. Start required services (or all via docker-compose)
4. Start frontend: `cd ranked4-frontend && npm start`
5. Access app at `http://localhost:4200`

### Making Changes
- Backend changes: Rebuild and restart service (docker-compose up --build <service>)
- Frontend changes: Auto-reloads on save
- Database schema: Auto-updated via Hibernate DDL on service restart

### Testing Flow
1. Register user → auth-service → userprofile created
2. Join matchmaking → Redis queue → Match found
3. Play game → WebSocket moves → Game finishes
4. Check ELO update → userprofile-service

## Inter-Service Dependencies

```
gateway-service
  └─ (validates JWT) → all services

auth-service
  └─ Kafka: user-registered → userprofile-service

matchmaking-service
  ├─ Redis: queue storage
  ├─ WebClient → userprofile-service (fetch ELO)
  └─ Kafka: match.found → game-service

game-service
  ├─ WebSocket: real-time gameplay
  ├─ WebClient → userprofile-service (fetch display names)
  └─ Kafka: game.finished → userprofile-service

shop-service
  └─ WebClient → userprofile-service (debit gold)

userprofile-service
  └─ Kafka consumers: user-registered, game.finished
```

## Common Debugging Commands

```bash
# Check service health
docker-compose ps

# View Kafka topics
docker exec -it kafka_broker kafka-topics --list --bootstrap-server localhost:9092

# Connect to PostgreSQL
docker exec -it postgres_db psql -U ranked4user -d ranked4_db

# Check Redis queue
docker exec -it redis_cache redis-cli
> ZRANGE matchmaking_queue_ranked4 0 -1 WITHSCORES

# Follow specific service logs
docker-compose logs -f --tail=100 game-service

# Rebuild single service without cache
docker-compose build --no-cache game-service
```

## API Gateway Routes

All frontend requests go through gateway at `http://localhost:8080`:

- `/api/auth/**` → auth-service (public)
- `/api/profiles/**` → userprofile-service
- `/api/discs/**` → userprofile-service
- `/api/game/**` → game-service
- `/api/gifs/**` → game-service
- `/api/matchmaking/**` → matchmaking-service
- `/api/private-matches/**` → matchmaking-service
- `/api/shop/**` → shop-service
- `/ws/**` → game-service (WebSocket, no auth)

## Security Model

- JWT tokens issued by auth-service, validated by gateway-service
- Access tokens expire in 1 hour, refresh tokens in 7 days
- Gateway adds trusted headers: `X-User-Id`, `X-User-Roles`
- Backend services do NOT validate JWTs (trust gateway)
- Admin operations check for `ROLE_ADMIN` in `X-User-Roles` header
- WebSocket connections bypass gateway JWT filter (game-service handles WS auth)

## ELO System

- Starting ELO: 1200
- K-factor: 30
- Matchmaking range: ±200 ELO
- Only ranked games affect ELO
- Standard ELO formula: `newELO = oldELO + K * (actualScore - expectedScore)`

## Gold Economy

- Win: 30 gold (ranked games only)
- Draw: 15 gold (ranked games only)
- Loss: 0 gold
- Spent on shop items and customizations

## Important Notes

- All services use stateless session management
- Database schema auto-updates via Hibernate (ddl-auto=update)
- Kafka topics are created automatically on first message
- Frontend assumes gateway is at localhost:8080
- CORS configured at gateway for localhost:4200 and production domain
- Container names in docker-compose are used as DNS hostnames
- Refresh tokens are single-use (old token deleted on new login)
- Private match lobbies expire after 30 minutes in Redis
- Matchmaking queue entries auto-cleaned after 5 minutes
- Game grid stored as 42-character string (0=empty, 1=player1, 2=player2)
