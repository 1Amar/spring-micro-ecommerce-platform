import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable } from 'rxjs';
import { Product, ProductSearchResult, ProductSearchFilter } from '@shared/models/product.model';
import { ProductService } from '@core/services/product.service';

@Component({
  selector: 'app-products',
  template: `
    <div class="container-fluid py-4">
      <h1>Products</h1>
      <p>Product catalog coming soon...</p>
      <div class="alert alert-info">
        <mat-icon>info</mat-icon>
        This feature is under development. Full product catalog with filtering, sorting, and pagination will be available soon.
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
export class ProductsComponent implements OnInit {

  constructor(
    private productService: ProductService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {}
}