import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { API_ENDPOINTS } from '../../core/config/api.config';

export interface PveGameResponse {
  gameId: string;
  playerId: string;
  difficulty: number;
}

@Injectable({
  providedIn: 'root'
})
export class AiGameService {
  private http = inject(HttpClient);
  private router = inject(Router);

  private readonly API_URL = `${API_ENDPOINTS.GAME}/pve`;

  createAiGame(difficulty: number): Observable<PveGameResponse> {
    return this.http
      .post<PveGameResponse>(this.API_URL, null, {
        params: { difficulty: difficulty.toString() }
      })
      .pipe(
        tap((response) => {
          this.router.navigate(['/game', response.gameId]);
        })
      );
  }
}
