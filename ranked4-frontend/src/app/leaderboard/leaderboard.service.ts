import { HttpClient } from "@angular/common/http";
import { Injectable, inject } from "@angular/core";
import { Observable } from "rxjs";
import { UserProfile } from "../profile/profile.model";

@Injectable({
  providedIn: 'root'
})
export class LeaderboardService {
  private http = inject(HttpClient);
  
  private readonly API_URL = 'http://localhost:8080/api/profiles';

  getLeaderboard(): Observable<UserProfile[]> {
    return this.http.get<UserProfile[]>(`${this.API_URL}/leaderboard`);
  }
}