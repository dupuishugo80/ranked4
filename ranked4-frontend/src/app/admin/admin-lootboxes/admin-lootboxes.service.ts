import { HttpClient, HttpParams } from "@angular/common/http";
import { Injectable, inject } from "@angular/core";
import { Observable } from "rxjs";
import { API_ENDPOINTS } from "../../core/config/api.config";
import { ApiLootboxesResponse } from "./admin-lootboxes.model";

@Injectable({
  providedIn: 'root'
})
export class AdminLootboxesService {
    private http = inject(HttpClient);
    private readonly API_LOOTBOX_URL = `${API_ENDPOINTS.SHOP}/products`;
    
    getLootboxes(page: number = 0, size: number = 10, sortBy: string = 'name'): Observable<ApiLootboxesResponse> {
        const params = new HttpParams()
        .set('page', page.toString())
        .set('size', size.toString())
        .set('sort', sortBy);

        return this.http.get<ApiLootboxesResponse>(this.API_LOOTBOX_URL, { params });
    }
}