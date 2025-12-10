import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_ENDPOINTS } from '../core/config/api.config';
import { GameHistoryEntry } from './game-history.model';

@Injectable({
  providedIn: 'root'
})
export class GameHistoryService {
  private http = inject(HttpClient);

  private readonly API_URL = API_ENDPOINTS.GAME;

  getGlobalHistory(): Observable<GameHistoryEntry[]> {
    return this.http.get<GameHistoryEntry[]>(`${this.API_URL}/history`);
  }

  getPlayerHistory(userId: string): Observable<GameHistoryEntry[]> {
    return this.http.get<GameHistoryEntry[]>(`${this.API_URL}/history/player/${userId}`);
  }
}
