import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { LeaderboardEntry } from '../profile/profile.model';
import { API_ENDPOINTS } from '../core/config/api.config';

@Injectable({
  providedIn: 'root'
})
export class LeaderboardService {
  private http = inject(HttpClient);

  private readonly API_URL = API_ENDPOINTS.PROFILES;

  getLeaderboard(): Observable<LeaderboardEntry[]> {
    return this.http.get<LeaderboardEntry[]>(`${this.API_URL}/leaderboard`);
  }
}
