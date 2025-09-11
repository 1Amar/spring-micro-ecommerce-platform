import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { Client, IMessage, StompConfig, StompHeaders } from '@stomp/stompjs';
import { environment } from '@environments/environment';

export enum ConnectionState {
  CONNECTING = 'CONNECTING',
  CONNECTED = 'CONNECTED',
  DISCONNECTED = 'DISCONNECTED',
  RECONNECTING = 'RECONNECTING',
  ERROR = 'ERROR'
}

export interface OrderStatusUpdate {
  orderId: string;
  orderNumber: string;
  userId: string;
  previousStatus: string;
  newStatus: string;
  reason: string;
  changedBy: string;
  timestamp: string;
  paymentStatus?: string;
  trackingNumber?: string;
  carrier?: string;
  progressInfo?: {
    currentStep: number;
    totalSteps: number;
    currentStepName: string;
    nextStepName?: string;
    estimatedCompletion?: string;
  };
}

@Injectable({
  providedIn: 'root'
})
export class StompWebSocketService {
  private client!: Client;
  private connectionState$ = new BehaviorSubject<ConnectionState>(ConnectionState.DISCONNECTED);
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private subscriptions = new Map<string, Subject<any>>();

  constructor() {
    this.initializeWebSocketConnection();
  }

  /**
   * Initialize WebSocket connection with STOMP
   */
  private initializeWebSocketConnection(): void {
    const wsUrl = `ws://localhost:8083/ws/orders-native`;
    
    const stompConfig: StompConfig = {
      // Use native WebSocket instead of SockJS to avoid Node.js polyfill issues
      brokerURL: wsUrl,
      
      connectHeaders: {
        // Add authentication headers if needed
        // 'Authorization': 'Bearer ' + token
      },
      
      debug: (str) => {
        console.log('[STOMP Debug]', str);
      },
      
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      
      onConnect: (frame) => {
        console.log('[STOMP] Connected:', frame);
        this.connectionState$.next(ConnectionState.CONNECTED);
        this.reconnectAttempts = 0;
      },
      
      onStompError: (frame) => {
        console.error('[STOMP] Error:', frame);
        this.connectionState$.next(ConnectionState.ERROR);
      },
      
      onWebSocketClose: (event) => {
        console.log('[STOMP] WebSocket closed:', event);
        this.connectionState$.next(ConnectionState.DISCONNECTED);
        this.scheduleReconnect();
      },
      
      onWebSocketError: (event) => {
        console.error('[STOMP] WebSocket error:', event);
        this.connectionState$.next(ConnectionState.ERROR);
      },
      
      onDisconnect: (frame) => {
        console.log('[STOMP] Disconnected:', frame);
        this.connectionState$.next(ConnectionState.DISCONNECTED);
      }
    };

    this.client = new Client(stompConfig);
  }

  /**
   * Connect to WebSocket server
   */
  connect(): void {
    if (this.client && !this.client.connected) {
      this.connectionState$.next(ConnectionState.CONNECTING);
      this.client.activate();
    }
  }

  /**
   * Disconnect from WebSocket server
   */
  disconnect(): void {
    if (this.client) {
      this.client.deactivate();
    }
    // Clean up all subscriptions
    this.subscriptions.forEach(subject => subject.complete());
    this.subscriptions.clear();
  }

  /**
   * Get current connection state
   */
  getConnectionState(): Observable<ConnectionState> {
    return this.connectionState$.asObservable();
  }

  /**
   * Subscribe to order status updates for a specific order
   */
  subscribeToOrder(orderId: string): Observable<OrderStatusUpdate> {
    const destination = `/topic/orders/${orderId}`;
    return this.subscribeToDestination<OrderStatusUpdate>(destination);
  }

  /**
   * Subscribe to all order updates for a user
   */
  subscribeToUserOrders(userId: string): Observable<OrderStatusUpdate> {
    const destination = `/topic/orders/user/${userId}`;
    return this.subscribeToDestination<OrderStatusUpdate>(destination);
  }

