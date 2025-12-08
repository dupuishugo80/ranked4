export interface LootboxContent {
  id?: number;
  itemCode: string;
  itemType: 'DISC' | 'GOLD';
  weight: number;
  goldAmount?: number;
}

export interface Lootbox {
  id: number;
  name: string;
  description: string;
  imageUrl: string;
  price: number;
  dailyFree?: boolean;
}

export interface LootboxDetail extends Lootbox {
  contents: LootboxContent[];
}

export interface CreateLootboxRequest {
  name: string;
  description: string;
  imageUrl: string;
  price: number;
  dailyFree: boolean;
  contents: Omit<LootboxContent, 'id'>[];
}

export interface LootboxOpeningResult {
  openingId: number;
  rewardItemCode: string;
  rewardItemType: string;
  rewardGoldAmount: number | null;
  displayMessage: string;
}

export interface ApiLootboxesResponse {
  content: Lootbox[];
  pageable: {
    pageNumber: number;
    pageSize: number;
    sort: {
      sorted: boolean;
      empty: boolean;
      unsorted: boolean;
    };
    offset: number;
    paged: boolean;
    unpaged: boolean;
  };
  last: boolean;
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
  sort: {
    sorted: boolean;
    empty: boolean;
    unsorted: boolean;
  };
  first: boolean;
  numberOfElements: number;
  empty: boolean;
}