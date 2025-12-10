import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { catchError, map, Observable, of } from 'rxjs';
import { API_ENDPOINTS } from '../core/config/api.config';
import { Product, PurchaseRequest, PurchaseResponse, RecentDrop, Skin } from './shop.model';

export interface PageResponse<T> {
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

  getProducts(page: number = 0, size: number = 10): Observable<PageResponse<Skin>> {
    return this.http.get<PageResponse<DiscCustomization>>(`${this.API_DISCS_URL}?page=${page}&size=${size}`).pipe(
      map((response) => ({
        content: response.content
          .filter((disc) => disc.price !== null && disc.price > 0 && disc.availableForPurchase !== false)
          .map((disc) => ({
            id: 0,
            name: disc.displayName,
            description: `Custom disc ${disc.displayName}`,
            price: disc.price!,
            type: 'SKIN' as const,
            itemCode: disc.itemCode,
            skinType: disc.type,
            value: disc.value
          })),
        totalElements: response.totalElements,
        totalPages: response.totalPages
      }))
    );
  }

  getAllDiscs(page: number = 0, size: number = 1000): Observable<DiscCustomization[]> {
    return this.http
      .get<PageResponse<DiscCustomization>>(`${this.API_DISCS_URL}?page=${page}&size=${size}`)
      .pipe(map((response) => response.content));
  }

  purchaseProduct(itemCode: string): Observable<PurchaseResponse> {
    const request = { itemCode };
    return this.http.post<PurchaseResponse>(`${this.API_DISCS_URL}/purchase`, request);
  }

  getLootboxes(page: number = 0, size: number = 10): Observable<PageResponse<any>> {
    return this.http.get<PageResponse<any>>(`${this.API_SHOP_URL}/lootboxes?page=${page}&size=${size}`);
  }

  openLootbox(lootboxId: number): Observable<LootboxOpeningResult> {
    return this.http.post<LootboxOpeningResult>(`${this.API_SHOP_URL}/lootboxes/${lootboxId}/open`, {});
  }

  getRecentDrops(lootboxId: number): Observable<RecentDrop[]> {
    return this.http.get<RecentDrop[]>(
      `${this.API_SHOP_URL}/lootboxes/${lootboxId}/recent-drops`
    ).pipe(
      catchError(error => {
        console.error('Failed to fetch recent drops:', error);
        return of([]);
      })
    );
  }
}

interface LootboxOpeningResult {
  openingId: number;
  rewardItemCode: string;
  rewardItemType: 'DISC' | 'GOLD';
  rewardGoldAmount: number | null;
  displayMessage: string;
}
