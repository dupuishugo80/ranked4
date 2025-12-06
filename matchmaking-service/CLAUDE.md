# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Spring Boot matchmaking service for the Ranked4 gaming platform. Handles both ranked ELO-based matchmaking and private 1v1 lobbies using Redis queues and Kafka event streaming.

## Build & Run

```bash
# Build
mvn clean package

# Build (skip tests)
mvn clean package -DskipTests

# Run locally
mvn spring-boot:run

# Docker build (multi-stage)
docker build -t matchmaking-service .
```

## Architecture

### Core Components

**MatchmakingService** - ELO-based ranked matchmaking
- Uses Redis sorted set (`matchmaking_queue_ranked4`) to store players by ELO rating
- Matches players within ±200 ELO range
- Fetches user profiles from user-profile-service via WebClient
- Publishes `MatchFoundEvent` to Kafka topic `match.found`
- Scheduled cleanup removes queue entries older than 5 minutes
- Listens to `player.disconnected` Kafka topic to remove disconnected players

**PrivateMatchService** - Private lobby system
- Generates unique 8-character alphanumeric lobby codes
- Stores lobby state in Redis with 30-minute TTL
- Host creates lobby, guest joins with code, host starts match
- Lobbies stored as `private_match_lobby:{code}` in Redis
- User-to-lobby mapping via `user_private_lobby:{userId}`

### External Dependencies

- **Redis** (`redis:6379`) - Queue storage for matchmaking and lobby state
- **Kafka** (`kafka:29092`) - Event streaming for match coordination
  - Produces: `match.found` topic with `MatchFoundEvent`
  - Consumes: `player.disconnected` topic with `PlayerDisconnectEvent`
- **user-profile-service** (`http://user-profile-service:8082`) - Fetches user ELO ratings

### Data Flow

1. Player joins queue → Fetch ELO from user-profile-service
2. Search Redis sorted set for compatible opponent (±200 ELO)
3. If match found → Remove both from queue, publish to Kafka
4. If no match → Add to queue with ELO as score

### Key Configuration

All services are Docker hostnames (redis, kafka, user-profile-service). When running locally, update `application.properties`:
- `spring.data.redis.host=localhost`
- `spring.kafka.producer.bootstrap-servers=localhost:9092`
- `user.profile.service.url=http://localhost:8082`

### REST Endpoints

**Ranked Matchmaking:**
- `POST /api/matchmaking/join` - Join ranked queue (requires `X-User-Id` header)
- `POST /api/matchmaking/leave` - Leave ranked queue

**Private Matches:**
- `POST /api/private-matches` - Create lobby (returns code)
- `POST /api/private-matches/join` - Join lobby by code
- `POST /api/private-matches/start` - Start match (host only)
- `GET /api/private-matches/{code}` - Get lobby status

### Package Structure

Base package: `com.ranked4.matchmaking.matchmaking_service`
- `controller/` - REST controllers
- `service/` - Business logic (MatchmakingService, PrivateMatchService)
- `dto/` - Data transfer objects and Kafka events
- `config/` - Redis, WebClient configuration
- `util/` - Kafka producer/consumer configs, event listeners
