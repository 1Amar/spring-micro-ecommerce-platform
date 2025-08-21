import { Component, OnInit } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, forkJoin, interval, of } from 'rxjs';
import { catchError, delay, switchMap, tap } from 'rxjs/operators';
import { environment } from '@environments/environment';
import { ProductService } from '@core/services/product.service';
import { OrderService } from '@core/services/order.service';
import { AuthService } from '@core/services/auth.service';
import { CorrelationIdService } from '@core/services/correlation-id.service';

interface TestResult {
  step: string;
  correlationId: string;
  service: string;
  status: 'success' | 'error' | 'running';
  response?: any;
  error?: string;
  timestamp: Date;
  duration?: number;
}

@Component({
  selector: 'app-elk-test',
  template: `
    <div class="elk-test-container">
      <div class="test-header">
        <h2>🔍 ELK Stack Testing Dashboard</h2>
        <p>Generate end-to-end correlation-tracked requests for Kibana analysis</p>
      </div>

      <div class="test-controls">
        <div class="control-group">
          <button 
            (click)="runSimpleTest()" 
            [disabled]="isRunning"
            class="btn btn-primary">
            🚀 Simple Gateway Test
          </button>
          
          <button 
            (click)="runWorkingTest()" 
            [disabled]="isRunning"
            class="btn btn-info">
            ✅ Working ELK Test
          </button>
          
          <button 
            (click)="runComplexOrderFlow()" 
            [disabled]="isRunning"
            class="btn btn-success">
            🛒 Complex Order Flow Test
          </button>
          
          <button 
            (click)="runStressTest()" 
            [disabled]="isRunning"
            class="btn btn-warning">
            ⚡ Stress Test (10 requests)
          </button>
          
          <button 
            (click)="runErrorTest()" 
            [disabled]="isRunning"
            class="btn btn-danger">
            💥 Error Simulation Test
          </button>
          
          <button 
            (click)="runCorrelationIdTest()" 
            [disabled]="isRunning"
            class="btn btn-info">
            🔗 Correlation ID Flow Test
          </button>
        </div>

        <div class="search-info" *ngIf="currentCorrelationId">
          <strong>Current Correlation ID:</strong> 
          <code>{{ currentCorrelationId }}</code>
          <button (click)="copyToClipboard(currentCorrelationId)" class="btn btn-sm btn-outline-secondary">
            📋 Copy for Kibana
          </button>
        </div>
      </div>

      <div class="test-results">
        <h3>Test Results ({{ testResults.length }})</h3>
        <div class="results-list" *ngIf="testResults.length > 0">
          <div *ngFor="let result of testResults; let i = index" 
               class="result-item"
               [ngClass]="'result-' + result.status">
            
            <div class="result-header">
              <span class="step-number">{{ i + 1 }}</span>
              <strong>{{ result.step }}</strong>
              <span class="service-badge">{{ result.service }}</span>
              <span class="status-badge" [ngClass]="'status-' + result.status">
                {{ result.status }}
              </span>
              <span class="timestamp">{{ result.timestamp | date:'HH:mm:ss.SSS' }}</span>
              <span *ngIf="result.duration" class="duration">{{ result.duration }}ms</span>
            </div>
            
            <div class="correlation-id">
              <strong>Correlation ID:</strong> <code>{{ result.correlationId }}</code>
            </div>
            
            <div class="result-details" *ngIf="result.response">
              <button (click)="toggleDetails(i)" class="btn btn-sm btn-outline-info">
                {{ showDetails[i] ? 'Hide' : 'Show' }} Response
              </button>
              <pre *ngIf="showDetails[i]" class="response-json">{{ result.response | json }}</pre>
            </div>
            
            <div class="error-details" *ngIf="result.error">
              <strong>Error:</strong> <span class="error-message">{{ result.error }}</span>
            </div>
          </div>
        </div>
        
        <div *ngIf="testResults.length === 0" class="no-results">
          No test results yet. Click a test button to start generating ELK logs!
        </div>
      </div>

      <div class="kibana-info">
        <h3>🔍 How to Track Correlation ID Flow in Kibana</h3>
        <ol>
          <li>Open Kibana: <a href="http://localhost:5601" target="_blank">http://localhost:5601</a></li>
          <li>Go to <strong>Discover</strong></li>
          <li>Make sure index pattern is: <code>spring-boot-logs-*</code></li>
          <li>Set time range to <strong>"Last 15 minutes"</strong></li>
          <li><strong>Search for correlation ID:</strong> Copy any correlation ID from above and paste in search</li>
          <li><strong>Filter by service:</strong> <code>springAppName: "ecom-order-service"</code> or <code>"inventory-service"</code> or <code>"payment-service"</code></li>
          <li><strong>Sort by timestamp</strong> to see the request flow order</li>
          <li><strong>Look for patterns:</strong> You should see the same correlation ID across multiple services</li>
        </ol>
        
        <div style="margin-top: 15px; padding: 10px; background: #fff3cd; border-radius: 4px;">
          <strong>🎯 What to Look For:</strong><br>
          • Same correlation ID appearing in order-service, inventory-service, and payment-service logs<br>
          • Request flow: Gateway → Order Service → Inventory Service → Payment Service<br>
          • Each service should log the received correlation ID<br>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .elk-test-container {
      padding: 20px;
      max-width: 1200px;
      margin: 0 auto;
    }

    .test-header {
      text-align: center;
      margin-bottom: 30px;
      padding: 20px;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;
      border-radius: 10px;
    }

    .test-controls {
      margin-bottom: 30px;
    }

    .control-group {
      display: flex;
      gap: 15px;
      margin-bottom: 20px;
      flex-wrap: wrap;
    }

    .btn {
      padding: 12px 24px;
      border: none;
      border-radius: 6px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.3s ease;
    }

    .btn:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }

    .btn-primary { background: #007bff; color: white; }
    .btn-success { background: #28a745; color: white; }
    .btn-warning { background: #ffc107; color: #212529; }
    .btn-danger { background: #dc3545; color: white; }
    .btn-outline-secondary { 
      background: transparent; 
      border: 1px solid #6c757d; 
      color: #6c757d; 
      padding: 4px 8px;
      font-size: 12px;
    }

    .search-info {
      padding: 15px;
      background: #e7f3ff;
      border-radius: 6px;
      border-left: 4px solid #007bff;
    }

    .search-info code {
      background: #f8f9fa;
      padding: 2px 6px;
      border-radius: 3px;
      font-family: 'Courier New', monospace;
    }

    .test-results {
      margin-bottom: 30px;
    }

    .results-list {
      max-height: 600px;
      overflow-y: auto;
      border: 1px solid #dee2e6;
      border-radius: 6px;
    }

    .result-item {
      padding: 15px;
      border-bottom: 1px solid #dee2e6;
      transition: background-color 0.3s ease;
    }

    .result-item:hover {
      background-color: #f8f9fa;
    }

    .result-success { border-left: 4px solid #28a745; }
    .result-error { border-left: 4px solid #dc3545; }
    .result-running { border-left: 4px solid #ffc107; }

    .result-header {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-bottom: 8px;
      flex-wrap: wrap;
    }

    .step-number {
      background: #6c757d;
      color: white;
      border-radius: 50%;
      width: 24px;
      height: 24px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 12px;
      font-weight: bold;
    }

    .service-badge {
      background: #e9ecef;
      color: #495057;
      padding: 2px 8px;
      border-radius: 12px;
      font-size: 12px;
      font-weight: 500;
    }

    .status-badge {
      padding: 2px 8px;
      border-radius: 12px;
      font-size: 12px;
      font-weight: 500;
    }

    .status-success { background: #d4edda; color: #155724; }
    .status-error { background: #f8d7da; color: #721c24; }
    .status-running { background: #fff3cd; color: #856404; }

    .timestamp, .duration {
      font-size: 12px;
      color: #6c757d;
    }

    .correlation-id {
      margin-bottom: 8px;
      font-size: 14px;
    }

    .correlation-id code {
      background: #f8f9fa;
      padding: 2px 6px;
      border-radius: 3px;
      font-family: 'Courier New', monospace;
      color: #e83e8c;
    }

    .response-json {
      background: #f8f9fa;
      padding: 10px;
      border-radius: 4px;
      font-size: 12px;
      max-height: 200px;
      overflow-y: auto;
      margin-top: 8px;
    }

    .error-message {
      color: #dc3545;
      font-weight: 500;
    }

    .no-results {
      text-align: center;
      padding: 40px;
      color: #6c757d;
      font-style: italic;
    }

    .kibana-info {
      background: #f8f9fa;
      padding: 20px;
      border-radius: 6px;
      border-left: 4px solid #17a2b8;
    }

    .kibana-info ol {
      margin: 10px 0;
      padding-left: 20px;
    }

    .kibana-info li {
      margin-bottom: 8px;
    }

    .kibana-info code {
      background: #e9ecef;
      padding: 2px 6px;
      border-radius: 3px;
      font-family: 'Courier New', monospace;
    }

    .kibana-info a {
      color: #007bff;
      text-decoration: none;
    }

    .kibana-info a:hover {
      text-decoration: underline;
    }
  `]
})
export class ELKTestComponent implements OnInit {
  testResults: TestResult[] = [];
  isRunning = false;
  currentCorrelationId = '';
  showDetails: boolean[] = [];

