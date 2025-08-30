import { Component, OnInit } from '@angular/core';
import { Observable, combineLatest } from 'rxjs';
import { map } from 'rxjs/operators';
import { Cart, CartItem, UpdateCartItemRequest } from '@shared/models/cart.model';
import { CartService } from '@core/services/cart.service';
import { ProductService } from '@core/services/product.service';

@Component({
  selector: 'app-cart',
  template: `
    <div class="container-fluid py-4">
      <div class="row">
        <div class="col-12">
          <div class="d-flex justify-content-between align-items-center mb-4">
            <h1 class="mb-0">
              <mat-icon class="me-2">shopping_cart</mat-icon>
              Shopping Cart
            </h1>
          </div>

          <!-- Cart Content -->
          <div *ngIf="cartData$ | async as data; else loadingTemplate">
            <!-- Empty Cart -->
            <div *ngIf="!data.cart || data.cart.items.length === 0" class="text-center py-5">
              <mat-icon class="empty-cart-icon mb-3">shopping_cart</mat-icon>
              <h3>Your cart is empty</h3>
              <p class="text-muted">Start shopping to add items to your cart</p>
              <button mat-raised-button color="primary" routerLink="/products">
                <mat-icon class="me-2">store</mat-icon>
                Continue Shopping
              </button>
            </div>

            <!-- Cart with Items -->
            <div *ngIf="data.cart && data.cart.items.length > 0">
              <div class="row">
                <!-- Cart Items -->
                <div class="col-lg-8">
                  <div class="card">
                    <div class="card-header">
                      <h5 class="mb-0">Cart Items ({{ data.cart.itemCount }})</h5>
                    </div>
                    <div class="card-body p-0">
                      <div *ngFor="let item of data.cart.items; trackBy: trackByProductId" 
                           class="cart-item border-bottom p-3">
                        <div class="row align-items-center">
                          <!-- Product Image -->
                          <div class="col-md-2">
                            <img [src]="getProductImageUrl(item)" 
                                 [alt]="item.productName"
                                 class="img-fluid rounded"
                                 style="max-height: 80px; object-fit: cover;"
                                 (error)="onImageError($event)">
                          </div>
                          
                          <!-- Product Details -->
                          <div class="col-md-4">
                            <h6 class="mb-1">{{ item.productName }}</h6>
                            <small class="text-muted">Added: {{ item.addedAt | date:'short' }}</small>
                          </div>
                          
                          <!-- Quantity Controls -->
                          <div class="col-md-3">
                            <div class="d-flex align-items-center">
                              <button mat-icon-button 
                                      [disabled]="data.isUpdating"
                                      (click)="updateQuantity(item.productId, item.quantity - 1)">
                                <mat-icon>remove</mat-icon>
                              </button>
                              <span class="mx-3 fw-bold">{{ item.quantity }}</span>
                              <button mat-icon-button 
                                      [disabled]="data.isUpdating"
                                      (click)="updateQuantity(item.productId, item.quantity + 1)">
                                <mat-icon>add</mat-icon>
                              </button>
                            </div>
                          </div>
                          
                          <!-- Price -->
                          <div class="col-md-2">
                            <div class="text-end">
                              <div class="fw-bold">{{ item.totalPrice | currency:'USD':'symbol':'1.2-2' }}</div>
                              <small class="text-muted">{{ item.unitPrice | currency:'USD':'symbol':'1.2-2' }} each</small>
                            </div>
                          </div>
                          
                          <!-- Remove Button -->
                          <div class="col-md-1">
                            <button mat-icon-button 
                                    color="warn"
                                    [disabled]="data.isUpdating"
                                    (click)="removeItem(item.productId)"
                                    title="Remove from cart">
                              <mat-icon>delete</mat-icon>
                            </button>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- Cart Summary -->
                <div class="col-lg-4">
                  <div class="card">
                    <div class="card-header">
                      <h5 class="mb-0">Order Summary</h5>
                    </div>
                    <div class="card-body">
                      <div class="d-flex justify-content-between mb-2">
                        <span>Items ({{ data.cart.itemCount }}):</span>
                        <span>{{ data.cart.totalAmount | currency:'USD':'symbol':'1.2-2' }}</span>
                      </div>
                      <div class="d-flex justify-content-between mb-2">
                        <span>Shipping:</span>
                        <span class="text-success">FREE</span>
                      </div>
                      <hr>
                      <div class="d-flex justify-content-between mb-3">
                        <strong>Total:</strong>
                        <strong>{{ data.cart.totalAmount | currency:'USD':'symbol':'1.2-2' }}</strong>
                      </div>
                      
                      <button mat-raised-button 
                              color="primary" 
                              class="w-100 mb-3"
                              [disabled]="data.isUpdating">
                        <mat-icon class="me-2">payment</mat-icon>
                        Proceed to Checkout
                      </button>
                      
                      <button mat-stroked-button 
                              color="primary" 
                              class="w-100"
                              routerLink="/products">
                        <mat-icon class="me-2">store</mat-icon>
                        Continue Shopping
                      </button>
                    </div>
                  </div>

                  <!-- Clear Cart -->
                  <div class="mt-3">
                    <button mat-button 
                            color="warn" 
                            class="w-100"
                            [disabled]="data.isUpdating"
                            (click)="clearCart()">
                      <mat-icon class="me-2">clear_all</mat-icon>
                      Clear Cart
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Loading Template -->
          <ng-template #loadingTemplate>
            <div class="text-center py-5">
              <mat-spinner diameter="50"></mat-spinner>
              <p class="mt-3 text-muted">Loading your cart...</p>
            </div>
          </ng-template>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .empty-cart-icon {
      font-size: 72px;
      color: #ccc;
      height: 72px;
      width: 72px;
    }
    
    .cart-item:last-child {
      border-bottom: none !important;
    }
    
    .cart-item:hover {
      background-color: #f8f9fa;
    }
    
    .card {
      box-shadow: 0 2px 4px rgba(0,0,0,0.1);
      border: 1px solid #e9ecef;
    }
    
    .card-header {
      background-color: #f8f9fa;
      border-bottom: 1px solid #e9ecef;
    }
    
    img {
      border: 1px solid #e9ecef;
    }
  `]
})
export class CartComponent implements OnInit {
  cartData$: Observable<{cart: Cart | null, isUpdating: boolean}>;

