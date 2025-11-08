import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { LoginService } from '../security/login/login.service';
import { catchError, throwError } from 'rxjs';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(LoginService);
  const token = authService.getAccessToken();

  if (token) {
    req = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (req.url.includes('/api/auth/')) {
        if (req.url.includes('/refresh')) {
          authService.logout();
        }
        return throwError(() => error);
      }

      if (error.status === 401) {
        return authService.handle401Error(req, next);
      }
      
      return throwError(() => error);
    })
  );
};