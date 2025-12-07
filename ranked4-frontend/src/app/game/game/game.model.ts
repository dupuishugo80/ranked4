export type PlayerDisc = 'PLAYER_ONE' | 'PLAYER_TWO';

export interface GameUpdate {
  gameId: string;
  playerOne: PlayerInfo;
  playerTwo: PlayerInfo;
  boardState: string;
  nextPlayer: PlayerDisc;
  status: 'IN_PROGRESS' | 'FINISHED';
  winner: PlayerDisc | 'DRAW' | null;
  error: string | null;
  origin?: string;
}

export interface DiscCustomization {
  itemCode?: string;
  displayName?: string;
  type: 'color' | 'image';
  value: string;
  price?: number | null;
}

export interface PlayerInfo {
  userId: string;
  displayName: string;
  avatarUrl: string;
  elo: number;
  disc: DiscCustomization | null;
}