export const API_BASE_URL = 'http://localhost:8080/api';

export const WEBSOCKET_URL = 'ws://localhost:8080/ws';

export const API_ENDPOINTS = {
  AUTH: `${API_BASE_URL}/auth`,
  PROFILES: `${API_BASE_URL}/profiles`,
  MATCHMAKING_JOIN: `${API_BASE_URL}/matchmaking/join`,
  MATCHMAKING_LEAVE: `${API_BASE_URL}/matchmaking/leave`,
  PRIVATE_MATCHES: `${API_BASE_URL}/private-matches`,
  PRIVATE_MATCHES_JOIN: `${API_BASE_URL}/private-matches/join`,
  PRIVATE_MATCHES_START: `${API_BASE_URL}/private-matches/start`,
  PRIVATE_MATCHES_LOBBY: `${API_BASE_URL}/private-matches`,
} as const;
