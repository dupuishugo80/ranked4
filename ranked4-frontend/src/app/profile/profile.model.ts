export interface UserProfile {
  userId: any;
  id: string;
  displayName: string;
  email: string;
  avatarUrl: string | null;
  elo: number;
  gamesPlayed: number;
  wins: number;
  losses: number;
  draws: number;
  gold: number;
  disc: DiscCustomization | null;
  equippedDisc?: DiscCustomization | null;
  ownedDiscs?: DiscCustomization[];
  createdAt?: string;
  updatedAt?: string;
}

export interface LeaderboardEntry {
  userId: string;
  displayName: string;
  avatarUrl: string | null;
  elo: number;
  wins: number;
  losses: number;
  draws: number;
  rank: number;
}

export interface DiscCustomization {
  itemCode?: string;
  displayName?: string;
  type: 'color' | 'image';
  value: string;
  price?: number | null;
  availableForPurchase?: boolean;
}

export interface GameHistoryItem {
  gameId: string;
  playerOneId: string;
  playerOneName: string;
  playerTwoId: string;
  playerTwoName: string;
  winner: 'PLAYER_ONE' | 'PLAYER_TWO' | null;
  finishedAt: string;
  ranked: boolean;
  origin: string;
  aiDifficulty: number | null;
}

export interface GameHistoryResponse {
  content: GameHistoryItem[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
}
