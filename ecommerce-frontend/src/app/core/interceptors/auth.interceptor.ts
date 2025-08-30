import { Injectable } from '@angular/core';
import {
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpInterceptor,
  HttpErrorResponse
} from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AuthService } from '../services/auth.service';
import { CorrelationIdService } from '../services/correlation-id.service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {

  constructor(
    private authService: AuthService,
    private correlationIdService: CorrelationIdService
  ) {}

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    // Always add correlation ID for ELK tracking
    const headers: { [name: string]: string } = {
      'X-Correlation-ID': this.correlationIdService.getCurrentCorrelationId()
    };

    // Add authorization header if user is logged in
    if (this.authService.isLoggedIn) {
      const token = this.authService.token;
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }
      
      // Add user identification headers for debugging and cart session management
      const currentUser = this.authService.getCurrentUser();
      if (currentUser) {
        headers['X-User-Id'] = currentUser.username || 'unknown';
        if (currentUser.email) {
          headers['X-User-Email'] = currentUser.email;
        }
      }
    }

    request = request.clone({ setHeaders: headers });

    return next.handle(request).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status === 401) {
          // Token might be expired, try to refresh
          this.authService.refreshToken().then(refreshed => {
            if (!refreshed) {
              // Refresh failed, redirect to login
              this.authService.login();
            }
          });
        }
        return throwError(() => error);
      })
    );
  }

}