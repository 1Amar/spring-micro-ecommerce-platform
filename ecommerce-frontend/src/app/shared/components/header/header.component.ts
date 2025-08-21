import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { AuthService, UserProfile } from '@core/services/auth.service';
import { CartService } from '@core/services/cart.service';
import { ProductService } from '@core/services/product.service';
import { ProductCategory } from '@shared/models/product.model';

@Component({
  selector: 'app-header',
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.scss']
})
export class HeaderComponent implements OnInit {
  userProfile$: Observable<UserProfile | null>;
  cartItemCount$: Observable<number>;
  categories$: Observable<ProductCategory[]>;
  isLoggedIn$: Observable<boolean>;
  searchQuery = '';
  isMenuOpen = false;

  constructor(
    private authService: AuthService,
    private cartService: CartService,
    private productService: ProductService,
    private router: Router
  ) {
    this.userProfile$ = this.authService.userProfile$;
    this.cartItemCount$ = this.cartService.getItemCount();
    this.categories$ = this.productService.categories$;
    this.isLoggedIn$ = this.userProfile$.pipe(
      map(user => user !== null)
    );
  }

  ngOnInit(): void {
    // Refresh auth state when component initializes
    // This helps detect state after Keycloak redirects
    setTimeout(() => {
      this.authService.refreshAuthState();
    }, 100);
  }

  onSearch(): void {
    if (this.searchQuery.trim()) {
      this.router.navigate(['/search'], { 
        queryParams: { q: this.searchQuery.trim() } 
      });
      this.searchQuery = '';
    }
  }

  onLogin(): void {
    this.authService.login();
  }

  onLogout(): void {
    this.authService.logout();
  }

  toggleMenu(): void {
    this.isMenuOpen = !this.isMenuOpen;
  }

  navigateToCategory(category: ProductCategory): void {
    this.router.navigate(['/category', category.id]);
  }


  get username(): string {
    return this.authService.username;
  }
}