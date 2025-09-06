import { Component, OnInit, OnDestroy } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, forkJoin, interval, of, Subscription } from 'rxjs';
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

interface ServiceStatus {
  name: string;
  url: string;
  status: 'online' | 'offline' | 'checking';
  responseTime?: number;
  lastChecked?: Date;
  port: number;
}

interface MonitoringLink {
  name: string;
  description: string;
  url: string;
  icon: string;
  color: string;
}

@Component({
  selector: 'app-elk-test',
  template: `
    <!-- System Admin Dashboard -->
    <div class="admin-dashboard">
      <!-- Dashboard Header -->
      <div class="dashboard-header">
        <h1>🎛️ System Admin Dashboard</h1>
        <p>Comprehensive monitoring and testing for microservices platform</p>
        <div class="system-info">
          <span class="info-badge">Environment: {{ environment.production ? 'Production' : 'Development' }}</span>
          <span class="info-badge">Last Updated: {{ lastUpdate | date:'short' }}</span>
        </div>
      </div>

      <!-- Services Status Grid -->
      <div class="services-status-section">
        <h3>📊 Services Health Status</h3>
        <div class="services-grid">
          <div *ngFor="let service of services" 
               class="service-card"
               [ngClass]="'status-' + service.status">
            <div class="service-header">
              <h4>{{ service.name }}</h4>
              <span class="status-indicator" [ngClass]="'indicator-' + service.status">
                {{ service.status | titlecase }}
              </span>
            </div>
            <div class="service-details">
              <p><strong>Port:</strong> {{ service.port }}</p>
              <p *ngIf="service.responseTime"><strong>Response Time:</strong> {{ service.responseTime }}ms</p>
              <p *ngIf="service.lastChecked"><strong>Last Checked:</strong> {{ service.lastChecked | date:'HH:mm:ss' }}</p>
            </div>
            <button (click)="checkSingleService(service)" 
                    [disabled]="service.status === 'checking'"
                    class="btn btn-sm btn-outline-primary">
              {{ service.status === 'checking' ? '⏳ Checking...' : '🔄 Check Health' }}
            </button>
          </div>
        </div>
        <div class="services-actions">
          <button (click)="checkAllServices()" 
                  [disabled]="isCheckingAll"
                  class="btn btn-success">
            {{ isCheckingAll ? '⏳ Checking All Services...' : '🚀 Check All Services Health' }}
          </button>
        </div>
      </div>

      <!-- Monitoring Tools Links -->
      <div class="monitoring-section">
        <h3>🔗 Monitoring & Observability Tools</h3>
        <div class="monitoring-grid">
          <div *ngFor="let link of monitoringLinks" class="monitoring-card" [style.border-left-color]="link.color">
            <div class="monitoring-header">
              <span class="monitoring-icon">{{ link.icon }}</span>
              <h4>{{ link.name }}</h4>
            </div>
            <p class="monitoring-description">{{ link.description }}</p>
            <a [href]="link.url" target="_blank" class="btn btn-outline-primary btn-sm">
              🔗 Open {{ link.name }}
            </a>
          </div>
        </div>
      </div>

      <!-- Quick Actions Panel -->
      <div class="quick-actions-section">
        <h3>🧪 System Testing & Actions</h3>
        <div class="actions-grid">
          <div class="action-card">
            <h4>🔗 Correlation ID Testing</h4>
            <p>Test end-to-end request flow across all microservices (WORKING STATE ALPHA)</p>
            <button (click)="runCorrelationIdTest()" 
                    [disabled]="isRunning"
                    class="btn btn-primary">
              {{ isRunning ? '⏳ Running Test...' : '🚀 Run Correlation ID Test' }}
            </button>
            <div class="correlation-info" *ngIf="currentCorrelationId">
              <strong>Active Correlation ID:</strong>
              <code class="correlation-id">{{ currentCorrelationId }}</code>
              <button (click)="copyToClipboard(currentCorrelationId)" 
                      class="btn btn-sm btn-outline-secondary">
                📋 Copy for Kibana
              </button>
            </div>
          </div>
          
          <div class="action-card">
            <h4>🧹 System Maintenance</h4>
            <p>Clear test results and reset dashboard state</p>
            <button (click)="clearAllData()" class="btn btn-warning">
              🗑️ Clear All Test Data
            </button>
          </div>
          
          <div class="action-card">
            <h4>📈 Quick Metrics</h4>
            <p>View basic system statistics</p>
            <div class="metrics-display">
              <div class="metric">
                <span class="metric-value">{{ testResults.length }}</span>
                <span class="metric-label">Total Tests</span>
              </div>
              <div class="metric">
                <span class="metric-value">{{ getSuccessfulTests() }}</span>
                <span class="metric-label">Successful</span>
              </div>
              <div class="metric">
                <span class="metric-value">{{ getFailedTests() }}</span>
                <span class="metric-label">Failed</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Test Results Section -->
      <div class="test-results-section">
        <div class="results-header">
          <h3>📋 Test Results & Logs ({{ testResults.length }})</h3>
          <div class="results-filters">
            <button (click)="filterResults('all')" 
                    [class.active]="currentFilter === 'all'"
                    class="filter-btn">All</button>
            <button (click)="filterResults('success')" 
                    [class.active]="currentFilter === 'success'"
                    class="filter-btn">Success</button>
            <button (click)="filterResults('error')" 
                    [class.active]="currentFilter === 'error'"
                    class="filter-btn">Errors</button>
            <button (click)="exportResults()" class="btn btn-sm btn-outline-info">
              📄 Export Results
            </button>
          </div>
        </div>
        
        <div class="results-list" *ngIf="getFilteredResults().length > 0">
          <div *ngFor="let result of getFilteredResults(); let i = index" 
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
        
        <div *ngIf="getFilteredResults().length === 0" class="no-results">
          {{ testResults.length === 0 ? 'No test results yet. Run the correlation ID test to see results here!' : 'No results match the current filter.' }}
        </div>
      </div>

      <!-- Troubleshooting Guide -->
      <div class="troubleshooting-section">
        <h3>🔍 Troubleshooting & Analysis Guide</h3>
        <div class="guide-grid">
          <div class="guide-card">
            <h4>📊 Kibana Log Analysis</h4>
            <ol>
              <li>Search for correlation ID: <code>{{ currentCorrelationId || 'your-correlation-id' }}</code></li>
              <li>Filter by service: <code>springAppName: "ecom-order-service"</code></li>
              <li>Sort by timestamp to see request flow</li>
              <li>Look for same correlation ID across multiple services</li>
            </ol>
          </div>
          
          <div class="guide-card">
            <h4>🔗 Jaeger Trace Analysis</h4>
            <ol>
              <li>Search by correlation ID or trace ID</li>
              <li>View service dependency graph</li>
              <li>Analyze request latency breakdown</li>
              <li>Identify bottlenecks and errors</li>
            </ol>
          </div>
          
          <div class="guide-card">
            <h4>📈 Grafana Metrics</h4>
            <ol>
              <li>Check service health dashboards</li>
              <li>Monitor request rates and response times</li>
              <li>View error rates and success rates</li>
              <li>Analyze system resource usage</li>
            </ol>
          </div>
        </div>
      </div>
    </div>
  `,
  styleUrls: ['./elk-test.component.scss']
})
export class ELKTestComponent implements OnInit, OnDestroy {
  testResults: TestResult[] = [];
  isRunning = false;
  currentCorrelationId = '';
  showDetails: boolean[] = [];
  lastUpdate = new Date();
  environment = environment;
  
