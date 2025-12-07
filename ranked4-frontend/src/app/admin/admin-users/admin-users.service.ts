import { HttpClient, HttpParams } from "@angular/common/http";
import { Injectable, inject } from "@angular/core";
import { Observable } from "rxjs";
import { API_ENDPOINTS } from "../../core/config/api.config";
import { ApiUsersResponse, ApiUserProfile } from "./admin-users.model";

@Injectable({
  providedIn: 'root'
})
export class AdminUsersService {
    private http = inject(HttpClient);
    private readonly API_ADMIN_BASE = API_ENDPOINTS.PROFILES;

    getUserList(page: number = 0, size: number = 10, sortBy: string = 'elo'): Observable<ApiUsersResponse> {
        const params = new HttpParams()
        .set('page', page.toString())
        .set('size', size.toString())
        .set('sort', sortBy);

        return this.http.get<ApiUsersResponse>(`${this.API_ADMIN_BASE}/adminUserList`, { params });
    }

    creditGold(userId: string, amount: number): Observable<void> {
        const params = new HttpParams().set('amount', amount.toString());
        return this.http.post<void>(`${this.API_ADMIN_BASE}/${userId}/credit-gold`, null, { params });
    }

    deleteUser(userId: string): Observable<void> {
        return this.http.delete<void>(`${this.API_ADMIN_BASE}/${userId}`);
    }

    addDiscToUser(userId: string, itemCode: string, equip: boolean): Observable<ApiUserProfile> {
        return this.http.post<ApiUserProfile>(`${this.API_ADMIN_BASE}/${userId}/add-disc`, {
            itemCode,
            equip
        });
    }

    removeDiscFromUser(userId: string, itemCode: string): Observable<ApiUserProfile> {
        return this.http.delete<ApiUserProfile>(`${this.API_ADMIN_BASE}/${userId}/remove-disc/${itemCode}`);
    }
}