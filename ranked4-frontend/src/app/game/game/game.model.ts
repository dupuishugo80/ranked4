export type PlayerDisc = 'PLAYER_ONE' | 'PLAYER_TWO';

export interface GameUpdate {
  gameId: string;
  playerOneId: string;
  playerTwoId: string;
  boardState: string;
  nextPlayer: PlayerDisc;
  status: 'IN_PROGRESS' | 'FINISHED';
  winner: PlayerDisc | 'DRAW' | null;
  error: string | null;
  origin?: string;
}