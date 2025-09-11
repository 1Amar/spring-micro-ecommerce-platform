import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, Subject, combineLatest } from 'rxjs';
import { map, takeUntil, distinctUntilChanged, filter } from 'rxjs/operators';
import { OrderService } from './order.service';
import { StompWebSocketService, OrderStatusUpdate, ConnectionState } from './stomp-websocket.service';
import { AuthService } from './auth.service';

export interface OrderTrackingInfo {
  orderId: string;
  isTracking: boolean;
  lastUpdate?: OrderStatusUpdate;
  connectionState: ConnectionState;
}

@Injectable({
  providedIn: 'root'
})
export class OrderRealtimeService {
  private destroy$ = new Subject<void>();
  private trackedOrders = new Map<string, BehaviorSubject<OrderStatusUpdate | null>>();
  private orderTrackingInfo$ = new BehaviorSubject<Map<string, OrderTrackingInfo>>(new Map());
  private globalNotifications$ = new Subject<OrderStatusUpdate>();
  
  // Connection management
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 3;

  constructor(
    private orderService: OrderService,
    private stompWebSocketService: StompWebSocketService,
    private authService: AuthService
  ) {
    this.initializeGlobalTracking();
  }

  /**
   * Initialize global order tracking for current user
   */
  private initializeGlobalTracking(): void {
    // Track connection state
    this.stompWebSocketService.getConnectionState()
      .pipe(
        takeUntil(this.destroy$),
        distinctUntilChanged()
      )
      .subscribe(state => {
        this.updateAllOrderConnectionStates(state);
        
        if (state === ConnectionState.CONNECTED) {
          this.reconnectAttempts = 0;
          this.refreshAllTrackedOrders();
        } else if (state === ConnectionState.ERROR) {
          this.handleConnectionError();
        }
      });

    // Subscribe to user's orders when authenticated
    const currentUser = this.authService.getCurrentUser();
    if (currentUser?.id) {
      this.subscribeToUserOrders(currentUser.id);
    }
  }

  /**
   * Start tracking a specific order
   */
  trackOrder(orderId: string): Observable<OrderStatusUpdate | null> {
    if (this.trackedOrders.has(orderId)) {
      return this.trackedOrders.get(orderId)!.asObservable();
    }

    const orderSubject = new BehaviorSubject<OrderStatusUpdate | null>(null);
    this.trackedOrders.set(orderId, orderSubject);

    // Update tracking info
    this.updateOrderTrackingInfo(orderId, {
      orderId,
      isTracking: true,
      connectionState: this.stompWebSocketService.getCurrentConnectionState()
    });

    // Subscribe to order updates
    this.orderService.subscribeToOrderUpdates(orderId)
      .pipe(
        takeUntil(this.destroy$),
        distinctUntilChanged((prev, curr) => 
          prev?.timestamp === curr?.timestamp && prev?.newStatus === curr?.newStatus
        )
      )
      .subscribe(
        (update: OrderStatusUpdate) => {
          orderSubject.next(update);
          this.updateOrderTrackingInfo(orderId, {
            orderId,
            isTracking: true,
            lastUpdate: update,
            connectionState: this.stompWebSocketService.getCurrentConnectionState()
          });
          
          // Emit global notification
          this.globalNotifications$.next(update);
        },
        (error) => {
          console.error(`Error tracking order ${orderId}:`, error);
          this.handleOrderTrackingError(orderId, error);
        }
      );

    // Request immediate status refresh
    this.orderService.refreshOrderStatus(orderId);

    return orderSubject.asObservable();
  }

  /**
   * Stop tracking a specific order
   */
  stopTrackingOrder(orderId: string): void {
    const orderSubject = this.trackedOrders.get(orderId);
    if (orderSubject) {
      orderSubject.complete();
      this.trackedOrders.delete(orderId);
    }

    // Update tracking info
    const currentInfo = this.orderTrackingInfo$.value;
    currentInfo.delete(orderId);
    this.orderTrackingInfo$.next(new Map(currentInfo));
  }

  /**
   * Get tracking information for all orders
   */
  getOrderTrackingInfo(): Observable<Map<string, OrderTrackingInfo>> {
    return this.orderTrackingInfo$.asObservable();
  }

  /**
   * Get global order notifications stream
   */
  getGlobalNotifications(): Observable<OrderStatusUpdate> {
    return this.globalNotifications$.asObservable();
  }

