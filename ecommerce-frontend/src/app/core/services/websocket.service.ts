import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, Subject, timer, EMPTY } from 'rxjs';
import { webSocket, WebSocketSubject } from 'rxjs/webSocket';
import { catchError, delay, retryWhen, tap, takeUntil, switchMap } from 'rxjs/operators';
import { environment } from '@environments/environment';

export enum ConnectionState {
  CONNECTING = 'CONNECTING',
  CONNECTED = 'CONNECTED',
  DISCONNECTED = 'DISCONNECTED',
  RECONNECTING = 'RECONNECTING',
  ERROR = 'ERROR'
}

export interface WebSocketMessage {
  body: string;
  headers?: any;
  type: string;
}

@Injectable({
  providedIn: 'root'
})
export class WebSocketService {
  private socket$!: WebSocketSubject<any>;
  private messagesSubject$ = new Subject<WebSocketMessage>();
  private connectionState$ = new BehaviorSubject<ConnectionState>(ConnectionState.DISCONNECTED);
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private reconnectInterval = 1000; // Start with 1 second
  private maxReconnectInterval = 30000; // Max 30 seconds
  private destroy$ = new Subject<void>();
  
  // WebSocket endpoint URL
  private wsUrl = `ws://localhost:8083/ws/orders`;

  constructor() {
    this.connect();
  }

  /**
   * Get current connection state
   */
  getConnectionState(): Observable<ConnectionState> {
    return this.connectionState$.asObservable();
  }

  /**
   * Get messages observable
   */
  getMessages(): Observable<WebSocketMessage> {
    return this.messagesSubject$.asObservable();
  }

  /**
   * Connect to WebSocket
   */
  private connect(): void {
    if (this.socket$) {
      return;
    }

    this.connectionState$.next(ConnectionState.CONNECTING);

    this.socket$ = webSocket({
      url: this.wsUrl,
      openObserver: {
        next: () => {
          console.log('[WebSocket] Connection opened');
          this.connectionState$.next(ConnectionState.CONNECTED);
          this.reconnectAttempts = 0;
          this.reconnectInterval = 1000; // Reset reconnect interval
        }
      },
      closeObserver: {
        next: (event) => {
          console.log('[WebSocket] Connection closed', event);
          this.connectionState$.next(ConnectionState.DISCONNECTED);
          this.socket$ = null!;
          this.scheduleReconnect();
        }
      }
    });

    // Subscribe to messages
    this.socket$
      .pipe(
        takeUntil(this.destroy$),
        catchError((error) => {
          console.error('[WebSocket] Connection error:', error);
          this.connectionState$.next(ConnectionState.ERROR);
          this.scheduleReconnect();
          return EMPTY;
        })
      )
      .subscribe(
        (message) => {
          console.log('[WebSocket] Received message:', message);
          this.messagesSubject$.next({
            body: JSON.stringify(message),
            type: message.type || 'message',
            headers: message.headers
          });
        }
      );
  }

  /**
   * Schedule reconnection with exponential backoff
   */
  private scheduleReconnect(): void {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error('[WebSocket] Max reconnection attempts reached');
      this.connectionState$.next(ConnectionState.ERROR);
      return;
    }

    this.reconnectAttempts++;
    this.connectionState$.next(ConnectionState.RECONNECTING);
    
    console.log(`[WebSocket] Reconnecting in ${this.reconnectInterval}ms (attempt ${this.reconnectAttempts})`);
    
    timer(this.reconnectInterval)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.connect();
      });

    // Exponential backoff with jitter
    this.reconnectInterval = Math.min(
      this.reconnectInterval * 2 + Math.random() * 1000,
      this.maxReconnectInterval
    );
  }

  /**
   * Send message to WebSocket
   */
  send(message: any): void {
    if (this.socket$ && this.connectionState$.value === ConnectionState.CONNECTED) {
      this.socket$.next(message);
    } else {
      console.warn('[WebSocket] Cannot send message - not connected');
    }
  }

  /**
   * Subscribe to specific topic
   */
  subscribe(destination: string): Observable<any> {
    return new Observable(observer => {
      // Wait for connection
      this.connectionState$
        .pipe(
          switchMap(state => {
            if (state === ConnectionState.CONNECTED) {
              // Send STOMP SUBSCRIBE frame
              this.send({
                command: 'SUBSCRIBE',
                destination: destination,
                id: this.generateSubscriptionId()
              });

              // Return messages for this destination
              return this.messagesSubject$.pipe(
                tap(message => {
                  try {
                    const parsedMessage = JSON.parse(message.body);
                    if (this.isMessageForDestination(parsedMessage, destination)) {
                      observer.next(parsedMessage);
                    }
                  } catch (error) {
                    console.error('[WebSocket] Error parsing message:', error);
                  }
                })
              );
            }
            return EMPTY;
          }),
          takeUntil(this.destroy$)
        )
        .subscribe();
    });
  }

  /**
   * Check if message is for specific destination
   */
  private isMessageForDestination(message: any, destination: string): boolean {
    // This is a simplified check - in a real STOMP implementation,
    // the server would include destination information
    return true;
  }

  /**
   * Generate unique subscription ID
   */
  private generateSubscriptionId(): string {
    return 'sub-' + Math.random().toString(36).substring(2, 15);
  }

  /**
   * Disconnect WebSocket
   */
  disconnect(): void {
    if (this.socket$) {
      this.socket$.complete();
      this.socket$ = null!;
    }
    this.connectionState$.next(ConnectionState.DISCONNECTED);
  }

  /**
   * Cleanup on destroy
   */
  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.disconnect();
  }
}