  constructor(
    private cartService: CartService,
    private productService: ProductService
  ) {
    this.cartData$ = combineLatest([
      this.cartService.cart$,
      this.cartService.isUpdating$
    ]).pipe(
      map(([cart, isUpdating]) => ({
        cart,
        isUpdating
      }))
    );
  }

  ngOnInit(): void {
    this.cartService.getCart().subscribe();
  }

  trackByProductId(index: number, item: CartItem): number {
    return item.productId;
  }

  getProductImageUrl(item: CartItem): string {
    // Use the same logic as the products component
    return item.imageUrl || 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjAwIiBoZWlnaHQ9IjIwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSIjZjhmOWZhIiBzdHJva2U9IiNkZWUyZTYiLz48dGV4dCB4PSI1MCUiIHk9IjUwJSIgZm9udC1mYW1pbHk9IkFyaWFsIiBmb250LXNpemU9IjE0IiBmaWxsPSIjNmM3NTdkIiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBkeT0iLjNlbSI+Tm8gSW1hZ2U8L3RleHQ+PC9zdmc+';
  }

  updateQuantity(productId: number, newQuantity: number): void {
    if (newQuantity < 1) {
      this.removeItem(productId);
      return;
    }

    const request: UpdateCartItemRequest = {
      productId,
      quantity: newQuantity
    };

    this.cartService.updateCartItem(request).subscribe({
      next: () => console.log('Item quantity updated'),
      error: (error) => console.error('Failed to update quantity:', error)
    });
  }

  removeItem(productId: number): void {
    this.cartService.removeFromCart(productId).subscribe({
      next: () => console.log('Item removed from cart'),
      error: (error) => console.error('Failed to remove item:', error)
    });
  }

  clearCart(): void {
    if (confirm('Are you sure you want to clear your cart?')) {
      this.cartService.clearCart().subscribe({
        next: () => console.log('Cart cleared'),
        error: (error) => console.error('Failed to clear cart:', error)
      });
    }
  }

  onImageError(event: any): void {
    // Prevent infinite loop of error events
    if (!event.target.dataset.errorHandled) {
      event.target.dataset.errorHandled = 'true';
      // Use a simple base64 encoded placeholder image
      event.target.src = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjAwIiBoZWlnaHQ9IjIwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSIjZjhmOWZhIiBzdHJva2U9IiNkZWUyZTYiLz48dGV4dCB4PSI1MCUiIHk9IjUwJSIgZm9udC1mYW1pbHk9IkFyaWFsIiBmb250LXNpemU9IjE0IiBmaWxsPSIjNmM3NTdkIiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBkeT0iLjNlbSI+Tm8gSW1hZ2U8L3RleHQ+PC9zdmc+';
    }
  }
}