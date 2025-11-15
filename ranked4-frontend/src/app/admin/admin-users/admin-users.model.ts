export interface ApiUserProfile {
  userId: string;
  displayName: string;
  avatarUrl: string;
  elo: number;
  gamesPlayed: number;
  wins: number;
  losses: number;
  draws: number;
  equippedDisc: {
    itemCode: string;
    displayName: string;
    type: string;
    value: string;
  } | null;
  createdAt: string;
  updatedAt: string;
}

export interface ApiUsersResponse {
  content: ApiUserProfile[];
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