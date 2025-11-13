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
  disc: DiscCustomization | null;
}

export interface DiscCustomization {
  type: 'color' | 'image';
  value: string;
}