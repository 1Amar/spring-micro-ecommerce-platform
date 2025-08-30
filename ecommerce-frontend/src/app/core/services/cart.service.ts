import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, throwError } from 'rxjs';
import { tap, map, catchError } from 'rxjs/operators';
import { environment } from '@environments/environment';
import { Cart, CartItem, AddToCartRequest, UpdateCartItemRequest } from '@shared/models/cart.model';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class CartService {
  private readonly apiUrl = environment.apiUrl + environment.services.cartService;

  private cartSubject = new BehaviorSubject<Cart | null>(null);
  public cart$ = this.cartSubject.asObservable();

  private isUpdatingSubject = new BehaviorSubject<boolean>(false);
  public isUpdating$ = this.isUpdatingSubject.asObservable();

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {
    this.initializeCart();
  }

  private initializeCart(): void {
    this.loadCart().subscribe({
      next: (cart) => console.log('Cart initialized:', cart),
      error: (error) => console.error('Failed to initialize cart:', error)
    });
  }

  // Main cart operations - works for both authenticated and anonymous users
  getCart(): Observable<Cart> {
    return this.loadCart();
  }

  private loadCart(): Observable<Cart> {
    return this.http.get<Cart>(this.apiUrl).pipe(
      tap(cart => {
        this.cartSubject.next(cart);
      }),
      catchError(error => {
        console.error('Failed to load cart:', error);
        return throwError(() => error);
      })
    );
  }

  addToCart(request: AddToCartRequest): Observable<Cart> {
    this.isUpdatingSubject.next(true);
    return this.http.post<Cart>(`${this.apiUrl}/items`, request).pipe(
      tap(cart => {
        this.cartSubject.next(cart);
        this.isUpdatingSubject.next(false);
      }),
      catchError(error => {
        console.error('Failed to add item to cart:', error);
        this.isUpdatingSubject.next(false);
        return throwError(() => error);
      })
    );
  }

  updateCartItem(request: UpdateCartItemRequest): Observable<Cart> {
    this.isUpdatingSubject.next(true);
    return this.http.put<Cart>(`${this.apiUrl}/items`, request).pipe(
      tap(cart => {
        this.cartSubject.next(cart);
        this.isUpdatingSubject.next(false);
      }),
      catchError(error => {
        console.error('Failed to update cart item:', error);
        this.isUpdatingSubject.next(false);
        return throwError(() => error);
      })
    );
  }

  removeFromCart(productId: number): Observable<Cart> {
    this.isUpdatingSubject.next(true);
    return this.http.delete<Cart>(`${this.apiUrl}/items/${productId}`).pipe(
      tap(cart => {
        this.cartSubject.next(cart);
        this.isUpdatingSubject.next(false);
      }),
      catchError(error => {
        console.error('Failed to remove item from cart:', error);
        this.isUpdatingSubject.next(false);
        return throwError(() => error);
      })
    );
  }

  clearCart(): Observable<Cart> {
    this.isUpdatingSubject.next(true);
    return this.http.delete<Cart>(this.apiUrl).pipe(
      tap(cart => {
        this.cartSubject.next(cart);
        this.isUpdatingSubject.next(false);
      }),
      catchError(error => {
        console.error('Failed to clear cart:', error);
        this.isUpdatingSubject.next(false);
        return throwError(() => error);
      })
    );
  }

  // Get cart item count
  getCartCount(): Observable<number> {
    return this.http.get<{count: number}>(`${this.apiUrl}/count`).pipe(
      map(response => response.count),
      catchError(error => {
        console.error('Failed to get cart count:', error);
        return throwError(() => error);
      })
    );
  }

  // Utility getters
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
      map(cart => cart?.totalAmount || 0)
    );
  }

  isItemInCart(productId: number): Observable<boolean> {
    return this.cart$.pipe(
      map(cart => cart?.items.some(item => item.productId === productId) || false)
    );
  }

  getCartItemQuantity(productId: number): Observable<number> {
    return this.cart$.pipe(
      map(cart => {
        const item = cart?.items.find(item => item.productId === productId);
        return item?.quantity || 0;
      })
    );
  }
}