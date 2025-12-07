import { HttpClient, HttpParams } from "@angular/common/http";
import { Injectable, inject } from "@angular/core";
import { Observable } from "rxjs";
import { API_ENDPOINTS } from "../../core/config/api.config";
import { ApiLootboxesResponse, CreateLootboxRequest, LootboxDetail, LootboxOpeningResult } from "./admin-lootboxes.model";

@Injectable({
  providedIn: 'root'
})
export class AdminLootboxesService {
    private http = inject(HttpClient);
    private readonly API_LOOTBOX_URL = `${API_ENDPOINTS.SHOP}/lootboxes`;

    getLootboxes(page: number = 0, size: number = 10, sortBy: string = 'name'): Observable<ApiLootboxesResponse> {
        const params = new HttpParams()
        .set('page', page.toString())
        .set('size', size.toString())
        .set('sort', sortBy);

        return this.http.get<ApiLootboxesResponse>(this.API_LOOTBOX_URL, { params });
    }

    getLootboxById(id: number): Observable<LootboxDetail> {
        return this.http.get<LootboxDetail>(`${this.API_LOOTBOX_URL}/${id}`);
    }

    createLootbox(request: CreateLootboxRequest): Observable<LootboxDetail> {
        return this.http.post<LootboxDetail>(this.API_LOOTBOX_URL, request);
    }

    updateLootbox(id: number, request: CreateLootboxRequest): Observable<LootboxDetail> {
        return this.http.put<LootboxDetail>(`${this.API_LOOTBOX_URL}/${id}`, request);
    }

    deleteLootbox(id: number): Observable<void> {
        return this.http.delete<void>(`${this.API_LOOTBOX_URL}/${id}`);
    }

    openLootbox(id: number): Observable<LootboxOpeningResult> {
        return this.http.post<LootboxOpeningResult>(`${this.API_LOOTBOX_URL}/${id}/open`, {});
    }
}