  /**
   * Check if an order is being tracked
   */
  isTrackingOrder(orderId: string): boolean {
    return this.trackedOrders.has(orderId);
  }

  /**
   * Get current connection state
   */
  getConnectionState(): Observable<ConnectionState> {
    return this.stompWebSocketService.getConnectionState();
  }

  /**
   * Manually reconnect WebSocket
   */
  reconnect(): void {
    this.stompWebSocketService.disconnect();
    setTimeout(() => {
      this.stompWebSocketService.connect();
    }, 1000);
  }

  /**
   * Get tracking statistics
   */
  getTrackingStats(): Observable<{
    totalTracked: number;
    connected: boolean;
    connectionState: ConnectionState;
    reconnectAttempts: number;
  }> {
    return combineLatest([
      this.orderTrackingInfo$,
      this.getConnectionState()
    ]).pipe(
      map(([trackingInfo, connectionState]) => ({
        totalTracked: trackingInfo.size,
        connected: connectionState === ConnectionState.CONNECTED,
        connectionState,
        reconnectAttempts: this.reconnectAttempts
      }))
    );
  }

  /**
   * Subscribe to all orders for current user
   */
  private subscribeToUserOrders(userId: string): void {
    this.orderService.subscribeToUserOrders(userId)
      .pipe(
        takeUntil(this.destroy$),
        distinctUntilChanged((prev, curr) => 
          prev?.orderId === curr?.orderId && 
          prev?.timestamp === curr?.timestamp
        )
      )
      .subscribe(
        (update: OrderStatusUpdate) => {
          // Update specific order tracking if it exists
          const orderSubject = this.trackedOrders.get(update.orderId);
          if (orderSubject) {
            orderSubject.next(update);
          }

          // Update tracking info
          this.updateOrderTrackingInfo(update.orderId, {
            orderId: update.orderId,
            isTracking: this.trackedOrders.has(update.orderId),
            lastUpdate: update,
            connectionState: this.stompWebSocketService.getCurrentConnectionState()
          });

          // Emit global notification
          this.globalNotifications$.next(update);
        },
        (error) => {
          console.error('Error receiving user order updates:', error);
        }
      );
  }

  /**
   * Update order tracking information
   */
  private updateOrderTrackingInfo(orderId: string, info: OrderTrackingInfo): void {
    const currentInfo = this.orderTrackingInfo$.value;
    currentInfo.set(orderId, info);
    this.orderTrackingInfo$.next(new Map(currentInfo));
  }

  /**
   * Update connection state for all tracked orders
   */
  private updateAllOrderConnectionStates(state: ConnectionState): void {
    const currentInfo = this.orderTrackingInfo$.value;
    const updated = new Map();
    
    currentInfo.forEach((info, orderId) => {
      updated.set(orderId, { ...info, connectionState: state });
    });
    
    this.orderTrackingInfo$.next(updated);
  }

  /**
   * Refresh all tracked orders
   */
  private refreshAllTrackedOrders(): void {
    this.trackedOrders.forEach((_, orderId) => {
      this.orderService.refreshOrderStatus(orderId);
    });
  }

  /**
   * Handle connection errors
   */
  private handleConnectionError(): void {
    this.reconnectAttempts++;
    
    if (this.reconnectAttempts <= this.maxReconnectAttempts) {
      console.log(`Attempting to reconnect... (${this.reconnectAttempts}/${this.maxReconnectAttempts})`);
      setTimeout(() => {
        this.reconnect();
      }, 2000 * this.reconnectAttempts); // Exponential backoff
    } else {
      console.error('Max reconnection attempts reached. Switching to polling mode.');
      // Could implement HTTP polling fallback here
    }
  }

  /**
   * Handle order tracking errors
   */
  private handleOrderTrackingError(orderId: string, error: any): void {
    console.error(`Order tracking error for ${orderId}:`, error);
    
    // Update tracking info with error state
    this.updateOrderTrackingInfo(orderId, {
      orderId,
      isTracking: false,
      connectionState: ConnectionState.ERROR
    });

    // Could implement fallback to HTTP polling for this specific order
  }

  /**
   * Cleanup on destroy
   */
  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    
    // Complete all order subjects
    this.trackedOrders.forEach(subject => subject.complete());
    this.trackedOrders.clear();
    
    // Complete subjects
    this.orderTrackingInfo$.complete();
    this.globalNotifications$.complete();
  }
}