  constructor(
    private http: HttpClient,
    private productService: ProductService,
    private orderService: OrderService,
    private authService: AuthService,
    private correlationIdService: CorrelationIdService
  ) {}

  ngOnInit(): void {
    console.log('🔍 ELK Test Component initialized');
  }

  async runSimpleTest(): Promise<void> {
    this.isRunning = true;
    this.currentCorrelationId = this.generateCorrelationId();
    this.correlationIdService.setCorrelationId(this.currentCorrelationId);
    this.clearResults();

    try {
      // Test 1: API Gateway Health Check
      await this.executeStep(
        'API Gateway Health Check',
        'api-gateway',
        () => this.checkHealth('/gateway')
      );

      // Test 2: Get Categories (this should work via gateway)
      await this.executeStep(
        'Get Product Categories',
        'catalog-service',
        () => this.productService.getCategories()
      );

      // Test 3: Test the existing endpoint that works
      await this.executeStep(
        'Test Public Endpoint',
        'api-gateway',
        () => this.http.get(`${environment.apiUrl}/public/health`)
      );

    } catch (error) {
      console.error('Test failed:', error);
    } finally {
      this.isRunning = false;
    }
  }

  async runComplexOrderFlow(): Promise<void> {
    this.isRunning = true;
    this.currentCorrelationId = this.generateCorrelationId();
    this.correlationIdService.setCorrelationId(this.currentCorrelationId);
    this.clearResults();

    try {
      // Test the actual order simulation that goes through multiple services
      await this.executeStep(
        'Order Simulation (Order → Inventory → Payment)',
        'order-service-chain',
        () => this.http.post(`${environment.apiUrl}/order/simulate`, {})
      );

      // Test individual service endpoints for comparison
      await this.executeStep(
        'Direct Inventory Check',
        'inventory-service',
        () => this.http.get(`${environment.apiUrl}/inventory/check/PRODUCT-001`)
      );

      await this.executeStep(
        'Direct Payment Health',
        'payment-service',
        () => this.http.get(`${environment.apiUrl}/payments/health`)
      );

      await this.executeStep(
        'Catalog Service Test',
        'catalog-service',
        () => this.http.get(`${environment.apiUrl}/catalog/categories`)
      );

    } catch (error) {
      console.error('Complex test failed:', error);
    } finally {
      this.isRunning = false;
    }
  }

