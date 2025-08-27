import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable, BehaviorSubject, combineLatest, Subject, of } from 'rxjs';
import { debounceTime, switchMap, takeUntil, startWith, catchError } from 'rxjs/operators';
import { PageEvent } from '@angular/material/paginator';
import { Product, ProductSearchResult, ProductCategory } from '@shared/models/product.model';
import { ProductService } from '@core/services/product.service';
import { Page, PageRequest, ProductFilters } from '@shared/models/page.model';

@Component({
  selector: 'app-products',
  template: `
    <div class="product-catalog-container">
      <!-- Header Section -->
      <div class="catalog-header">
        <div class="header-content">
          <h1 class="catalog-title">Product Catalog</h1>
          <p class="catalog-subtitle">Discover our collection of {{ totalProducts }} products across {{ totalCategories }} categories</p>
        </div>
        
        <!-- Search Bar -->
        <div class="search-section">
          <mat-form-field appearance="outline" class="search-field">
            <mat-label>Search products...</mat-label>
            <input matInput 
                   [(ngModel)]="searchQuery" 
                   (input)="onSearchChange()"
                   (keyup.enter)="onSearchSubmit()"
                   placeholder="Search by name, brand, or category">
            <mat-icon matSuffix>search</mat-icon>
          </mat-form-field>
        </div>
      </div>

      <div class="catalog-content">
        <!-- Sidebar Filters -->
        <mat-sidenav-container class="sidenav-container">
          <mat-sidenav mode="side" opened class="filter-sidenav">
            <div class="filter-content">
              <h3>Filters</h3>
              
              <!-- Category Filter -->
              <div class="filter-section">
                <h4>Categories</h4>
                <mat-form-field appearance="outline" class="full-width">
                  <mat-label>Select Category</mat-label>
                  <mat-select [(value)]="selectedCategoryId" (selectionChange)="onCategoryChange()">
                    <mat-option [value]="null">All Categories</mat-option>
                    <mat-option *ngFor="let category of categories" [value]="category.id">
                      {{ category.name }}
                    </mat-option>
                  </mat-select>
                </mat-form-field>
              </div>

              <!-- Sort Options -->
              <div class="filter-section">
                <h4>Sort By</h4>
                <mat-form-field appearance="outline" class="full-width">
                  <mat-label>Sort By</mat-label>
                  <mat-select [(value)]="sortBy" (selectionChange)="onSortChange()">
                    <mat-option value="createdAt">Newest First</mat-option>
                    <mat-option value="name">Name A-Z</mat-option>
                    <mat-option value="price">Price Low-High</mat-option>
                    <mat-option value="stars">Highest Rated</mat-option>
                  </mat-select>
                </mat-form-field>
                
                <mat-form-field appearance="outline" class="full-width">
                  <mat-label>Order</mat-label>
                  <mat-select [(value)]="sortDir" (selectionChange)="onSortChange()">
                    <mat-option value="asc">Ascending</mat-option>
                    <mat-option value="desc">Descending</mat-option>
                  </mat-select>
                </mat-form-field>
              </div>

              <!-- Active Filters -->
              <div class="filter-section" *ngIf="hasActiveFilters()">
                <h4>Active Filters</h4>
                <div class="active-filters">
                  <mat-chip-set>
                    <mat-chip *ngIf="searchQuery" 
                             (removed)="clearSearch()" 
                             [removable]="true">
                      Search: {{ searchQuery }}
                      <mat-icon matChipRemove>cancel</mat-icon>
                    </mat-chip>
                    <mat-chip *ngIf="selectedCategoryId" 
                             (removed)="clearCategory()" 
                             [removable]="true">
                      Category: {{ getCategoryName(selectedCategoryId) }}
                      <mat-icon matChipRemove>cancel</mat-icon>
                    </mat-chip>
                  </mat-chip-set>
                  <button mat-button color="warn" (click)="clearAllFilters()">
                    Clear All Filters
                  </button>
                </div>
              </div>
            </div>
          </mat-sidenav>

          <!-- Main Content -->
          <mat-sidenav-content class="main-content">
            <!-- Loading Indicator -->
            <div *ngIf="isLoading" class="loading-container">
              <mat-progress-bar mode="indeterminate"></mat-progress-bar>
              <p>Loading products...</p>
            </div>

            <!-- Error State -->
            <div *ngIf="error && !isLoading" class="error-container">
              <mat-icon color="warn">error</mat-icon>
              <h3>Failed to load products</h3>
              <p>{{ error }}</p>
              <button mat-raised-button color="primary" (click)="retry()">
                <mat-icon>refresh</mat-icon>
                Retry
              </button>
            </div>

            <!-- Product Grid -->
            <div *ngIf="!isLoading && !error" class="products-content">
              <!-- Results Info -->
              <div class="results-info">
                <span class="results-count">
                  Showing {{ (currentPage * pageSize) + 1 }} - {{ Math.min((currentPage + 1) * pageSize, totalProducts) }} 
                  of {{ totalProducts }} products
                </span>
              </div>

              <!-- Product Grid -->
              <div class="product-grid" *ngIf="products.length > 0">
                <mat-card *ngFor="let product of products" class="product-card">
                  <div class="product-image-container">
                    <img mat-card-image 
                         [src]="productService.getProductImageUrl(product)" 
                         [alt]="product.name"
                         class="product-image"
                         (error)="onImageError($event)">
                    <div class="product-badges">
                      <mat-chip class="sale-badge" *ngIf="productService.isOnSale(product)">
                        Save {{ productService.getSavingsAmount(product) | currency }}
                      </mat-chip>
                      <mat-chip class="bestseller-badge" *ngIf="product.isBestSeller">
                        Best Seller
                      </mat-chip>
                    </div>
                  </div>
                  
                  <mat-card-content class="product-info">
                    <h3 class="product-name" [title]="product.name">{{ product.name | slice:0:60 }}...</h3>
                    <p class="product-brand">{{ product.brand }}</p>
                    <p class="product-category">{{ product.categoryName }}</p>
                    
                    <div class="product-pricing">
                      <span class="current-price">{{ product.price | currency }}</span>
                      <span *ngIf="product.compareAtPrice" class="original-price">
                        {{ product.compareAtPrice | currency }}
                      </span>
                    </div>
                    
                    <div class="product-rating" *ngIf="product.stars">
                      <div class="stars">
                        <mat-icon *ngFor="let star of getStars(product.stars)" 
                                 [class.filled]="star <= product.stars">
                          {{ star <= product.stars ? 'star' : 'star_border' }}
                        </mat-icon>
                      </div>
                      <span class="review-count">({{ product.reviewCount || 0 }} reviews)</span>
                    </div>
                  </mat-card-content>
                  
                  <mat-card-actions class="product-actions">
                    <button mat-button color="primary" (click)="viewProduct(product)">
                      <mat-icon>visibility</mat-icon>
                      View Details
                    </button>
                    <button mat-raised-button color="primary" 
                           [disabled]="!product.inStock"
                           (click)="addToCart(product)">
                      <mat-icon>shopping_cart</mat-icon>
                      {{ product.inStock ? 'Add to Cart' : 'Out of Stock' }}
                    </button>
                  </mat-card-actions>
                </mat-card>
              </div>

              <!-- No Results -->
              <div *ngIf="products.length === 0" class="no-results">
                <mat-icon>search_off</mat-icon>
                <h3>No products found</h3>
                <p>Try adjusting your search or filters</p>
                <button mat-raised-button color="primary" (click)="clearAllFilters()">
                  Clear Filters
                </button>
              </div>

              <!-- Pagination -->
              <div class="pagination-container" *ngIf="totalProducts > 0">
                <mat-paginator
                  [length]="totalProducts"
                  [pageSize]="pageSize"
                  [pageIndex]="currentPage"
                  [pageSizeOptions]="pageSizeOptions"
                  (page)="onPageChange($event)"
                  showFirstLastButtons>
                </mat-paginator>
              </div>
            </div>
          </mat-sidenav-content>
        </mat-sidenav-container>
      </div>
    </div>
  `,
  styles: [`
    .product-catalog-container {
      height: 100vh;
      display: flex;
      flex-direction: column;
    }
    
    .catalog-header {
      background: #f8f9fa;
      padding: 1.5rem;
      border-bottom: 1px solid #dee2e6;
    }
    
    .header-content h1 {
      margin: 0 0 0.5rem 0;
      color: #212529;
    }
    
    .catalog-subtitle {
      color: #6c757d;
      margin: 0 0 1rem 0;
    }
    
    .search-field {
      width: 100%;
      max-width: 500px;
    }
    
    .catalog-content {
      flex: 1;
      display: flex;
    }
    
    .sidenav-container {
      flex: 1;
    }
    
    .filter-sidenav {
      width: 300px;
      padding: 1rem;
      border-right: 1px solid #dee2e6;
    }
    
    .filter-section {
      margin-bottom: 1.5rem;
    }
    
    .filter-section h4 {
      margin: 0 0 1rem 0;
      color: #495057;
    }
    
    .full-width {
      width: 100%;
    }
    
    .active-filters {
      margin-top: 1rem;
    }
    
    .main-content {
      padding: 1.5rem;
      overflow: auto;
    }
    
    .loading-container, .error-container {
      text-align: center;
      padding: 3rem;
    }
    
    .results-info {
      margin-bottom: 1rem;
      color: #6c757d;
    }
    
    .product-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
      gap: 1.5rem;
      margin-bottom: 2rem;
    }
    
    .product-card {
      display: flex;
      flex-direction: column;
      height: 100%;
    }
    
    .product-image-container {
      position: relative;
      height: 200px;
      overflow: hidden;
    }
    
    .product-image {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
    
    .product-badges {
      position: absolute;
      top: 8px;
      right: 8px;
      display: flex;
      flex-direction: column;
      gap: 4px;
    }
    
    .sale-badge {
      background: #28a745;
      color: white;
    }
    
    .bestseller-badge {
      background: #ffc107;
      color: #212529;
    }
    
    .product-info {
      flex: 1;
    }
    
    .product-name {
      margin: 0 0 0.5rem 0;
      font-weight: 500;
      line-height: 1.3;
    }
    
    .product-brand, .product-category {
      color: #6c757d;
      margin: 0 0 0.5rem 0;
      font-size: 0.875rem;
    }
    
    .product-pricing {
      margin: 1rem 0;
    }
    
    .current-price {
      font-size: 1.25rem;
      font-weight: 600;
      color: #28a745;
    }
    
    .original-price {
      margin-left: 0.5rem;
      text-decoration: line-through;
      color: #6c757d;
    }
    
    .product-rating {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      margin-top: 0.5rem;
    }
    
    .stars {
      display: flex;
      color: #ffc107;
    }
    
    .stars mat-icon {
      font-size: 1rem;
      width: 1rem;
      height: 1rem;
    }
    
    .review-count {
      font-size: 0.875rem;
      color: #6c757d;
    }
    
    .product-actions {
      padding: 1rem;
      display: flex;
      gap: 0.5rem;
    }
    
    .product-actions button {
      flex: 1;
    }
    
    .no-results {
      text-align: center;
      padding: 3rem;
      color: #6c757d;
    }
    
    .no-results mat-icon {
      font-size: 4rem;
      width: 4rem;
      height: 4rem;
      margin-bottom: 1rem;
    }
    
    .pagination-container {
      display: flex;
      justify-content: center;
      margin-top: 2rem;
    }
    
    @media (max-width: 768px) {
      .filter-sidenav {
        position: fixed;
        z-index: 1000;
      }
      
      .product-grid {
        grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
        gap: 1rem;
      }
    }
  `]
})
export class ProductsComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  
  // Data
  products: Product[] = [];
  categories: ProductCategory[] = [];
  
  // Pagination
  currentPage = 0;
  pageSize = 20;
  totalProducts = 0;
  totalPages = 0;
  pageSizeOptions = [10, 20, 50, 100];
  
  // Filtering & Search
  searchQuery = '';
  selectedCategoryId: number | null = null;
  sortBy = 'createdAt';
  sortDir = 'desc';
  
  // State
  isLoading = false;
  error: string | null = null;
  totalCategories = 0;
  
  // Math reference for template
  Math = Math;

  constructor(
    public productService: ProductService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadCategories();
    this.loadProducts();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    // Clear search timeout on destroy
    if (this.searchTimeout) {
      clearTimeout(this.searchTimeout);
    }
  }

  private loadCategories(): void {
    this.productService.getCategories()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (categories) => {
          this.categories = categories;
          this.totalCategories = categories.length;
        },
        error: (error) => console.error('Failed to load categories:', error)
      });
  }

  private loadProducts(): void {
    // Prevent multiple simultaneous requests
    if (this.isLoading) {
      return;
    }
    
    this.isLoading = true;
    this.error = null;
    
    let request: Observable<ProductSearchResult>;
    
    if (this.searchQuery && this.searchQuery.trim().length > 0) {
      request = this.productService.searchProducts(
        this.searchQuery.trim(), this.currentPage, this.pageSize, this.sortBy, this.sortDir
      );
    } else if (this.selectedCategoryId) {
      request = this.productService.getProductsByCategory(
        this.selectedCategoryId, this.currentPage, this.pageSize, this.sortBy, this.sortDir
      );
    } else {
      request = this.productService.getProducts(
        this.currentPage, this.pageSize, this.sortBy, this.sortDir
      );
    }
    
    request.pipe(
      takeUntil(this.destroy$),
      catchError(error => {
        console.error('Product loading error:', error);
        this.error = error.message || 'Failed to load products';
        this.isLoading = false;
        return of({ 
          content: [], 
          totalElements: 0, 
          totalPages: 0, 
          number: 0,
          size: this.pageSize,
          first: true,
          last: true,
          empty: true,
          numberOfElements: 0,
          pageable: { pageNumber: 0, pageSize: this.pageSize, sort: { empty: true, sorted: false, unsorted: true }, offset: 0, paged: true, unpaged: false },
          sort: { empty: true, sorted: false, unsorted: true }
        } as ProductSearchResult);
      })
    ).subscribe({
      next: (result) => {
        this.products = result.content || [];
        this.totalProducts = result.totalElements || 0;
        this.totalPages = result.totalPages || 0;
        this.currentPage = result.number || 0;
        this.isLoading = false;
      }
    });
  }

  // Event Handlers
  onSearchChange(): void {
    // Debounce search to prevent continuous API calls
    clearTimeout(this.searchTimeout);
    this.searchTimeout = setTimeout(() => {
      this.currentPage = 0;
      this.loadProducts();
    }, 1000); // Increased to 1 second
  }
  
  onSearchSubmit(): void {
    // Immediate search on Enter key
    if (this.searchTimeout) {
      clearTimeout(this.searchTimeout);
    }
    this.currentPage = 0;
    this.loadProducts();
  }
  
  private searchTimeout: any;

  onCategoryChange(): void {
    this.currentPage = 0;
    this.loadProducts();
  }

  onSortChange(): void {
    this.currentPage = 0;
    this.loadProducts();
  }

  onPageChange(event: PageEvent): void {
    this.currentPage = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadProducts();
  }

  // Filter Management
  hasActiveFilters(): boolean {
    return !!(this.searchQuery || this.selectedCategoryId);
  }

  clearSearch(): void {
    this.searchQuery = '';
    this.onSearchChange();
  }

  clearCategory(): void {
    this.selectedCategoryId = null;
    this.onCategoryChange();
  }

  clearAllFilters(): void {
    this.searchQuery = '';
    this.selectedCategoryId = null;
    this.sortBy = 'createdAt';
    this.sortDir = 'desc';
    this.currentPage = 0;
    this.loadProducts();
  }

  getCategoryName(categoryId: number): string {
    const category = this.categories.find(c => c.id === categoryId);
    return category ? category.name : 'Unknown';
  }

  // Actions
  retry(): void {
    this.loadProducts();
  }

  viewProduct(product: Product): void {
    this.router.navigate(['/products', product.id]);
  }

  addToCart(product: Product): void {
    // Implement add to cart functionality
    console.log('Add to cart:', product);
    // You would typically call a cart service here
  }

  onImageError(event: any): void {
    event.target.src = '/assets/images/product-placeholder.svg';
  }

  getStars(rating: number): number[] {
    return Array(5).fill(0).map((_, i) => i + 1);
  }
}