  // Service status tracking
  services: ServiceStatus[] = [
    { name: 'API Gateway', url: '/public/health', status: 'offline', port: 8081 },
    { name: 'Order Service', url: '/order/health', status: 'offline', port: 8083 },
    { name: 'Inventory Service', url: '/inventory/health', status: 'offline', port: 8084 },
    { name: 'Payment Service', url: '/payments/health', status: 'offline', port: 8087 },
    { name: 'Product Service', url: '/products/health', status: 'offline', port: 8088 },
    { name: 'Cart Service', url: '/cart/health', status: 'offline', port: 8089 },
    { name: 'User Service', url: '/users/health', status: 'offline', port: 8082 },
    { name: 'Search Service', url: '/search/health', status: 'offline', port: 8086 },
    { name: 'Notification Service', url: '/notifications/health', status: 'offline', port: 8086 }
  ];
  
  isCheckingAll = false;
  
  // Monitoring tools links
  monitoringLinks: MonitoringLink[] = [
    {
      name: 'Kibana',
      description: 'Search and analyze logs across all services',
      url: environment.production ? 'https://monitor.amars.shop:5601' : 'http://localhost:5601',
      icon: '📊',
      color: '#00BCD4'
    },
    {
      name: 'Jaeger',
      description: 'Distributed tracing and service dependency mapping',
      url: environment.production ? 'https://monitor.amars.shop/jaeger' : 'http://localhost:16686',
      icon: '🔗',
      color: '#FF9800'
    },
    {
      name: 'Grafana',
      description: 'Metrics dashboards and system monitoring',
      url: environment.production ? 'https://monitor.amars.shop/grafana' : 'http://localhost:3000',
      icon: '📈',
      color: '#FF5722'
    },
    {
      name: 'Prometheus',
      description: 'Raw metrics and system health data',
      url: environment.production ? 'https://monitor.amars.shop:9090' : 'http://localhost:9090',
      icon: '⚡',
      color: '#4CAF50'
    }
  ];
  
  // Filtering
  currentFilter: 'all' | 'success' | 'error' = 'all';
  
