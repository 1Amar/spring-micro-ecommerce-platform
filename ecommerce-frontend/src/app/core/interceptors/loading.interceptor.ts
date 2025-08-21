import { Injectable } from '@angular/core';
import {
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpInterceptor
} from '@angular/common/http';
import { Observable } from 'rxjs';
import { finalize } from 'rxjs/operators';
import { LoadingService } from '../services/loading.service';

@Injectable()
export class LoadingInterceptor implements HttpInterceptor {

  constructor(private loadingService: LoadingService) {}

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    // Don't show loading for certain requests
    if (this.shouldSkipLoading(request)) {
      return next.handle(request);
    }

    this.loadingService.show();

    return next.handle(request).pipe(
      finalize(() => this.loadingService.hide())
    );
  }

  private shouldSkipLoading(request: HttpRequest<any>): boolean {
    // Skip loading for:
    // 1. Requests to health check endpoints
    // 2. Requests marked with skipLoading header
    // 3. Background requests (like token refresh)
    
    const skipHeader = request.headers.get('Skip-Loading');
    const isHealthCheck = request.url.includes('/health') || request.url.includes('/actuator');
    const isTokenRefresh = request.url.includes('/token') && request.method === 'POST';
    
    return skipHeader === 'true' || isHealthCheck || isTokenRefresh;
  }
}