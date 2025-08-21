import { Component, OnInit } from '@angular/core';
import { AuthService } from './core/services/auth.service';
import { CartService } from './core/services/cart.service';
import { LoadingService } from './core/services/loading.service';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit {
  title = 'E-Commerce Platform';
  isLoading$: Observable<boolean>;
  isLoggedIn = false;

  constructor(
    private authService: AuthService,
    private cartService: CartService,
    private loadingService: LoadingService
  ) {
    this.isLoading$ = this.loadingService.loading$;
  }

  ngOnInit(): void {
    // Simple initialization without async complexity
    this.isLoggedIn = this.authService.isLoggedIn;
    
    // Initialize cart when app starts
    this.cartService.getCart().subscribe();
    
    // Merge local cart with server cart if user is logged in
    if (this.isLoggedIn) {
      this.cartService.mergeLocalCartWithServer().subscribe();
    }
  }
}