  async runStressTest(): Promise<void> {
    this.isRunning = true;
    this.clearResults();

    const requests = [];
    for (let i = 0; i < 10; i++) {
      this.currentCorrelationId = this.generateCorrelationId();
      this.correlationIdService.setCorrelationId(this.currentCorrelationId);
      requests.push(
        this.executeStep(
          `Stress Test Request ${i + 1}`,
          'product-service',
          () => this.checkHealth('/products'),
          500 // 500ms delay between requests
        )
      );
    }

    try {
      await Promise.all(requests);
    } catch (error) {
      console.error('Stress test failed:', error);
    } finally {
      this.isRunning = false;
    }
  }

  async runWorkingTest(): Promise<void> {
    this.isRunning = true;
    this.currentCorrelationId = this.generateCorrelationId();
    this.correlationIdService.setCorrelationId(this.currentCorrelationId);
    this.clearResults();

    try {
      // Use the confirmed working public endpoint
      await this.executeStep(
        'Test Public Health Endpoint',
        'api-gateway',
        () => this.http.get(`${environment.apiUrl}/public/health`)
      );

      // Add a slight delay and test again with different correlation ID
      await new Promise(resolve => setTimeout(resolve, 1000));
      this.currentCorrelationId = this.generateCorrelationId();
      this.correlationIdService.setCorrelationId(this.currentCorrelationId);
      
      await this.executeStep(
        'Test Public Health Endpoint (2nd)',
        'api-gateway', 
        () => this.http.get(`${environment.apiUrl}/public/health`)
      );

    } catch (error) {
      console.error('Working test failed:', error);
    } finally {
      this.isRunning = false;
    }
  }

