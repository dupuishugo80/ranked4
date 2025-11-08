import { HttpClient } from "@angular/common/http";
import { Injectable, inject } from "@angular/core";
import { Observable } from "rxjs";
import { UserProfile } from "./profile.model";

@Injectable({
  providedIn: 'root'
})
export class ProfileService {
  private http = inject(HttpClient);
  
  private readonly API_URL = 'http://localhost:8080/api/profiles';

  getProfile(): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.API_URL}/me`);
  }

  getProfileById(userId: string): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.API_URL}/${userId}`);
  }
}