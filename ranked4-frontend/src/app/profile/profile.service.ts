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

  getProfile(): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.API_URL}/me`);
  }

  getProfileById(userId: string): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.API_URL}/${userId}`);
  }
}