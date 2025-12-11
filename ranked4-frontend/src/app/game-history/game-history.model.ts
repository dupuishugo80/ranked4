export interface GameHistoryEntry {
  gameId: string;
  playerOneId: string;
  playerOneName: string;
  playerTwoId: string;
  playerTwoName: string;
  winner: 'PLAYER_ONE' | 'PLAYER_TWO' | null;
  finishedAt: string;
}
