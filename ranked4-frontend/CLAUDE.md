# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is the **ranked4-frontend** Angular application for Ranked4, a Connect Four game with ELO ranking. It provides the user interface for authentication, matchmaking, gameplay, leaderboard, and administration.

Part of a larger microservices architecture including:

- auth-service (JWT authentication)
- game-service (game logic, WebSocket)
- matchmaking-service (queue management)
- shop-service (in-game purchases)
- userprofile-service (profiles, ELO, statistics)
- gateway-service (API gateway at localhost:8080)

## Technology Stack

- Angular 19.2
- TypeScript 5.7
- SCSS for styling
- RxJS for reactive programming
- STOMP/WebSocket for real-time game communication
- Karma/Jasmine for testing
- JWT token authentication

## Build and Run Commands

### Development

```bash
# Install dependencies
npm install

# Start development server (http://localhost:4200)
npm start
# or
ng serve

# Build for production
npm run build
# or
ng build

# Watch mode for development
npm run watch

# Run unit tests
npm test
# or
ng test
```

### Angular CLI Commands

```bash
# Generate a new component
ng generate component component-name

# Generate a service
ng generate service service-name

# Generate a guard
ng generate guard guard-name

# See all available schematics
ng generate --help
```

## Architecture

### Authentication Flow

- JWT-based authentication with access/refresh token pattern
- Tokens stored in localStorage
- `LoginService` handles login/logout and token refresh
- `authInterceptor` automatically adds Bearer token to requests
- Automatic token refresh on 401 errors (except for /api/auth/ endpoints)
- `authGuard` protects authenticated routes
- `adminGuard` protects admin routes (checks for ROLE_ADMIN)
- `publicGuard` redirects authenticated users away from login/register

### API Configuration

All backend communication goes through the gateway at `http://localhost:8080/api`.

API endpoints are centralized in `src/app/core/config/api.config.ts`:

- AUTH: `/api/auth`
- PROFILES: `/api/profiles`
- MATCHMAKING_JOIN/LEAVE: `/api/matchmaking/*`
- PRIVATE_MATCHES: `/api/private-matches/*`
- GAME: `/api/game`
- SHOP: `/api/shop`

WebSocket connection: `ws://localhost:8080/ws`

### WebSocket Architecture

Real-time game communication uses STOMP over WebSocket (`@stomp/stompjs`):

**WebSocketService** (`src/app/game/websocket/websocket.service.ts`):

- Manages STOMP client lifecycle
- Handles connection states: DISCONNECTED, CONNECTING, CONNECTED
- Auto-reconnect with 5-second delay
- Provides `subscribeToTopic()` for subscribing to WebSocket topics
- Provides `publishMessage()` for sending messages

Connection state is exposed as Observable for components to react to connection changes.

### Directory Structure

```
src/app/
├── admin/              # Admin panel components
│   ├── admin-lootboxes/
│   ├── admin-skin/
│   └── admin-users/
├── core/
│   └── config/         # API and configuration constants
├── game/               # Game-related components
│   ├── game/           # Main game component
│   ├── gif/            # GIF display service
│   ├── matchmaking/    # Matchmaking queue
│   ├── private-game/   # Private game lobby
│   └── websocket/      # WebSocket service
├── game-history/       # Game history display
├── guards/             # Route guards (auth, admin, public)
├── home/               # Home/dashboard component
├── interceptors/       # HTTP interceptors (auth)
├── leaderboard/        # Leaderboard component
├── profile/            # User profile components
└── security/           # Authentication components
    ├── login/
    └── register/
```

### Route Structure

Routes are defined in `src/app/app.routes.ts`:

**Public routes** (publicGuard):

- `/login` - Login page
- `/register` - Registration page

**Authenticated routes** (authGuard):

- `/home` - Home/dashboard
- `/matchmaking` - Matchmaking queue
- `/game/:id` - Active game by ID
- `/private-game` - Private game lobby

**Admin routes** (adminGuard):

- `/admin` - Admin dashboard
- `/admin-users` - User management
- `/admin-lootboxes` - Lootbox management
- `/admin-skins` - Skin management

### Token Management

`LoginService` provides:

- `login(payload)` - Authenticate and store tokens
- `logout()` - Clear tokens and redirect to login
- `getAccessToken()` / `getRefreshToken()` - Retrieve tokens from localStorage
- `isAuthenticated()` - Check if user has valid access token
- `isAdmin()` - Check if user has ROLE_ADMIN (decoded from JWT)
- `getUserId()` - Extract userId from JWT payload
- `handle401Error()` - Automatic token refresh with request retry

JWT payload structure:

```typescript
{
  sub: string;        // username
  userId: string;     // UUID
  roles: string[];    // ["ROLE_USER", "ROLE_ADMIN"]
  iat: number;        // issued at
  exp: number;        // expiration
}
```

### HTTP Interceptors

`authInterceptor` (`src/app/interceptors/auth.interceptor.ts`):

- Adds `Authorization: Bearer <token>` header to all requests
- Catches 401 errors and triggers token refresh flow
- Skips refresh for `/api/auth/` endpoints to prevent infinite loops
- Queues concurrent requests during token refresh

## Styling

- Component styles use SCSS (configured in angular.json)
- Global styles in `src/styles.scss`
- Style budget limits:
  - Initial bundle: 500kB warning, 1MB error
  - Component styles: 4kB warning, 8kB error

## Development Workflow

### Typical Development Tasks

1. Start backend services: `docker-compose up` (from parent directory)
2. Start frontend: `npm start` (runs on http://localhost:4200)
3. Frontend automatically proxies API calls to gateway at localhost:8080
4. Make changes - Angular auto-reloads on file changes

### Common Patterns

- Services are provided at root level (`providedIn: 'root'`)
- Use functional guards (CanActivateFn) instead of class-based guards
- Use functional interceptors (HttpInterceptorFn) instead of class-based
- Inject dependencies with `inject()` function in guards/interceptors

## Integration with Backend

The frontend communicates with the backend microservices through:

1. **REST API** (via gateway-service):
   - All HTTP requests go to `http://localhost:8080/api`
   - Gateway validates JWT and routes to appropriate microservice
   - Gateway adds `X-User-Id` and `X-User-Roles` headers for backend services

2. **WebSocket** (via gateway-service):
   - Real-time game updates via STOMP
   - Connection: `ws://localhost:8080/ws`
   - Used for matchmaking notifications and live gameplay

## Docker Integration

Frontend can be run in Docker as part of the full stack (see parent docker-compose.yml).

Backend services in docker-compose:

- postgres (5432) - Database
- redis (6379) - Caching/sessions
- kafka (9092) - Event streaming
- gateway-service (8080) - API Gateway
- auth-service (8081)
- userprofile-service (8082)
- game-service (8083)
- matchmaking-service (8084)
- shop-service (8085)

## Important Notes

- The app uses JWT decode to extract user information client-side
- Admin features require ROLE_ADMIN in JWT token
- WebSocket reconnects automatically on disconnect
- Token refresh happens transparently when access token expires
- SCSS is the preferred styling approach (configured per component)
