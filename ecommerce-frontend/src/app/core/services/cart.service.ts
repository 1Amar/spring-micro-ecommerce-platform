import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { tap, map } from 'rxjs/operators';
import { environment } from '@environments/environment';
import { Cart, CartItem, AddToCartRequest, UpdateCartItemRequest, ApplyCouponRequest, Coupon } from '@shared/models/cart.model';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class CartService {
  private readonly apiUrl = environment.apiUrl + '/cart';
  private readonly storageKey = 'ecommerce_cart';

  private cartSubject = new BehaviorSubject<Cart | null>(null);
  public cart$ = this.cartSubject.asObservable();

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {
    this.initializeCart();
  }

  private initializeCart(): void {
    if (this.authService.isLoggedIn) {
      this.loadCartFromServer().subscribe();
    } else {
      this.loadCartFromStorage();
    }
  }

  // Cart operations
  getCart(): Observable<Cart> {
    if (this.authService.isLoggedIn) {
      return this.loadCartFromServer();
    } else {
      const cart = this.getCartFromStorage();
      this.cartSubject.next(cart);
      return new Observable(observer => {
        observer.next(cart);
        observer.complete();
      });
    }
  }

  private loadCartFromServer(): Observable<Cart> {
    return this.http.get<Cart>(`${this.apiUrl}`).pipe(
      tap(cart => {
        this.cartSubject.next(cart);
        this.saveCartToStorage(cart);
      })
    );
  }

  addToCart(request: AddToCartRequest): Observable<Cart> {
    if (this.authService.isLoggedIn) {
      return this.http.post<Cart>(`${this.apiUrl}/items`, request).pipe(
        tap(cart => {
          this.cartSubject.next(cart);
          this.saveCartToStorage(cart);
        })
      );
    } else {
      return this.addToLocalCart(request);
    }
  }

  updateCartItem(request: UpdateCartItemRequest): Observable<Cart> {
    if (this.authService.isLoggedIn) {
      return this.http.put<Cart>(`${this.apiUrl}/items/${request.cartItemId}`, 
        { quantity: request.quantity }).pipe(
        tap(cart => {
          this.cartSubject.next(cart);
          this.saveCartToStorage(cart);
        })
      );
    } else {
      return this.updateLocalCartItem(request);
    }
  }

  removeFromCart(cartItemId: string): Observable<Cart> {
    if (this.authService.isLoggedIn) {
      return this.http.delete<Cart>(`${this.apiUrl}/items/${cartItemId}`).pipe(
        tap(cart => {
          this.cartSubject.next(cart);
          this.saveCartToStorage(cart);
        })
      );
    } else {
      return this.removeFromLocalCart(cartItemId);
    }
  }

  clearCart(): Observable<Cart> {
    if (this.authService.isLoggedIn) {
      return this.http.delete<Cart>(`${this.apiUrl}`).pipe(
        tap(cart => {
          this.cartSubject.next(cart);
          this.saveCartToStorage(cart);
        })
      );
    } else {
      const emptyCart = this.createEmptyCart();
      this.cartSubject.next(emptyCart);
      this.saveCartToStorage(emptyCart);
      return new Observable(observer => {
        observer.next(emptyCart);
        observer.complete();
      });
    }
  }

  applyCoupon(request: ApplyCouponRequest): Observable<Cart> {
    if (this.authService.isLoggedIn) {
      return this.http.post<Cart>(`${this.apiUrl}/coupon`, request).pipe(
        tap(cart => {
          this.cartSubject.next(cart);
          this.saveCartToStorage(cart);
        })
      );
    } else {
      // For local cart, we'd need to validate coupon separately
      throw new Error('Coupon application requires user to be logged in');
    }
  }

  removeCoupon(): Observable<Cart> {
    if (this.authService.isLoggedIn) {
      return this.http.delete<Cart>(`${this.apiUrl}/coupon`).pipe(
        tap(cart => {
          this.cartSubject.next(cart);
          this.saveCartToStorage(cart);
        })
      );
    } else {
      throw new Error('Coupon removal requires user to be logged in');
    }
  }

  // Local cart operations (for non-authenticated users)
  private addToLocalCart(request: AddToCartRequest): Observable<Cart> {
    return new Observable(observer => {
      // This would require product service call to get product details
      // For now, return empty implementation
      const cart = this.getCartFromStorage();
      observer.next(cart);
      observer.complete();
    });
  }

  private updateLocalCartItem(request: UpdateCartItemRequest): Observable<Cart> {
    return new Observable(observer => {
      const cart = this.getCartFromStorage();
      const item = cart.items.find(i => i.id === request.cartItemId);
      if (item) {
        item.quantity = request.quantity;
        this.recalculateCart(cart);
        this.saveCartToStorage(cart);
        this.cartSubject.next(cart);
      }
      observer.next(cart);
      observer.complete();
    });
  }

  private removeFromLocalCart(cartItemId: string): Observable<Cart> {
    return new Observable(observer => {
      const cart = this.getCartFromStorage();
      cart.items = cart.items.filter(i => i.id !== cartItemId);
      this.recalculateCart(cart);
      this.saveCartToStorage(cart);
      this.cartSubject.next(cart);
      observer.next(cart);
      observer.complete();
    });
  }

  // Storage operations
  private getCartFromStorage(): Cart {
    try {
      const stored = localStorage.getItem(this.storageKey);
      if (stored) {
        const cart = JSON.parse(stored);
        return cart;
      }
    } catch (error) {
      console.error('Error loading cart from storage:', error);
    }
    return this.createEmptyCart();
  }

  private saveCartToStorage(cart: Cart): void {
    try {
      localStorage.setItem(this.storageKey, JSON.stringify(cart));
    } catch (error) {
      console.error('Error saving cart to storage:', error);
    }
  }

  private loadCartFromStorage(): void {
    const cart = this.getCartFromStorage();
    this.cartSubject.next(cart);
  }

  // Utility methods
  private createEmptyCart(): Cart {
    return {
      id: 'local-cart',
      items: [],
      itemCount: 0,
      subtotal: 0,
      tax: 0,
      shipping: 0,
      discount: 0,
      total: 0,
      currency: 'USD',
      updatedAt: new Date()
    };
  }

  private recalculateCart(cart: Cart): void {
    cart.itemCount = cart.items.reduce((sum, item) => sum + item.quantity, 0);
    cart.subtotal = cart.items.reduce((sum, item) => sum + (item.price * item.quantity), 0);
    cart.tax = cart.subtotal * 0.1; // 10% tax rate
    cart.shipping = cart.subtotal > 50 ? 0 : 9.99; // Free shipping over $50
    cart.total = cart.subtotal + cart.tax + cart.shipping - cart.discount;
    cart.updatedAt = new Date();
  }

  // Getters
  getCurrentCart(): Cart | null {
    return this.cartSubject.value;
  }

  getItemCount(): Observable<number> {
    return this.cart$.pipe(
      map(cart => cart?.itemCount || 0)
    );
  }

  getCartTotal(): Observable<number> {
    return this.cart$.pipe(
      map(cart => cart?.total || 0)
    );
  }

  isItemInCart(productId: string): Observable<boolean> {
    return this.cart$.pipe(
      map(cart => cart?.items.some(item => item.productId === productId) || false)
    );
  }

  getCartItemQuantity(productId: string): Observable<number> {
    return this.cart$.pipe(
      map(cart => {
        const item = cart?.items.find(item => item.productId === productId);
        return item?.quantity || 0;
      })
    );
  }

  // Merge cart when user logs in
  mergeLocalCartWithServer(): Observable<Cart> {
    const localCart = this.getCartFromStorage();
    if (localCart.items.length === 0) {
      return this.loadCartFromServer();
    }

    return this.http.post<Cart>(`${this.apiUrl}/merge`, { items: localCart.items }).pipe(
      tap(cart => {
        this.cartSubject.next(cart);
        this.saveCartToStorage(cart);
      })
    );
  }
}