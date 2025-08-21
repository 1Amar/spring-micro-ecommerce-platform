import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Observable } from 'rxjs';
import { Product, ProductCategory } from '@shared/models/product.model';
import { ProductService } from '@core/services/product.service';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss']
})
export class HomeComponent implements OnInit {
  featuredProducts$: Observable<Product[]>;
  categories$: Observable<ProductCategory[]>;

  constructor(
    private productService: ProductService,
    private router: Router
  ) {
    this.featuredProducts$ = this.productService.getFeaturedProducts(8);
    this.categories$ = this.productService.categories$;
  }

  ngOnInit(): void {}

  onCategoryClick(category: ProductCategory): void {
    this.router.navigate(['/category', category.id]);
  }

  onProductClick(product: Product): void {
    this.router.navigate(['/products', product.id]);
  }

  onShopNowClick(): void {
    this.router.navigate(['/products']);
  }

  formatPrice(price: number): string {
    return this.productService.formatPrice(price);
  }

  getProductImageUrl(product: Product): string {
    return this.productService.getProductImageUrl(product);
  }
}