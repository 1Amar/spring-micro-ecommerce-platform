import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@environments/environment';
import { Order, CreateOrderRequest, OrderSummary, OrderStatus } from '@shared/models/order.model';

@Injectable({
  providedIn: 'root'
})
export class OrderService {
  private readonly apiUrl = environment.apiUrl + environment.services.orderService;

  constructor(private http: HttpClient) {}

  // Order operations
  createOrder(request: CreateOrderRequest): Observable<Order> {
    return this.http.post<Order>(`${this.apiUrl}`, request);
  }

  getOrders(page: number = 0, size: number = 20): Observable<{ orders: Order[], totalItems: number }> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<{ orders: Order[], totalItems: number }>(`${this.apiUrl}`, { params });
  }

  getOrder(orderId: string): Observable<Order> {
    return this.http.get<Order>(`${this.apiUrl}/${orderId}`);
  }

  getOrderByNumber(orderNumber: string): Observable<Order> {
    return this.http.get<Order>(`${this.apiUrl}/number/${orderNumber}`);
  }

  cancelOrder(orderId: string, reason?: string): Observable<Order> {
    const body = reason ? { reason } : {};
    return this.http.post<Order>(`${this.apiUrl}/${orderId}/cancel`, body);
  }

  requestRefund(orderId: string, reason: string, items?: string[]): Observable<Order> {
    const body = { reason, items };
    return this.http.post<Order>(`${this.apiUrl}/${orderId}/refund`, body);
  }

  // Order tracking
  trackOrder(orderNumber: string): Observable<{
    order: Order,
    trackingInfo: {
      carrier: string,
      trackingNumber: string,
      status: string,
      estimatedDelivery: Date,
      trackingEvents: {
        date: Date,
        status: string,
        location: string,
        description: string
      }[]
    }
  }> {
    return this.http.get<any>(`${this.apiUrl}/track/${orderNumber}`);
  }

  // Order summary and calculations
  calculateOrderSummary(items: any[], shippingAddress?: any): Observable<OrderSummary> {
    return this.http.post<OrderSummary>(`${this.apiUrl}/calculate`, {
      items,
      shippingAddress
    });
  }

  getShippingOptions(address: any, items: any[]): Observable<{
    id: string,
    name: string,
    description: string,
    price: number,
    estimatedDelivery: Date
  }[]> {
    return this.http.post<any[]>(`${this.apiUrl}/shipping-options`, {
      address,
      items
    });
  }

  // Order status updates (admin operations)
  updateOrderStatus(orderId: string, status: OrderStatus, notes?: string): Observable<Order> {
    return this.http.put<Order>(`${this.apiUrl}/${orderId}/status`, {
      status,
      notes
    });
  }

  addTrackingInfo(orderId: string, trackingNumber: string, carrier: string): Observable<Order> {
    return this.http.put<Order>(`${this.apiUrl}/${orderId}/tracking`, {
      trackingNumber,
      carrier
    });
  }

  // Order notifications
  resendOrderConfirmation(orderId: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/${orderId}/resend-confirmation`, {});
  }

  subscribeToOrderUpdates(orderId: string): Observable<any> {
    // This could be implemented with WebSocket or Server-Sent Events
    return this.http.get<any>(`${this.apiUrl}/${orderId}/updates`);
  }

  // Order analytics (for user dashboard)
  getOrderStatistics(): Observable<{
    totalOrders: number,
    totalSpent: number,
    averageOrderValue: number,
    ordersByStatus: { status: OrderStatus, count: number }[],
    monthlySpending: { month: string, amount: number }[]
  }> {
    return this.http.get<any>(`${this.apiUrl}/statistics`);
  }

  // Utility methods
  getOrderStatusDisplay(status: OrderStatus): string {
    const statusMap = {
      [OrderStatus.PENDING]: 'Pending',
      [OrderStatus.CONFIRMED]: 'Confirmed',
      [OrderStatus.PROCESSING]: 'Processing',
      [OrderStatus.SHIPPED]: 'Shipped',
      [OrderStatus.DELIVERED]: 'Delivered',
      [OrderStatus.CANCELLED]: 'Cancelled',
      [OrderStatus.REFUNDED]: 'Refunded'
    };
    return statusMap[status] || status;
  }

  getOrderStatusColor(status: OrderStatus): string {
    const colorMap = {
      [OrderStatus.PENDING]: 'warning',
      [OrderStatus.CONFIRMED]: 'info',
      [OrderStatus.PROCESSING]: 'primary',
      [OrderStatus.SHIPPED]: 'success',
      [OrderStatus.DELIVERED]: 'success',
      [OrderStatus.CANCELLED]: 'danger',
      [OrderStatus.REFUNDED]: 'secondary'
    };
    return colorMap[status] || 'secondary';
  }

  canCancelOrder(order: Order): boolean {
    const cancellableStatuses = [OrderStatus.PENDING, OrderStatus.CONFIRMED];
    return cancellableStatuses.includes(order.status);
  }

  canRequestRefund(order: Order): boolean {
    const refundableStatuses = [OrderStatus.DELIVERED];
    const daysSinceDelivery = order.updatedAt ? 
      (new Date().getTime() - new Date(order.updatedAt).getTime()) / (1000 * 60 * 60 * 24) : 0;
    
    return refundableStatuses.includes(order.status) && daysSinceDelivery <= 30; // 30 day return policy
  }

  formatOrderNumber(orderNumber: string): string {
    return `#${orderNumber}`;
  }

  formatPrice(price: number, currency: string = 'USD'): string {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: currency
    }).format(price);
  }
}