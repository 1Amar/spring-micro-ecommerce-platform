import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class CorrelationIdService {
  private currentCorrelationId: string = '';

  constructor() {
    // Generate initial correlation ID
    this.generateNewCorrelationId();
  }

  /**
   * Get the current correlation ID
   */
  getCurrentCorrelationId(): string {
    return this.currentCorrelationId;
  }

  /**
   * Set a custom correlation ID (used by ELK test component)
   */
  setCorrelationId(correlationId: string): void {
    this.currentCorrelationId = correlationId;
    console.log('🔍 Correlation ID set:', correlationId);
  }

  /**
   * Generate a new correlation ID and set it as current
   */
  generateNewCorrelationId(): string {
    this.currentCorrelationId = this.generateCorrelationId();
    console.log('🔍 New correlation ID generated:', this.currentCorrelationId);
    return this.currentCorrelationId;
  }

  /**
   * Generate a correlation ID in the standard format
   */
  private generateCorrelationId(): string {
    return 'web-' + Math.random().toString(36).substr(2, 9) + '-' + Date.now();
  }
}