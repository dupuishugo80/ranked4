import { HttpClient } from "@angular/common/http";
import { Injectable, inject } from "@angular/core";
import { Observable } from "rxjs";
import { UserProfile } from "./profile.model";
import { API_ENDPOINTS } from "../core/config/api.config";

@Injectable({
  providedIn: 'root'
})
export class ProfileService {
  private http = inject(HttpClient);

  private readonly API_URL = API_ENDPOINTS.PROFILES;
  private readonly DISCS_URL = API_ENDPOINTS.DISCS;

  getProfile(): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.API_URL}/me`);
  }

  getProfileById(userId: string): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.API_URL}/${userId}`);
  }

  updateAvatar(avatarUrl: string): Observable<UserProfile> {
    return this.http.put<UserProfile>(`${this.API_URL}/me/avatar`, { avatarUrl });
  }

  equipDisc(itemCode: string): Observable<any> {
    return this.http.post(`${this.DISCS_URL}/equip`, { itemCode });
  }

  unequipDisc(): Observable<any> {
    return this.http.post(`${this.DISCS_URL}/unequip`, {});
  }

  isDailyFreeAvailable(): Observable<{ available: boolean }> {
    return this.http.get<{ available: boolean }>(`${this.API_URL}/daily-free-available`);
  }
}