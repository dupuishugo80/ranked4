export interface DiscCustomization {
  itemCode: string;
  displayName: string;
  type: 'color' | 'image';
  value: string;
  price: number | null;
  availableForPurchase?: boolean;
}

export interface CreateDiscRequest {
  itemCode: string;
  displayName: string;
  type: 'color' | 'image';
  value: string;
  price: number | null;
  availableForPurchase?: boolean;
}

export interface ApiDiscsResponse {
  content: DiscCustomization[];
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
