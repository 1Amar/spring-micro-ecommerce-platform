import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { environment } from '@environments/environment';

export interface InventoryDto {
  id: string;
  productId: number;
  quantity: number;
  reservedQuantity: number;
  availableQuantity: number;
  reorderLevel: number;
  maxStockLevel: number;
  productName?: string;
  productAsin?: string;
  isLowStock: boolean;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface InventoryResponse {
  service: string;
  success: boolean;
  data: InventoryDto | InventoryDto[];
  message: string;
  timestamp: number;
  error?: string;
  errorCode?: string;
}

export interface StockStatus {
  inStock: boolean;
  availableQuantity: number;
  isLowStock: boolean;
  stockStatus: 'IN_STOCK' | 'LOW_STOCK' | 'OUT_OF_STOCK';
}

@Injectable({
  providedIn: 'root'
})
export class InventoryService {
  private readonly apiUrl = environment.apiUrl + '/inventory';

  constructor(private http: HttpClient) {}

  /**
   * Get inventory information for a single product
   */
  getInventoryForProduct(productId: number): Observable<InventoryDto | null> {
    return this.http.get<InventoryResponse>(`${this.apiUrl}/product/${productId}`).pipe(
      map(response => {
        if (response.success && response.data) {
          return response.data as InventoryDto;
        }
        return null;
      }),
      catchError(error => {
        console.error(`Failed to get inventory for product ${productId}:`, error);
        return of(null);
      })
    );
  }

  /**
   * Get inventory information for multiple products
   */
  getInventoryForProducts(productIds: number[]): Observable<InventoryDto[]> {
    if (productIds.length === 0) {
      return of([]);
    }

    return this.http.post<InventoryResponse>(`${this.apiUrl}/products`, productIds).pipe(
      map(response => {
        if (response.success && response.data) {
          return response.data as InventoryDto[];
        }
        return [];
      }),
      catchError(error => {
        console.error('Failed to get inventory for products:', error);
        return of([]);
      })
    );
  }

  /**
   * Check availability for a specific product and quantity
   */
  checkAvailability(productId: number, quantity: number): Observable<{available: boolean, message: string, availableQuantity: number}> {
    return this.http.get<any>(`${this.apiUrl}/availability/${productId}`, {
      params: { quantity: quantity.toString() }
    }).pipe(
      map(response => ({
        available: response.data?.available || false,
        message: response.data?.message || 'Unknown availability',
        availableQuantity: response.data?.availableQuantity || 0
      })),
      catchError(error => {
        console.error(`Failed to check availability for product ${productId}:`, error);
        return of({
          available: false,
          message: 'Unable to check availability',
          availableQuantity: 0
        });
      })
    );
  }

  /**
   * Convert InventoryDto to StockStatus for UI display
   */
  getStockStatus(inventory: InventoryDto): StockStatus {
    const availableQuantity = inventory.availableQuantity || 0;
    const inStock = availableQuantity > 0;
    const isLowStock = inventory.isLowStock || (availableQuantity > 0 && availableQuantity <= inventory.reorderLevel);
    
    let stockStatus: 'IN_STOCK' | 'LOW_STOCK' | 'OUT_OF_STOCK';
    if (!inStock) {
      stockStatus = 'OUT_OF_STOCK';
    } else if (isLowStock) {
      stockStatus = 'LOW_STOCK';
    } else {
      stockStatus = 'IN_STOCK';
    }

    return {
      inStock,
      availableQuantity,
      isLowStock,
      stockStatus
    };
  }

  /**
   * Get display text for stock status
   */
  getStockStatusText(status: 'IN_STOCK' | 'LOW_STOCK' | 'OUT_OF_STOCK', quantity?: number): string {
    switch (status) {
      case 'IN_STOCK':
        return 'In Stock';
      case 'LOW_STOCK':
        return quantity ? `Only ${quantity} left` : 'Low Stock';
      case 'OUT_OF_STOCK':
        return 'Out of Stock';
      default:
        return 'Unknown';
    }
  }

  /**
   * Get CSS class for stock status badge
   */
  getStockStatusClass(status: 'IN_STOCK' | 'LOW_STOCK' | 'OUT_OF_STOCK'): string {
    switch (status) {
      case 'IN_STOCK':
        return 'badge bg-success';
      case 'LOW_STOCK':
        return 'badge bg-warning';
      case 'OUT_OF_STOCK':
        return 'badge bg-danger';
      default:
        return 'badge bg-secondary';
    }
  }
}