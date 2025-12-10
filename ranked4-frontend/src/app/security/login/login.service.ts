import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpEvent, HttpHandlerFn, HttpRequest } from '@angular/common/http';
import { BehaviorSubject, catchError, filter, Observable, switchMap, take, tap, throwError } from 'rxjs';
import { AuthResponse, LoginRequest, RegisterRequest } from './login.model';
import { API_ENDPOINTS } from '../../core/config/api.config';
import { jwtDecode } from 'jwt-decode';
import { Router } from '@angular/router';

interface JwtPayload {
  sub: string;
  userId: string;
  roles: string[];
  iat: number;
  exp: number;
}

@Injectable({
  providedIn: 'root'
})
export class LoginService {
  private readonly API_URL = API_ENDPOINTS.AUTH;

  private http = inject(HttpClient);
  private router = inject(Router);

  private isRefreshing = false;
  private refreshTokenSubject = new BehaviorSubject<string | null>(null);

  login(payload: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API_URL}/login`, payload).pipe(
      tap((response) => {
        this.storeTokens(response);
      })
    );
  }

  logout(): void {
    const refreshToken = this.getRefreshToken();

    if (refreshToken) {
      this.http.post(`${this.API_URL}/logout`, { refreshToken }).subscribe({
        next: () => {},
        error: () => console.error('Échec de la déconnexion API')
      });
    }

    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
    this.router.navigate(['/login']);
  }

  private storeTokens(tokens: AuthResponse): void {
    localStorage.setItem('access_token', tokens.accessToken);
    localStorage.setItem('refresh_token', tokens.refreshToken);
  }

  getAccessToken(): string | null {
    return localStorage.getItem('access_token');
  }

  getRefreshToken(): string | null {
    return localStorage.getItem('refresh_token');
  }

  isAuthenticated(): boolean {
    return !!this.getAccessToken();
  }

  isAdmin(): boolean {
    const token = this.getAccessToken();
    if (!token) {
      return false;
    }

    try {
      const decodedToken = jwtDecode<JwtPayload & { roles?: string[] }>(token);
      const roles = decodedToken.roles || [];
      return roles.includes('ROLE_ADMIN');
    } catch (error) {
      console.error('Erreur lors du décodage du token', error);
      return false;
    }
  }

  getUserId(): string | null {
    const token = this.getAccessToken();
    if (!token) {
      return null;
    }

    try {
      const decodedToken = jwtDecode<JwtPayload>(token);
      return decodedToken.userId;
    } catch (error) {
      console.error('Erreur lors du décodage du token', error);
      return null;
    }
  }

  private refreshToken(): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.API_URL}/refresh`, {
        refreshToken: this.getRefreshToken()
      })
      .pipe(
        tap((tokens) => {
          this.storeTokens(tokens);
        })
      );
  }

  handle401Error(request: HttpRequest<any>, next: HttpHandlerFn): Observable<HttpEvent<any>> {
    if (!this.isRefreshing) {
      this.isRefreshing = true;
      this.refreshTokenSubject.next(null);

      return this.refreshToken().pipe(
        switchMap((tokens) => {
          this.isRefreshing = false;
          this.refreshTokenSubject.next(tokens.accessToken);

          return next(this.addTokenToRequest(request, tokens.accessToken));
        }),
        catchError((err) => {
          this.isRefreshing = false;
          this.logout();
          return throwError(() => err);
        })
      );
    } else {
      return this.refreshTokenSubject.pipe(
        filter((token) => token !== null),
        take(1),
        switchMap((token) => {
          return next(this.addTokenToRequest(request, token!));
        })
      );
    }
  }

  private addTokenToRequest(request: HttpRequest<any>, token: string) {
    return request.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }
}
