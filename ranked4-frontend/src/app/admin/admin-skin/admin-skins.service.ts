import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_ENDPOINTS } from '../../core/config/api.config';
import { ApiDiscsResponse, DiscCustomization, CreateDiscRequest } from './admin-skins.model';

@Injectable({
  providedIn: 'root'
})
export class AdminSkinsService {
  private http = inject(HttpClient);
  private readonly API_DISCS_URL = `${API_ENDPOINTS.PROFILES}/../../api/discs`;

  getAllDiscs(page: number = 0, size: number = 10): Observable<ApiDiscsResponse> {
    const params = new HttpParams().set('page', page.toString()).set('size', size.toString()).set('sort', 'id,desc');

    return this.http.get<ApiDiscsResponse>(this.API_DISCS_URL, { params });
  }

  createDisc(disc: CreateDiscRequest): Observable<DiscCustomization> {
    return this.http.post<DiscCustomization>(this.API_DISCS_URL, disc);
  }

  updateDisc(itemCode: string, disc: CreateDiscRequest): Observable<DiscCustomization> {
    return this.http.put<DiscCustomization>(`${this.API_DISCS_URL}/${itemCode}`, disc);
  }

  deleteDisc(itemCode: string): Observable<void> {
    return this.http.delete<void>(`${this.API_DISCS_URL}/${itemCode}`);
  }
}
