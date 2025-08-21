import { Component, OnInit } from '@angular/core';
import { Observable } from 'rxjs';
import { Cart } from '@shared/models/cart.model';
import { CartService } from '@core/services/cart.service';

@Component({
  selector: 'app-cart',
  template: `
    <div class="container-fluid py-4">
      <h1>Shopping Cart</h1>
      <p>Shopping cart functionality coming soon...</p>
      <div class="alert alert-info">
        <mat-icon>info</mat-icon>
        This feature is under development. Full cart management with item updates, quantity changes, and checkout will be available soon.
      </div>
    </div>
  `,
  styles: [`
    .alert {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 16px;
      border-radius: 4px;
      background-color: #E3F2FD;
      border: 1px solid #BBDEFB;
      color: #1976D2;
    }
  `]
})
export class CartComponent implements OnInit {
  cart$: Observable<Cart | null>;

  constructor(private cartService: CartService) {
    this.cart$ = this.cartService.cart$;
  }

  ngOnInit(): void {}
}