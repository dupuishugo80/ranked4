import { HttpClient } from "@angular/common/http";
import { Injectable, inject } from "@angular/core";
import { map, Observable } from "rxjs";
import { API_ENDPOINTS } from "../core/config/api.config";
import { Product, PurchaseRequest, PurchaseResponse, Skin } from "./shop.model";

interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
}

interface DiscCustomization {
  itemCode: string;
  displayName: string;
  type: 'color' | 'image';
  value: string;
  price: number | null;
  availableForPurchase?: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class ShopService {
  private http = inject(HttpClient);
  private readonly API_SHOP_URL = API_ENDPOINTS.SHOP;
  private readonly API_DISCS_URL = API_ENDPOINTS.DISCS;

  getProducts(): Observable<Skin[]> {
    return this.http.get<PageResponse<DiscCustomization>>(`${this.API_DISCS_URL}`).pipe(
      map(response => response.content
        .filter(disc => disc.price !== null && disc.price > 0 && disc.availableForPurchase !== false)
        .map(disc => ({
          id: 0,
          name: disc.displayName,
          description: `Custom disc ${disc.displayName}`,
          price: disc.price!,
          type: 'SKIN' as const,
          itemCode: disc.itemCode,
          skinType: disc.type,
          value: disc.value
        }))
      )
    );
  }

  purchaseProduct(itemCode: string): Observable<PurchaseResponse> {
    const request = { itemCode };
    return this.http.post<PurchaseResponse>(`${this.API_DISCS_URL}/purchase`, request);
  }

  getLootboxes(): Observable<any[]> {
    return this.http.get<PageResponse<any>>(`${this.API_SHOP_URL}/lootboxes`).pipe(
      map(response => response.content)
    );
  }

  openLootbox(lootboxId: number): Observable<LootboxOpeningResult> {
    return this.http.post<LootboxOpeningResult>(`${this.API_SHOP_URL}/lootboxes/${lootboxId}/open`, {});
  }
}

interface LootboxOpeningResult {
  openingId: number;
  rewardItemCode: string;
  rewardItemType: 'DISC' | 'GOLD';
  rewardGoldAmount: number | null;
  displayMessage: string;
}
