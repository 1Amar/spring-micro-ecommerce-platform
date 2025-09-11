import { Component, OnInit, OnDestroy } from '@angular/core';
import { Subject, takeUntil } from 'rxjs';
import { StompWebSocketService, ConnectionState } from '@core/services/stomp-websocket.service';
import { OrderService } from '@core/services/order.service';

@Component({
  selector: 'app-websocket-test',
  template: `
    <div class="websocket-test-container">
      <h2>WebSocket Real-time Order Updates Test</h2>
      
      <div class="connection-status">
        <h3>Connection Status</h3>
        <div class="status-indicator" [class]="getConnectionStatusClass()">
          {{ connectionState }}
        </div>
        <button (click)="connect()" [disabled]="connectionState === 'CONNECTED'">Connect</button>
        <button (click)="disconnect()" [disabled]="connectionState === 'DISCONNECTED'">Disconnect</button>
      </div>

      <div class="test-controls">
        <h3>Test Controls</h3>
        <div>
          <input type="text" [(ngModel)]="testOrderId" placeholder="Enter Order ID" />
          <button (click)="subscribeToOrder()">Subscribe to Order</button>
        </div>
        <div>
          <input type="text" [(ngModel)]="testUserId" placeholder="Enter User ID" />
          <button (click)="subscribeToUserOrders()">Subscribe to User Orders</button>
        </div>
        <button (click)="sendPing()">Send Ping</button>
        <button (click)="clearMessages()">Clear Messages</button>
      </div>

      <div class="message-log">
        <h3>Real-time Messages ({{ messages.length }})</h3>
        <div class="messages" #messagesContainer>
          <div *ngFor="let message of messages; trackBy: trackByFn" 
               class="message" 
               [class.highlight]="message.isNew">
            <span class="timestamp">{{ message.timestamp | date:'HH:mm:ss.SSS' }}</span>
            <span class="type">{{ message.type }}</span>
            <pre class="content">{{ message.content | json }}</pre>
          </div>
        </div>
      </div>

      <div class="stats">
        <h3>Statistics</h3>
        <div>Connection State: {{ connectionState }}</div>
        <div>Messages Received: {{ messages.length }}</div>
        <div>Active Subscriptions: {{ activeSubscriptions.length }}</div>
        <div>Subscriptions: {{ activeSubscriptions.join(', ') }}</div>
      </div>
    </div>
  `,
  styles: [`
    .websocket-test-container {
      padding: 20px;
      max-width: 1200px;
      margin: 0 auto;
    }

    .connection-status {
      margin: 20px 0;
      padding: 15px;
      border: 1px solid #ddd;
      border-radius: 5px;
      background: #f9f9f9;
    }

    .status-indicator {
      padding: 5px 10px;
      border-radius: 3px;
      display: inline-block;
      font-weight: bold;
      margin: 10px 0;
    }

    .status-connected { background: #d4edda; color: #155724; }
    .status-connecting { background: #fff3cd; color: #856404; }
    .status-reconnecting { background: #fff3cd; color: #856404; }
    .status-disconnected { background: #f8d7da; color: #721c24; }
    .status-error { background: #f8d7da; color: #721c24; }

    .test-controls {
      margin: 20px 0;
      padding: 15px;
      border: 1px solid #ddd;
      border-radius: 5px;
    }

    .test-controls > div {
      margin: 10px 0;
    }

    .test-controls input {
      margin: 0 10px;
      padding: 5px;
      width: 200px;
    }

    .test-controls button {
      margin: 0 5px;
      padding: 8px 15px;
      background: #007bff;
      color: white;
      border: none;
      border-radius: 3px;
      cursor: pointer;
    }

    .test-controls button:disabled {
      background: #6c757d;
      cursor: not-allowed;
    }

    .test-controls button:hover:not(:disabled) {
      background: #0056b3;
    }

    .message-log {
      margin: 20px 0;
    }

    .messages {
      height: 400px;
      overflow-y: auto;
      border: 1px solid #ddd;
      padding: 10px;
      background: #f8f9fa;
    }

    .message {
      margin: 5px 0;
      padding: 8px;
      background: white;
      border-radius: 3px;
      transition: background-color 0.5s;
    }

    .message.highlight {
      background: #e7f3ff;
    }

    .timestamp {
      color: #666;
      font-size: 12px;
      margin-right: 10px;
    }

    .type {
      background: #007bff;
      color: white;
      padding: 2px 6px;
      border-radius: 3px;
      font-size: 12px;
      margin-right: 10px;
    }

    .content {
      margin: 5px 0 0 0;
      font-size: 12px;
      background: #f1f3f4;
      padding: 5px;
      border-radius: 3px;
    }

    .stats {
      margin: 20px 0;
      padding: 15px;
      border: 1px solid #ddd;
      border-radius: 5px;
      background: #f9f9f9;
    }

    .stats > div {
      margin: 5px 0;
    }
  `]
})
export class WebSocketTestComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  
  connectionState: ConnectionState = ConnectionState.DISCONNECTED;
  messages: any[] = [];
  activeSubscriptions: string[] = [];
  
  testOrderId = '';
  testUserId = '';

  constructor(
    private stompWebSocketService: StompWebSocketService,
    private orderService: OrderService
  ) {}

  ngOnInit(): void {
    // Monitor connection state
    this.stompWebSocketService.getConnectionState()
      .pipe(takeUntil(this.destroy$))
      .subscribe(state => {
        this.connectionState = state;
        this.addMessage('CONNECTION', `Connection state: ${state}`, null);
      });

    // Auto-connect on init
    this.connect();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  connect(): void {
    this.addMessage('ACTION', 'Connecting to WebSocket...', null);
    this.stompWebSocketService.connect();
  }

  disconnect(): void {
    this.addMessage('ACTION', 'Disconnecting from WebSocket...', null);
    this.stompWebSocketService.disconnect();
    this.activeSubscriptions = [];
  }

  subscribeToOrder(): void {
    if (!this.testOrderId.trim()) {
      alert('Please enter an Order ID');
      return;
    }

    const destination = `Order: ${this.testOrderId}`;
    if (this.activeSubscriptions.includes(destination)) {
      alert('Already subscribed to this order');
      return;
    }

    this.addMessage('SUBSCRIPTION', `Subscribing to order: ${this.testOrderId}`, null);
    
    this.orderService.subscribeToOrderUpdates(this.testOrderId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (update) => {
          this.addMessage('ORDER_UPDATE', `Order ${this.testOrderId} update`, update);
        },
        error: (error) => {
          this.addMessage('ERROR', `Order subscription error`, error);
        }
      });

    this.activeSubscriptions.push(destination);
  }

  subscribeToUserOrders(): void {
    if (!this.testUserId.trim()) {
      alert('Please enter a User ID');
      return;
    }

    const destination = `User: ${this.testUserId}`;
    if (this.activeSubscriptions.includes(destination)) {
      alert('Already subscribed to this user');
      return;
    }

    this.addMessage('SUBSCRIPTION', `Subscribing to user orders: ${this.testUserId}`, null);
    
    this.orderService.subscribeToUserOrders(this.testUserId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (update) => {
          this.addMessage('USER_ORDER_UPDATE', `User ${this.testUserId} order update`, update);
        },
        error: (error) => {
          this.addMessage('ERROR', `User orders subscription error`, error);
        }
      });

    this.activeSubscriptions.push(destination);
  }

  sendPing(): void {
    this.addMessage('ACTION', 'Sending ping...', null);
    this.stompWebSocketService.sendPing();
  }

  clearMessages(): void {
    this.messages = [];
  }

  private addMessage(type: string, message: string, data: any): void {
    const newMessage = {
      id: Date.now(),
      timestamp: new Date(),
      type,
      message,
      content: data,
      isNew: true
    };

    this.messages.unshift(newMessage);
    
    // Keep only last 100 messages
    if (this.messages.length > 100) {
      this.messages = this.messages.slice(0, 100);
    }

    // Remove highlight after 2 seconds
    setTimeout(() => {
      newMessage.isNew = false;
    }, 2000);
  }

  getConnectionStatusClass(): string {
    return `status-${this.connectionState.toLowerCase()}`;
  }

  trackByFn(index: number, item: any): any {
    return item.id;
  }
}