import { Component } from '@angular/core';

@Component({
  selector: 'app-category',
  template: `
    <div class="container-fluid py-4">
      <h1>Category Products</h1>
      <div class="alert alert-info">
        <mat-icon>info</mat-icon>
        Category-based product browsing coming soon with filters and sorting options.
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
export class CategoryComponent { }