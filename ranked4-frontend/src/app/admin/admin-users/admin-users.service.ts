import { HttpClient, HttpParams } from "@angular/common/http";
import { Injectable, inject } from "@angular/core";
import { Observable } from "rxjs";
import { API_ENDPOINTS } from "../../core/config/api.config";
import { ApiUsersResponse } from "./admin-users.model";

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
}