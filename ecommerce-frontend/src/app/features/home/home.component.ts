import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, Subject, takeUntil, combineLatest } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';
import { Product, ProductCategory, ProductSearchResult } from '@shared/models/product.model';
import { ProductService } from '@core/services/product.service';
import { CartService } from '@core/services/cart.service';
import { InventoryService, InventoryDto } from '@core/services/inventory.service';
import { AddToCartRequest } from '@shared/models/cart.model';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss']
})
export class HomeComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  categories$: Observable<ProductCategory[]>;
  products: Product[] = [];
  isLoading = true;

  constructor(
    private productService: ProductService,
    private cartService: CartService,
    private inventoryService: InventoryService,
    private router: Router
  ) {
    this.categories$ = this.productService.categories$;
  }

  ngOnInit(): void {
    this.loadFeaturedProducts();
  }

  private loadFeaturedProducts(): void {
    this.isLoading = true;
    
    // First get featured products, then get inventory for those products
    this.productService.getFeaturedProducts(0, 16).pipe(
      map((result: ProductSearchResult) => result.content || []),
      switchMap((products: Product[]) => {
        if (products.length === 0) {
          return [products]; // Return empty array if no products
        }
        
        const productIds = products.map(p => p.id);
        return combineLatest([
          [products], // Wrap products in array so combineLatest works
          this.inventoryService.getInventoryForProducts(productIds)
        ]);
      }),
      takeUntil(this.destroy$)
    ).subscribe({
      next: (data) => {
        if (Array.isArray(data) && data.length === 2) {
          const [products, inventories] = data as [Product[], InventoryDto[]];
          this.enrichProductsWithInventory(products, inventories);
          // Filter to only show products with stock and limit to 8
          this.products = products.filter((p: Product) => (p as any).inStock).slice(0, 8);
        } else {
          this.products = data as Product[]; // Handle empty case
        }
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Failed to load featured products:', error);
        this.products = [];
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
        // Add inventory properties to product for UI display
        (product as any).inStock = stockStatus.inStock;
        (product as any).lowStock = stockStatus.isLowStock;
        (product as any).availableQuantity = inventory.availableQuantity;
        (product as any).stockStatus = stockStatus.stockStatus;
      } else {
        // Default to out of stock if no inventory data
        (product as any).inStock = false;
        (product as any).stockStatus = 'OUT_OF_STOCK';
        (product as any).availableQuantity = 0;
      }
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onCategoryClick(category: ProductCategory): void {
    this.router.navigate(['/category', category.id]);
  }

  onProductClick(product: Product): void {
    this.router.navigate(['/products', product.id]);
  }

  onShopNowClick(): void {
    this.router.navigate(['/products']);
  }

  onAddToCart(product: Product, event: Event): void {
    event.stopPropagation(); // Prevent card click navigation
    
    if (!this.canAddToCart(product)) {
      return;
    }
    
    const request: AddToCartRequest = {
      productId: product.id,
      quantity: 1
    };

    this.cartService.addToCart(request).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (cart) => {
        console.log(`Added ${product.name} to cart`);
        // TODO: Show success notification/snackbar
      },
      error: (error) => {
        console.error('Failed to add product to cart:', error);
        // TODO: Show error notification/snackbar
      }
    });
  }

  // Stock status helper methods
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

  isLowStock(product: Product): boolean {
    return !!(product as any).lowStock;
  }

  getAvailableQuantity(product: Product): number {
    return (product as any).availableQuantity || 0;
  }

  formatPrice(price: number): string {
    return this.productService.formatPrice(price);
  }

  getProductImageUrl(product: Product): string {
    return this.productService.getProductImageUrl(product);
  }
}