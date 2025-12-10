import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { RegisterRequest } from '../login/login.model';
import { API_ENDPOINTS } from '../../core/config/api.config';
import { inject, Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class RegisterService {
  private readonly API_URL = API_ENDPOINTS.AUTH;

  private http = inject(HttpClient);

  register(payload: RegisterRequest): Observable<any> {
    return this.http.post(`${this.API_URL}/register`, payload);
  }
}
