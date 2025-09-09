import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject, takeUntil, combineLatest, switchMap, catchError, of } from 'rxjs';
import { PageEvent } from '@angular/material/paginator';
import { Product, ProductCategory, ProductSearchResult } from '@shared/models/product.model';
import { ProductService } from '@core/services/product.service';
import { CartService } from '@core/services/cart.service';
import { InventoryService, InventoryDto } from '@core/services/inventory.service';
import { AddToCartRequest } from '@shared/models/cart.model';

@Component({
  selector: 'app-category',
  templateUrl: './category.component.html',
  styleUrls: ['./category.component.scss']
})
export class CategoryComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  
  // Data
  products: Product[] = [];
  category: ProductCategory | null = null;
  categoryId: number = 0;
  
  // Pagination
  currentPage = 0;
  pageSize = 20;
  totalProducts = 0;
  totalPages = 0;
  pageSizeOptions = [12, 20, 36, 60];
  
  // Sorting
  sortBy = 'name';
  sortDir = 'asc';
  sortOptions = [
    { value: 'name', label: 'Name' },
    { value: 'price', label: 'Price' },
    { value: 'createdAt', label: 'Newest' },
    { value: 'stars', label: 'Rating' }
  ];
  
  // State
  isLoading = false;
  error: string | null = null;
  
  // Math reference for template
  Math = Math;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    public productService: ProductService,
    private cartService: CartService,
    private inventoryService: InventoryService
  ) {}

  ngOnInit(): void {
    this.route.params.pipe(takeUntil(this.destroy$)).subscribe(params => {
      const id = parseInt(params['id']);
      if (id) {
        this.categoryId = id;
        this.loadCategory();
        this.loadProducts();
      }
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadCategory(): void {
    this.productService.getCategory(this.categoryId).pipe(
      takeUntil(this.destroy$),
      catchError(error => {
        console.error('Failed to load category:', error);
        return of(null);
      })
    ).subscribe(category => {
      this.category = category;
    });
  }

  private loadProducts(): void {
    if (this.isLoading) return;
    
    this.isLoading = true;
    this.error = null;
    
    this.productService.getProductsByCategory(this.categoryId, this.currentPage, this.pageSize, this.sortBy, this.sortDir).pipe(
      switchMap((result: ProductSearchResult) => {
        const products = result.content || [];
        this.totalProducts = result.totalElements || 0;
        this.totalPages = result.totalPages || 0;
        this.currentPage = result.number || 0;
        
        if (products.length === 0) {
          return [products];
        }
        
        const productIds = products.map(p => p.id);
        return combineLatest([
          [products],
          this.inventoryService.getInventoryForProducts(productIds)
        ]);
      }),
      takeUntil(this.destroy$),
      catchError(error => {
        console.error('Failed to load products:', error);
        this.error = error.message || 'Failed to load products';
        this.isLoading = false;
        return of([]);
      })
    ).subscribe({
      next: (data) => {
        if (Array.isArray(data) && data.length === 2) {
          const [products, inventories] = data as [Product[], InventoryDto[]];
          this.enrichProductsWithInventory(products, inventories);
          this.products = products;
        } else {
          this.products = data as Product[];
        }
        this.isLoading = false;
      }
    });
  }

  private enrichProductsWithInventory(products: Product[], inventories: InventoryDto[]): void {
    const inventoryMap = new Map<number, InventoryDto>();
    inventories.forEach(inv => inventoryMap.set(inv.productId, inv));

    products.forEach(product => {
      const inventory = inventoryMap.get(product.id);
      if (inventory) {
        const stockStatus = this.inventoryService.getStockStatus(inventory);
        (product as any).inStock = stockStatus.inStock;
        (product as any).lowStock = stockStatus.isLowStock;
        (product as any).availableQuantity = inventory.availableQuantity;
        (product as any).stockStatus = stockStatus.stockStatus;
      } else {
        (product as any).inStock = false;
        (product as any).stockStatus = 'OUT_OF_STOCK';
        (product as any).availableQuantity = 0;
      }
    });
  }

  // Event Handlers
  onSortChange(sortBy: string): void {
    if (this.sortBy === sortBy) {
      this.sortDir = this.sortDir === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortBy = sortBy;
      this.sortDir = 'asc';
    }
    this.currentPage = 0;
    this.loadProducts();
  }

  onPageChange(event: PageEvent): void {
    this.currentPage = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadProducts();
  }

  retry(): void {
    this.loadProducts();
  }

  viewProduct(product: Product): void {
    this.router.navigate(['/products', product.id]);
  }

  addToCart(product: Product): void {
    if (!this.canAddToCart(product)) return;

    const request: AddToCartRequest = {
      productId: product.id,
      quantity: 1
    };

    this.cartService.addToCart(request).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: () => {
        console.log(`Added ${product.name} to cart`);
      },
      error: (error) => {
        console.error('Failed to add product to cart:', error);
      }
    });
  }

  // Helper methods
  canAddToCart(product: Product): boolean {
    return (product as any).inStock === true && (product as any).availableQuantity > 0;
  }

  getAddToCartButtonText(product: Product): string {
    if (!(product as any).inStock) return 'Out of Stock';
    if ((product as any).lowStock) return 'Add to Cart (Limited)';
    return 'Add to Cart';
  }

  getStockStatusText(product: Product): string {
    const stockStatus = (product as any).stockStatus;
    if (!stockStatus) return 'Unknown';
    return this.inventoryService.getStockStatusText(stockStatus, (product as any).availableQuantity);
  }

  getStockBadgeClasses(product: Product): string[] {
    const classes = ['stock-badge'];
    const stockStatus = (product as any).stockStatus;
    
    if (stockStatus === 'IN_STOCK') classes.push('bg-success');
    else if (stockStatus === 'LOW_STOCK') classes.push('bg-warning');
    else if (stockStatus === 'OUT_OF_STOCK') classes.push('bg-danger');
    else classes.push('bg-secondary');
    
    return classes;
  }

  isLowStock(product: Product): boolean {
    return !!(product as any).lowStock;
  }

  getAvailableQuantity(product: Product): number {
    return (product as any).availableQuantity || 0;
  }

  onImageError(event: any): void {
    event.target.src = '/assets/images/product-placeholder.svg';
  }

  getStars(rating: number): number[] {
    return Array(5).fill(0).map((_, i) => i + 1);
  }
}