  /**
   * Subscribe to order progress updates
   */
  subscribeToOrderProgress(orderId: string): Observable<OrderStatusUpdate> {
    const destination = `/topic/orders/${orderId}/progress`;
    return this.subscribeToDestination<OrderStatusUpdate>(destination);
  }

  /**
   * Generic method to subscribe to any destination
   */
  private subscribeToDestination<T>(destination: string): Observable<T> {
    // Return existing subscription if already exists
    if (this.subscriptions.has(destination)) {
      return this.subscriptions.get(destination)!.asObservable();
    }

    const subject = new Subject<T>();
    this.subscriptions.set(destination, subject);

    // Wait for connection before subscribing
    const connectionSubscription = this.connectionState$.subscribe(state => {
      if (state === ConnectionState.CONNECTED && this.client.connected) {
        console.log(`[STOMP] Subscribing to: ${destination}`);
        
        const stompSubscription = this.client.subscribe(destination, (message: IMessage) => {
          try {
            const data = JSON.parse(message.body);
            console.log(`[STOMP] Received message from ${destination}:`, data);
            subject.next(data);
          } catch (error) {
            console.error(`[STOMP] Error parsing message from ${destination}:`, error);
            subject.error(error);
          }
        });

        // Store subscription for cleanup
        const originalComplete = subject.complete.bind(subject);
        subject.complete = () => {
          console.log(`[STOMP] Unsubscribing from: ${destination}`);
          stompSubscription.unsubscribe();
          this.subscriptions.delete(destination);
          originalComplete();
        };

        connectionSubscription.unsubscribe();
      }
    });

    return subject.asObservable();
  }

  /**
   * Send message to server
   */
  sendMessage(destination: string, body: any, headers?: StompHeaders): void {
    if (this.client && this.client.connected) {
      this.client.publish({
        destination,
        body: JSON.stringify(body),
        headers: headers || {}
      });
    } else {
      console.warn('[STOMP] Cannot send message - not connected');
    }
  }

  /**
   * Send ping to keep connection alive
   */
  sendPing(): void {
    this.sendMessage('/app/orders/ping', { message: 'ping', timestamp: new Date().toISOString() });
  }

  /**
   * Request order status refresh
   */
  refreshOrderStatus(orderId: string): void {
    this.sendMessage(`/app/orders/${orderId}/refresh`, { orderId, timestamp: new Date().toISOString() });
  }

  /**
   * Request user orders refresh
   */
  refreshUserOrders(userId: string): void {
    this.sendMessage(`/app/orders/user/${userId}/refresh`, { userId, timestamp: new Date().toISOString() });
  }

  /**
   * Schedule reconnection with exponential backoff
   */
  private scheduleReconnect(): void {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error('[STOMP] Max reconnection attempts reached');
      this.connectionState$.next(ConnectionState.ERROR);
      return;
    }

    this.reconnectAttempts++;
    this.connectionState$.next(ConnectionState.RECONNECTING);
    
    const reconnectDelay = Math.min(1000 * Math.pow(2, this.reconnectAttempts), 30000);
    console.log(`[STOMP] Reconnecting in ${reconnectDelay}ms (attempt ${this.reconnectAttempts})`);
    
    setTimeout(() => {
      this.connect();
    }, reconnectDelay);
  }

  /**
   * Check if connected
   */
  isConnected(): boolean {
    return this.client && this.client.connected;
  }

  /**
   * Get current connection state value
   */
  getCurrentConnectionState(): ConnectionState {
    return this.connectionState$.value;
  }

  /**
   * Get connection statistics
   */
  getConnectionStats(): any {
    return {
      connected: this.isConnected(),
      state: this.connectionState$.value,
      reconnectAttempts: this.reconnectAttempts,
      activeSubscriptions: this.subscriptions.size
    };
  }

  /**
   * Cleanup on destroy
   */
  ngOnDestroy(): void {
    this.disconnect();
    this.connectionState$.complete();
  }
}