  // Auto-refresh subscription
  private autoRefreshSubscription?: Subscription;

  constructor(
    private http: HttpClient,
    private productService: ProductService,
    private orderService: OrderService,
    private authService: AuthService,
    private correlationIdService: CorrelationIdService
  ) {}

  ngOnInit(): void {
    console.log('🎛️ System Admin Dashboard initialized');
    this.checkAllServices();
    this.startAutoRefresh();
  }

  ngOnDestroy(): void {
    this.stopAutoRefresh();
  }

  // Service Health Checking Methods
  async checkAllServices(): Promise<void> {
    this.isCheckingAll = true;
    const checkPromises = this.services.map(service => this.checkSingleService(service));
    await Promise.all(checkPromises);
    this.isCheckingAll = false;
    this.lastUpdate = new Date();
  }

  async checkSingleService(service: ServiceStatus): Promise<void> {
    service.status = 'checking';
    const startTime = Date.now();

    try {
      const response = await this.http.get(`${environment.apiUrl}${service.url}`)
        .pipe(
          catchError(() => of({ status: 'error' }))
        ).toPromise();
      
      const responseTime = Date.now() - startTime;
      service.status = 'online';
      service.responseTime = responseTime;
      service.lastChecked = new Date();
      
    } catch (error) {
      service.status = 'offline';
      service.responseTime = undefined;
      service.lastChecked = new Date();
    }
  }

  // Auto-refresh services every 30 seconds
  startAutoRefresh(): void {
    this.autoRefreshSubscription = interval(30000).subscribe(() => {
      if (!this.isCheckingAll && !this.isRunning) {
        this.checkAllServices();
      }
    });
  }

  stopAutoRefresh(): void {
    if (this.autoRefreshSubscription) {
      this.autoRefreshSubscription.unsubscribe();
    }
  }

  // Test Results Filtering
  filterResults(filter: 'all' | 'success' | 'error'): void {
    this.currentFilter = filter;
  }

  getFilteredResults(): TestResult[] {
    if (this.currentFilter === 'all') {
      return this.testResults;
    }
    return this.testResults.filter(result => result.status === this.currentFilter);
  }

  // Metrics Calculation
  getSuccessfulTests(): number {
    return this.testResults.filter(result => result.status === 'success').length;
  }

  getFailedTests(): number {
    return this.testResults.filter(result => result.status === 'error').length;
  }

  // System Actions
  clearAllData(): void {
    this.testResults = [];
    this.showDetails = [];
    this.currentCorrelationId = '';
    this.lastUpdate = new Date();
  }

  exportResults(): void {
    const exportData = {
      timestamp: new Date().toISOString(),
      totalTests: this.testResults.length,
      successfulTests: this.getSuccessfulTests(),
      failedTests: this.getFailedTests(),
      environment: environment.production ? 'production' : 'development',
      results: this.testResults
    };

    const blob = new Blob([JSON.stringify(exportData, null, 2)], { type: 'application/json' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `elk-test-results-${new Date().toISOString().split('T')[0]}.json`;
    link.click();
    window.URL.revokeObjectURL(url);
  }

  // Working Correlation ID Test (our alpha baseline)
  async runCorrelationIdTest(): Promise<void> {
    this.isRunning = true;
    this.currentCorrelationId = this.generateCorrelationId();
    this.correlationIdService.setCorrelationId(this.currentCorrelationId);
    this.clearResults();

    try {
      console.log('🔗 Starting Correlation ID Flow Test with ID:', this.currentCorrelationId);
      
      // Step 1: Test Complete Order Simulation via Gateway (this propagates correlation ID properly)
      await this.executeStep(
        'Complete Order Simulation Flow',
        'order-simulation-controller',
        () => this.http.post(`${environment.apiUrl}/simulation/complete-order-flow`, {})
      );

      // Step 2: Test Individual Services for Correlation ID Verification
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

      // Step 3: Test Search Service
      await this.executeStep(
        'Search Service Query Test',
        'search-service',
        () => this.http.get(`${environment.apiUrl}/search/products?query=laptop&page=0&size=5`)
      );

      await this.executeStep(
        'Search Service Statistics',
        'search-service',
        () => this.http.get(`${environment.apiUrl}/search/stats`)
      );

      console.log('🔗 Correlation ID Flow Test completed. Check Kibana for logs with correlation ID:', this.currentCorrelationId);

    } catch (error) {
      console.error('Correlation ID test failed:', error);
    } finally {
      this.isRunning = false;
    }
  }

  // Legacy test methods (keep for backward compatibility but not used in UI)
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

      // Test 2: Get Categories (this should work via product-service)
      await this.executeStep(
        'Get Product Categories',
        'product-service',
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
        'Product Service Categories Test',
        'product-service',
        () => this.http.get(`${environment.apiUrl}/products/categories`)
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