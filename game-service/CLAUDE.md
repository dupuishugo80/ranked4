# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is the **game-service** for Ranked4, a Connect Four (Puissance 4) multiplayer game. It manages game state, validates moves, detects win conditions, and communicates with players in real-time via WebSocket. Part of a microservices architecture with Kafka messaging.

**Base package:** `com.ranked4.game.game_service`
**Port:** 8080 (inside container), exposed as 8083 externally
**Java version:** 21
**Spring Boot version:** 3.5.7

## Build & Run Commands

### Local Development
```bash
# Build (from game-service directory)
./mvnw clean package

# Run locally
./mvnw spring-boot:run

# Build without tests
./mvnw clean package -DskipTests
```

### Docker
```bash
# Build and run entire stack (from parent directory)
docker-compose up --build

# Build only game-service
docker-compose build game-service

# Rebuild game-service and restart
docker-compose up --build game-service

# View logs
docker-compose logs -f game-service
```

## Architecture

### Core Components

**GameBoard (`model/GameBoard.java`)**
- Pure game logic (6x7 grid, 4-in-a-row win detection)
- Serializes/deserializes grid state to/from string for persistence
- Stateless logic - all state stored in `Game` entity

**Game (`model/Game.java`)**
- JPA entity persisting game state to PostgreSQL
- Contains `GameBoard` instance, player IDs, timestamps
- Manages game lifecycle (IN_PROGRESS, FINISHED)
- Handles forfeit logic

**GameService (`service/GameService.java`)**
- Core business logic: create games, apply moves, detect game end
- Publishes `GameFinishedEvent` to Kafka topic `game.finished`
- Fetches player info from `userprofile-service` via REST for display names
- Handles ranked/unranked games and forfeit scenarios

**GameSocketController (`controller/GameSocketController.java`)**
- WebSocket endpoint handlers for real-time gameplay
- `@MessageMapping("/game.move/{gameId}")` - processes player moves
- `@MessageMapping("/game.join/{gameId}")` - registers player connections
- Broadcasts game updates to `/topic/game/{gameId}`

**KafkaService (`util/KafkaService.java`)**
- Consumes `match.found` events from matchmaking-service
- Creates new games when matches are found
- Implements 10-second grace period for ranked games (no-show detection)
- Sends initial game state to WebSocket subscribers

### Communication Flow

1. **Game Creation:** Matchmaking → Kafka (`match.found`) → KafkaService → GameService → WebSocket broadcast
2. **Gameplay:** Client → WebSocket → GameSocketController → GameService → WebSocket broadcast
3. **Game End:** GameService → Kafka (`game.finished`) → userprofile-service (ELO update)

### Key Dependencies

- **PostgreSQL:** Game and move persistence
- **Kafka:** Event-driven communication with other services
- **WebSocket:** Real-time bidirectional communication with frontend
- **RestTemplate:** Sync calls to userprofile-service for player info

## Configuration

**Database connection** (`application.properties`):
- Uses PostgreSQL at `postgres:5432/ranked4_db` (Docker network)
- Hibernate DDL auto-update enabled (`spring.jpa.hibernate.ddl-auto=update`)

**Kafka** (`application.properties`):
- Consumer group: `game-service-group`
- Bootstrap servers: `kafka:29092` (Docker network)
- Topics consumed: `match.found`
- Topics produced: `game.finished`

**WebSocket** (`util/WebSocketConfig.java`):
- Endpoint: `/ws`
- STOMP destinations: `/app/game.*`, `/topic/game/*`

## Data Model

**Game Entity:**
- Stores serialized grid state (42-character string: 0=EMPTY, 1=PLAYER_ONE, 2=PLAYER_TWO)
- Tracks both players' UUIDs
- `ranked` boolean flag for ELO-affecting games
- `origin` field (RANKED, UNRANKED, FRIEND, CANCELLED_NO_SHOW)

**Move Entity:**
- Historical record of each move (column, player, move number)
- Used for game replay and auditing

## Testing

No test files currently exist in this service.

## Important Notes

- Game logic is deterministic and validated server-side
- WebSocket sessions tracked via `GameSessionRegistry` to detect disconnections
- Ranked games cancelled if both players don't connect within 10 seconds
- All Kafka messages use JSON serialization with `spring.json.trusted.packages=*`
- Service depends on userprofile-service availability for player display names
