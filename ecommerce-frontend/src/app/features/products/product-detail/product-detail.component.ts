import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-product-detail',
  template: `
    <div class="container-fluid py-4">
      <h1>Product Detail</h1>
      <p>Product detail page coming soon...</p>
      <div class="alert alert-info">
        <mat-icon>info</mat-icon>
        This feature is under development. Full product detail view with images, reviews, and purchase options will be available soon.
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
export class ProductDetailComponent implements OnInit {

  constructor(private route: ActivatedRoute) {}

  ngOnInit(): void {}
}