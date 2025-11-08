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
}