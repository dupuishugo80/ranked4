export interface Product {
  id: number;
  name: string;
  description: string;
  price: number;
  type: 'SKIN' | 'LOOTBOX';
  imageUrl?: string;
  itemCode?: string;
}

export interface Skin extends Product {
  type: 'SKIN';
  itemCode: string;
  skinType: 'color' | 'image';
  value: string;
}

export interface Lootbox extends Product {
  type: 'LOOTBOX';
  imageUrl: string;
  dailyFree?: boolean;
  contents?: LootboxContent[];
}

export interface LootboxContent {
  id?: number;
  itemCode: string;
  itemType: 'DISC' | 'GOLD';
  weight: number;
  goldAmount?: number;
}

export interface PurchaseRequest {
  productId: number;
}

export interface PurchaseResponse {
  success: boolean;
  message: string;
  newBalance?: number;
}

export interface RecentDrop {
  displayName: string;
  userId: string;
  rewardItemCode: string;
  rewardItemType: 'DISC' | 'GOLD';
  rewardGoldAmount: number | null;
  openedAt: string;
}