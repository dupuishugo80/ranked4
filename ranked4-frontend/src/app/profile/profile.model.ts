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

export interface DiscCustomization {
  itemCode?: string;
  displayName?: string;
  type: 'color' | 'image';
  value: string;
  price?: number | null;
  availableForPurchase?: boolean;
}
