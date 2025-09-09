import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { KeycloakService } from 'keycloak-angular';
import { AuthService, UserProfile } from '@core/services/auth.service';
import { CartService } from '@core/services/cart.service';
import { ProductService } from '@core/services/product.service';
import { ProductCategory } from '@shared/models/product.model';
import { SearchSuggestion } from '@shared/models/search.model';

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
  isMenuOpen = false;

  constructor(
    private authService: AuthService,
    private cartService: CartService,
    private productService: ProductService,
    private router: Router,
    private keycloakService: KeycloakService
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

  onSearch(query: string): void {
    if (query.trim()) {
      this.router.navigate(['/search'], { 
        queryParams: { q: query.trim() } 
      });
    }
  }

  onSuggestionSelected(suggestion: SearchSuggestion): void {
    // Handle different suggestion types
    switch (suggestion.type) {
      case 'BRAND':
        // Could navigate to brand-specific search or page
        this.router.navigate(['/search'], { 
          queryParams: { q: suggestion.text, type: 'brand' } 
        });
        break;
      case 'CATEGORY':
        // Could navigate to category page if we can determine category ID
        this.router.navigate(['/search'], { 
          queryParams: { q: suggestion.text, type: 'category' } 
        });
        break;
      case 'PRODUCT':
      default:
        // Regular search
        this.router.navigate(['/search'], { 
          queryParams: { q: suggestion.text } 
        });
        break;
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

  get isAdmin(): boolean {
    return this.keycloakService.isUserInRole('admin') || 
           this.keycloakService.isUserInRole('ADMIN') ||
           this.keycloakService.isUserInRole('administrator');
  }
}