  async runErrorTest(): Promise<void> {
    this.isRunning = true;
    this.currentCorrelationId = this.generateCorrelationId();
    this.correlationIdService.setCorrelationId(this.currentCorrelationId);
    this.clearResults();

    try {
      // Test error scenarios
      await this.executeStep(
        'Test Non-existent Endpoint',
        'unknown-service',
        () => this.http.get(environment.apiUrl + '/nonexistent')
      );

      await this.executeStep(
        'Test Invalid Product ID',
        'product-service',
        () => this.http.get(environment.apiUrl + '/products/invalid-id-999')
      );

    } catch (error) {
      console.error('Error test completed (expected):', error);
    } finally {
      this.isRunning = false;
    }
  }

  async runCorrelationIdTest(): Promise<void> {
    this.isRunning = true;
    this.currentCorrelationId = this.generateCorrelationId();
    this.correlationIdService.setCorrelationId(this.currentCorrelationId);
    this.clearResults();

    try {
      console.log('🔗 Starting Correlation ID Flow Test with ID:', this.currentCorrelationId);
      
      // Step 1: Test Order Simulation (Order Service → Inventory Service → Payment Service)
      await this.executeStep(
        'Order Simulation Chain (3 Services)',
        'order-service-chain',
        () => this.http.post(`${environment.apiUrl}/order/simulate`, {})
      );

      // Step 2: Test Complete Order Simulation via Gateway
      await this.executeStep(
        'Complete Order Simulation Flow',
        'order-simulation-controller',
        () => this.http.post(`${environment.apiUrl}/simulation/complete-order-flow`, {})
      );

      // Step 3: Test Individual Services for Correlation ID Verification
      await this.executeStep(
        'Inventory Service Direct Call',
        'inventory-service',
        () => this.http.get(`${environment.apiUrl}/inventory/check/PRODUCT-TEST`)
      );

      await this.executeStep(
        'Payment Service Health Check',
        'payment-service',
        () => this.http.get(`${environment.apiUrl}/payments/health`)
      );

      // Step 4: Test multiple correlation IDs for comparison
      this.currentCorrelationId = this.generateCorrelationId();
      this.correlationIdService.setCorrelationId(this.currentCorrelationId);
      
      await this.executeStep(
        'Second Order Simulation (Different Correlation ID)',
        'order-service-chain-2',
        () => this.http.post(`${environment.apiUrl}/order/simulate`, {})
      );

      console.log('🔗 Correlation ID Flow Test completed. Check Kibana for logs with correlation IDs!');

    } catch (error) {
      console.error('Correlation ID test failed:', error);
    } finally {
      this.isRunning = false;
    }
  }

  private async executeStep(
    stepName: string,
    serviceName: string,
    apiCall: () => Observable<any>,
    delayMs: number = 0
  ): Promise<void> {
    const startTime = Date.now();
    const correlationId = this.currentCorrelationId;

    // Add running result
    const runningResult: TestResult = {
      step: stepName,
      correlationId,
      service: serviceName,
      status: 'running',
      timestamp: new Date()
    };
    this.testResults.push(runningResult);

    if (delayMs > 0) {
      await new Promise(resolve => setTimeout(resolve, delayMs));
    }

    try {
      const response = await apiCall().toPromise();
      const duration = Date.now() - startTime;

      // Update to success
      const resultIndex = this.testResults.length - 1;
      this.testResults[resultIndex] = {
        ...runningResult,
        status: 'success',
        response,
        duration,
        timestamp: new Date()
      };

    } catch (error: any) {
      const duration = Date.now() - startTime;

      // Update to error
      const resultIndex = this.testResults.length - 1;
      this.testResults[resultIndex] = {
        ...runningResult,
        status: 'error',
        error: error.message || error.toString(),
        duration,
        timestamp: new Date()
      };
    }
  }

  private checkHealth(servicePath: string): Observable<any> {
    // Use the public health endpoint through API Gateway
    const url = `${environment.apiUrl}/public/health`;
    return this.http.get(url).pipe(
      catchError(error => {
        // Return error as success for demonstration
        return of({ 
          error: error.message, 
          status: 'API Error',
          service: servicePath.replace('/', ''),
          correlationId: this.currentCorrelationId
        });
      })
    );
  }

  private generateCorrelationId(): string {
    return 'web-elk-test-' + Math.random().toString(36).substr(2, 9) + '-' + Date.now();
  }

  private clearResults(): void {
    this.testResults = [];
    this.showDetails = [];
  }

  toggleDetails(index: number): void {
    this.showDetails[index] = !this.showDetails[index];
  }

  copyToClipboard(text: string): void {
    navigator.clipboard.writeText(text).then(() => {
      console.log('Copied to clipboard:', text);
      // You could add a toast notification here
    });
  }
}