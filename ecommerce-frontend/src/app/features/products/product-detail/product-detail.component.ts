import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject, takeUntil, catchError, of } from 'rxjs';
import { Product } from '@shared/models/product.model';
import { ProductService } from '@core/services/product.service';
import { CartService } from '@core/services/cart.service';
import { InventoryService, InventoryDto } from '@core/services/inventory.service';
import { AddToCartRequest } from '@shared/models/cart.model';

@Component({
  selector: 'app-product-detail',
  template: `
    <div class="product-detail-container">
      <!-- Loading State -->
      <div *ngIf="isLoading" class="loading-container">
        <mat-progress-bar mode="indeterminate"></mat-progress-bar>
        <p>Loading product details...</p>
      </div>

      <!-- Error State -->
      <div *ngIf="error && !isLoading" class="error-container">
        <mat-icon color="warn">error</mat-icon>
        <h2>Product not found</h2>
        <p>{{ error }}</p>
        <button mat-raised-button color="primary" (click)="goBack()">
          <mat-icon>arrow_back</mat-icon>
          Go Back
        </button>
      </div>

      <!-- Product Detail -->
      <div *ngIf="product && !isLoading && !error" class="product-detail-content">
        <!-- Breadcrumb -->
        <nav class="breadcrumb-nav">
          <a mat-button routerLink="/products">
            <mat-icon>arrow_back</mat-icon>
            Back to Products
          </a>
        </nav>

        <div class="product-main">
          <!-- Product Image -->
          <div class="product-image-section">
            <div class="main-image">
              <img [src]="productService.getProductImageUrl(product)" 
                   [alt]="product.name"
                   class="product-image"
                   (error)="onImageError($event)">
            </div>
            <!-- Product Badges -->
            <div class="product-badges">
              <mat-chip class="sale-badge" *ngIf="productService.isOnSale(product)">
                Save {{ productService.getSavingsAmount(product) | currency }}
              </mat-chip>
              <mat-chip class="bestseller-badge" *ngIf="product.isBestSeller">
                Best Seller
              </mat-chip>
              <!-- Stock Status Badge -->
              <mat-chip [ngClass]="getStockBadgeClasses()" *ngIf="inventory">
                {{ getStockStatusText() }}
              </mat-chip>
            </div>
          </div>

          <!-- Product Info -->
          <div class="product-info-section">
            <div class="product-header">
              <h1 class="product-title">{{ product.name }}</h1>
              <p class="product-brand">{{ product.brand }}</p>
              <p class="product-category">Category: {{ product.categoryName }}</p>
            </div>

            <!-- Product Rating -->
            <div class="product-rating" *ngIf="product.stars">
              <div class="stars">
                <mat-icon *ngFor="let star of getStars(product.stars)" 
                         [class.filled]="star <= product.stars">
                  {{ star <= product.stars ? 'star' : 'star_border' }}
                </mat-icon>
              </div>
              <span class="rating-value">({{ product.stars }}/5)</span>
              <span class="review-count">{{ product.reviewCount || 0 }} reviews</span>
            </div>

            <!-- Pricing -->
            <div class="pricing-section">
              <div class="price-display">
                <span class="current-price">{{ product.price | currency }}</span>
                <span *ngIf="product.compareAtPrice" class="original-price">
                  {{ product.compareAtPrice | currency }}
                </span>
              </div>
              <div *ngIf="productService.isOnSale(product)" class="savings-info">
                <span class="savings-amount">
                  You save {{ productService.getSavingsAmount(product) | currency }}
                  ({{ getDiscountPercentage() }}% off)
                </span>
              </div>
            </div>

            <!-- Stock Information -->
            <div class="stock-section">
              <div class="stock-status">
                <mat-icon [class]="getStockIconClass()">{{ getStockIcon() }}</mat-icon>
                <span [class]="getStockTextClass()">{{ getStockStatusText() }}</span>
              </div>
              <div *ngIf="isLowStock() && inventory && inventory.availableQuantity > 0" class="low-stock-warning">
                <mat-icon color="warn">warning</mat-icon>
                <span>Only {{ inventory.availableQuantity }} left in stock</span>
              </div>
              <div *ngIf="!inventory || inventory.availableQuantity === 0" class="out-of-stock-info">
                <mat-icon color="warn">info</mat-icon>
                <span>This product is currently unavailable</span>
              </div>
            </div>

            <!-- Description -->
            <div class="description-section" *ngIf="product.description">
              <h3>Description</h3>
              <p class="product-description">{{ product.description }}</p>
            </div>

            <!-- Product Specs -->
            <div class="specs-section" *ngIf="product.weight || product.dimensions">
              <h3>Specifications</h3>
              <div class="specs-grid">
                <div *ngIf="product.weight" class="spec-item">
                  <span class="spec-label">Weight:</span>
                  <span class="spec-value">{{ product.weight }}g</span>
                </div>
                <div *ngIf="product.dimensions" class="spec-item">
                  <span class="spec-label">Dimensions:</span>
                  <span class="spec-value">{{ product.dimensions }}</span>
                </div>
              </div>
            </div>

            <!-- Add to Cart Section -->
            <div class="add-to-cart-section">
              <div class="quantity-selector">
                <button mat-icon-button 
                        [disabled]="quantity <= 1" 
                        (click)="decreaseQuantity()">
                  <mat-icon>remove</mat-icon>
                </button>
                <span class="quantity-display">{{ quantity }}</span>
                <button mat-icon-button 
                        [disabled]="!canIncreaseQuantity()" 
                        (click)="increaseQuantity()">
                  <mat-icon>add</mat-icon>
                </button>
              </div>
              <button mat-raised-button 
                     [color]="canAddToCart() ? 'primary' : 'warn'"
                     [disabled]="!canAddToCart()"
                     (click)="addToCart()"
                     class="add-to-cart-btn">
                <mat-icon>shopping_cart</mat-icon>
                {{ getAddToCartButtonText() }}
              </button>
            </div>

            <!-- Additional Actions -->
            <div class="action-buttons">
              <button mat-button color="accent" (click)="shareProduct()">
                <mat-icon>share</mat-icon>
                Share
              </button>
              <button mat-button color="accent" (click)="addToWishlist()">
                <mat-icon>favorite_border</mat-icon>
                Add to Wishlist
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .product-detail-container {
      min-height: 100vh;
      background: #f8f9fa;
    }

    .loading-container, .error-container {
      text-align: center;
      padding: 3rem;
    }

    .error-container h2 {
      color: #dc3545;
      margin: 1rem 0;
    }

    .product-detail-content {
      max-width: 1200px;
      margin: 0 auto;
      padding: 2rem;
    }

    .breadcrumb-nav {
      margin-bottom: 2rem;
    }

    .product-main {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 3rem;
      background: white;
      border-radius: 8px;
      padding: 2rem;
      box-shadow: 0 2px 8px rgba(0,0,0,0.1);
    }

    .product-image-section {
      position: relative;
    }

    .main-image {
      width: 100%;
      max-height: 500px;
      overflow: hidden;
      border-radius: 8px;
      background: #f8f9fa;
    }

    .product-image {
      width: 100%;
      height: 100%;
      object-fit: cover;
      max-height: 500px;
    }

    .product-badges {
      position: absolute;
      top: 1rem;
      right: 1rem;
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
    }

    .sale-badge {
      background: #28a745;
      color: white;
    }

    .bestseller-badge {
      background: #ffc107;
      color: #212529;
    }

    .stock-badge.bg-success {
      background: #28a745;
      color: white;
    }

    .stock-badge.bg-warning {
      background: #ffc107;
      color: #212529;
    }

    .stock-badge.bg-danger {
      background: #dc3545;
      color: white;
    }

    .product-info-section {
      display: flex;
      flex-direction: column;
      gap: 1.5rem;
    }

    .product-header h1 {
      margin: 0 0 0.5rem 0;
      color: #212529;
      font-size: 2rem;
      line-height: 1.3;
    }

    .product-brand {
      color: #6c757d;
      font-size: 1.1rem;
      margin: 0;
      font-weight: 500;
    }

    .product-category {
      color: #6c757d;
      font-size: 0.9rem;
      margin: 0;
    }

    .product-rating {
      display: flex;
      align-items: center;
      gap: 1rem;
    }

    .stars {
      display: flex;
      color: #ffc107;
    }

    .stars mat-icon {
      font-size: 1.2rem;
      width: 1.2rem;
      height: 1.2rem;
    }

    .rating-value {
      font-weight: 600;
      color: #495057;
    }

    .review-count {
      color: #6c757d;
      font-size: 0.9rem;
    }

    .pricing-section {
      padding: 1rem 0;
      border-top: 1px solid #dee2e6;
      border-bottom: 1px solid #dee2e6;
    }

    .price-display {
      display: flex;
      align-items: center;
      gap: 1rem;
      margin-bottom: 0.5rem;
    }

    .current-price {
      font-size: 2rem;
      font-weight: 700;
      color: #28a745;
    }

    .original-price {
      font-size: 1.2rem;
      text-decoration: line-through;
      color: #6c757d;
    }

    .savings-info {
      color: #28a745;
      font-weight: 600;
    }

    .stock-section {
      background: #f8f9fa;
      padding: 1rem;
      border-radius: 4px;
    }

    .stock-status {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      margin-bottom: 0.5rem;
    }

    .low-stock-warning {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      color: #856404;
      background: #fff3cd;
      padding: 0.5rem;
      border-radius: 4px;
      border: 1px solid #ffeaa7;
    }

    .out-of-stock-info {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      color: #721c24;
      background: #f8d7da;
      padding: 0.5rem;
      border-radius: 4px;
      border: 1px solid #f5c6cb;
    }

    .stock-in-stock { color: #28a745; }
    .stock-low-stock { color: #ffc107; }
    .stock-out-of-stock { color: #dc3545; }

    .description-section h3,
    .specs-section h3 {
      color: #495057;
      margin: 0 0 1rem 0;
      font-size: 1.3rem;
    }

    .product-description {
      line-height: 1.6;
      color: #495057;
    }

    .specs-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 1rem;
    }

    .spec-item {
      display: flex;
      justify-content: space-between;
      padding: 0.5rem;
      background: #f8f9fa;
      border-radius: 4px;
    }

    .spec-label {
      font-weight: 500;
      color: #495057;
    }

    .spec-value {
      color: #6c757d;
    }

    .add-to-cart-section {
      display: flex;
      align-items: center;
      gap: 1rem;
      padding: 1.5rem 0;
      border-top: 1px solid #dee2e6;
    }

    .quantity-selector {
      display: flex;
      align-items: center;
      border: 1px solid #dee2e6;
      border-radius: 4px;
      background: white;
    }

    .quantity-display {
      padding: 0.5rem 1rem;
      font-weight: 600;
      min-width: 3rem;
      text-align: center;
    }

    .add-to-cart-btn {
      flex: 1;
      padding: 0.75rem 2rem;
      font-size: 1.1rem;
      font-weight: 600;
    }

    .action-buttons {
      display: flex;
      gap: 1rem;
    }

    @media (max-width: 768px) {
      .product-detail-content {
        padding: 1rem;
      }

      .product-main {
        grid-template-columns: 1fr;
        gap: 2rem;
        padding: 1rem;
      }

      .product-header h1 {
        font-size: 1.5rem;
      }

      .current-price {
        font-size: 1.5rem;
      }

      .add-to-cart-section {
        flex-direction: column;
        align-items: stretch;
      }

      .quantity-selector {
        justify-content: center;
      }
    }
  `]
})
export class ProductDetailComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  
  product: Product | null = null;
  inventory: InventoryDto | null = null;
  isLoading = false;
  error: string | null = null;
  quantity = 1;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    public productService: ProductService,
    private cartService: CartService,
    private inventoryService: InventoryService
  ) {}

  ngOnInit(): void {
    this.route.params.pipe(takeUntil(this.destroy$)).subscribe(params => {
      const productId = parseInt(params['id']);
      if (productId) {
        this.loadProduct(productId);
      }
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadProduct(productId: number): void {
    this.isLoading = true;
    this.error = null;

    this.productService.getProduct(productId).pipe(
      takeUntil(this.destroy$),
      catchError(error => {
        console.error('Failed to load product:', error);
        this.error = error.status === 404 ? 'Product not found' : 'Failed to load product details';
        this.isLoading = false;
        return of(null);
      })
    ).subscribe(product => {
      if (product) {
        this.product = product;
        this.loadInventory(productId);
      }
      this.isLoading = false;
    });
  }

  private loadInventory(productId: number): void {
    this.inventoryService.getInventoryForProduct(productId).pipe(
      takeUntil(this.destroy$),
      catchError(error => {
        console.error('Failed to load inventory:', error);
        // Return a default inventory object indicating no stock instead of null
        return of({
          id: '',
          productId: productId,
          quantity: 0,
          reservedQuantity: 0,
          availableQuantity: 0,
          reorderLevel: 0,
          maxStockLevel: 0,
          isLowStock: false,
          version: 0,
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString()
        } as InventoryDto);
      })
    ).subscribe((inventory: InventoryDto | null) => {
      this.inventory = inventory;
    });
  }

  onImageError(event: any): void {
    event.target.src = '/assets/images/product-placeholder.svg';
  }

  getStars(rating: number): number[] {
    return Array(5).fill(0).map((_, i) => i + 1);
  }

  getDiscountPercentage(): number {
    if (!this.product?.compareAtPrice) return 0;
    return Math.round(((this.product.compareAtPrice - this.product.price) / this.product.compareAtPrice) * 100);
  }

  getStockStatusText(): string {
    if (!this.inventory || this.inventory.availableQuantity === 0) return 'Out of Stock';
    const stockStatus = this.inventoryService.getStockStatus(this.inventory);
    return this.inventoryService.getStockStatusText(stockStatus.stockStatus, this.inventory.availableQuantity);
  }

  getStockBadgeClasses(): string[] {
    const classes = ['stock-badge'];
    
    if (!this.inventory || this.inventory.availableQuantity === 0) {
      classes.push('bg-danger');
    } else {
      const stockStatus = this.inventoryService.getStockStatus(this.inventory);
      if (stockStatus.stockStatus === 'IN_STOCK') classes.push('bg-success');
      else if (stockStatus.stockStatus === 'LOW_STOCK') classes.push('bg-warning');
      else if (stockStatus.stockStatus === 'OUT_OF_STOCK') classes.push('bg-danger');
      else classes.push('bg-secondary');
    }
    
    return classes;
  }

  getStockIcon(): string {
    if (!this.inventory || this.inventory.availableQuantity === 0) return 'cancel';
    const stockStatus = this.inventoryService.getStockStatus(this.inventory);
    
    if (stockStatus.stockStatus === 'IN_STOCK') return 'check_circle';
    else if (stockStatus.stockStatus === 'LOW_STOCK') return 'warning';
    else if (stockStatus.stockStatus === 'OUT_OF_STOCK') return 'cancel';
    else return 'help';
  }

  getStockIconClass(): string {
    if (!this.inventory || this.inventory.availableQuantity === 0) return 'stock-out-of-stock';
    const stockStatus = this.inventoryService.getStockStatus(this.inventory);
    
    if (stockStatus.stockStatus === 'IN_STOCK') return 'stock-in-stock';
    else if (stockStatus.stockStatus === 'LOW_STOCK') return 'stock-low-stock';
    else if (stockStatus.stockStatus === 'OUT_OF_STOCK') return 'stock-out-of-stock';
    else return '';
  }

  getStockTextClass(): string {
    return this.getStockIconClass();
  }

  isLowStock(): boolean {
    if (!this.inventory || this.inventory.availableQuantity === 0) return false;
    const stockStatus = this.inventoryService.getStockStatus(this.inventory);
    return stockStatus.isLowStock;
  }

  canAddToCart(): boolean {
    if (!this.inventory || this.inventory.availableQuantity === 0) return false;
    const stockStatus = this.inventoryService.getStockStatus(this.inventory);
    return stockStatus.inStock && this.inventory.availableQuantity >= this.quantity;
  }

  canIncreaseQuantity(): boolean {
    if (!this.inventory || this.inventory.availableQuantity === 0) return false;
    return this.quantity < this.inventory.availableQuantity;
  }

  getAddToCartButtonText(): string {
    if (!this.inventory) return 'Loading...';
    if (this.inventory.availableQuantity === 0) return 'Out of Stock';
    
    const stockStatus = this.inventoryService.getStockStatus(this.inventory);
    
    if (!stockStatus.inStock) return 'Out of Stock';
    if (stockStatus.isLowStock) return `Add ${this.quantity} to Cart (Limited Stock)`;
    return `Add ${this.quantity} to Cart`;
  }

  increaseQuantity(): void {
    if (this.canIncreaseQuantity()) {
      this.quantity++;
    }
  }

  decreaseQuantity(): void {
    if (this.quantity > 1) {
      this.quantity--;
    }
  }

  addToCart(): void {
    if (!this.product || !this.canAddToCart()) return;

    const request: AddToCartRequest = {
      productId: this.product.id,
      quantity: this.quantity
    };

    this.cartService.addToCart(request).subscribe({
      next: () => {
        console.log(`Added ${this.quantity} x ${this.product!.name} to cart`);
        // Could show success snackbar here
      },
      error: (error) => {
        console.error('Failed to add product to cart:', error);
        // Could show error snackbar here
      }
    });
  }

  shareProduct(): void {
    if (navigator.share && this.product) {
      navigator.share({
        title: this.product.name,
        text: `Check out this product: ${this.product.name}`,
        url: window.location.href
      });
    } else {
      // Fallback to clipboard
      navigator.clipboard.writeText(window.location.href);
      console.log('Product URL copied to clipboard');
    }
  }

  addToWishlist(): void {
    // Placeholder for wishlist functionality
    console.log('Add to wishlist clicked');
  }

  goBack(): void {
    this.router.navigate(['/products']);
  }
}