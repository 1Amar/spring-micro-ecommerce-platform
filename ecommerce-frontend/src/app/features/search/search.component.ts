import { Component } from '@angular/core';

@Component({
  selector: 'app-search',
  template: `
    <div class="container-fluid py-4">
      <h1>Search Products</h1>
      <div class="alert alert-info">
        <mat-icon>info</mat-icon>
        Advanced search functionality coming soon with filters, sorting, and autocomplete.
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
export class SearchComponent { }