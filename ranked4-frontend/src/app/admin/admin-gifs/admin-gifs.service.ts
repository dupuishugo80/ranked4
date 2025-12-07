import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../core/config/api.config';
import { CreateGifRequest, Gif, UpdateGifRequest } from './admin-gifs.model';

@Injectable({
  providedIn: 'root'
})
export class AdminGifsService {
  private http = inject(HttpClient);
  private readonly API_URL = `${API_BASE_URL}/gifs`;

  getAllGifs(): Observable<Gif[]> {
    return this.http.get<Gif[]>(`${this.API_URL}?includeInactive=true`);
  }

  getGifById(id: number): Observable<Gif> {
    return this.http.get<Gif>(`${this.API_URL}/${id}`);
  }

  createGif(request: CreateGifRequest): Observable<Gif> {
    return this.http.post<Gif>(this.API_URL, request);
  }

  updateGif(id: number, request: UpdateGifRequest): Observable<Gif> {
    return this.http.put<Gif>(`${this.API_URL}/${id}`, request);
  }

  deleteGif(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`);
  }
}
