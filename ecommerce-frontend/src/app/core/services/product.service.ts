import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { environment } from '@environments/environment';
import { Product, ProductCategory, ProductReview, ProductSearchFilter, ProductSearchResult } from '@shared/models/product.model';

@Injectable({
  providedIn: 'root'
})
export class ProductService {
  private readonly apiUrl = environment.apiUrl + environment.services.productService;
  private readonly catalogUrl = environment.apiUrl + environment.services.catalogService;
  private readonly searchUrl = environment.apiUrl + environment.services.searchService;

  private categoriesSubject = new BehaviorSubject<ProductCategory[]>([]);
  public categories$ = this.categoriesSubject.asObservable();

  constructor(private http: HttpClient) {
    this.loadCategories();
  }

  // Product CRUD operations
  getProducts(page: number = 0, size: number = 20): Observable<ProductSearchResult> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<ProductSearchResult>(`${this.apiUrl}`, { params });
  }

  getProduct(id: string): Observable<Product> {
    return this.http.get<Product>(`${this.apiUrl}/${id}`);
  }

  getProductBysku(sku: string): Observable<Product> {
    return this.http.get<Product>(`${this.apiUrl}/sku/${sku}`);
  }

  // Search and filtering
  searchProducts(query: string, filters?: ProductSearchFilter, page: number = 0, size: number = 20): Observable<ProductSearchResult> {
    let params = new HttpParams()
      .set('query', query)
      .set('page', page.toString())
      .set('size', size.toString());

    if (filters) {
      if (filters.category) params = params.set('category', filters.category);
      if (filters.minPrice !== undefined) params = params.set('minPrice', filters.minPrice.toString());
      if (filters.maxPrice !== undefined) params = params.set('maxPrice', filters.maxPrice.toString());
      if (filters.brand?.length) params = params.set('brands', filters.brand.join(','));
      if (filters.rating !== undefined) params = params.set('minRating', filters.rating.toString());
      if (filters.inStock !== undefined) params = params.set('inStock', filters.inStock.toString());
      if (filters.sortBy) params = params.set('sortBy', filters.sortBy);
      if (filters.sortOrder) params = params.set('sortOrder', filters.sortOrder);
    }

    return this.http.get<ProductSearchResult>(`${this.searchUrl}/products`, { params });
  }

  getProductsByCategory(categoryId: string, page: number = 0, size: number = 20): Observable<ProductSearchResult> {
    const params = new HttpParams()
      .set('categoryId', categoryId)
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<ProductSearchResult>(`${this.catalogUrl}/products`, { params });
  }

  getFeaturedProducts(limit: number = 10): Observable<Product[]> {
    const params = new HttpParams().set('limit', limit.toString());
    return this.http.get<Product[]>(`${this.apiUrl}/featured`, { params });
  }

  getRelatedProducts(productId: string, limit: number = 5): Observable<Product[]> {
    const params = new HttpParams().set('limit', limit.toString());
    return this.http.get<Product[]>(`${this.apiUrl}/${productId}/related`, { params });
  }

  // Categories
  private loadCategories(): void {
    this.getCategories().subscribe({
      next: (categories) => {
        this.categoriesSubject.next(categories);
      },
      error: (error) => {
        console.error('Failed to load categories:', error);
        this.categoriesSubject.next([]); // Provide empty array on error
      }
    });
  }

  getCategories(): Observable<ProductCategory[]> {
    return this.http.get<{categories: ProductCategory[]}>(`${this.catalogUrl}/categories`)
      .pipe(
        map(response => response.categories || [])
      );
  }

  getCategory(id: string): Observable<ProductCategory> {
    return this.http.get<ProductCategory>(`${this.catalogUrl}/categories/${id}`);
  }

  // Reviews
  getProductReviews(productId: string, page: number = 0, size: number = 10): Observable<{ reviews: ProductReview[], totalItems: number }> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<{ reviews: ProductReview[], totalItems: number }>(`${this.apiUrl}/${productId}/reviews`, { params });
  }

  addProductReview(productId: string, review: Partial<ProductReview>): Observable<ProductReview> {
    return this.http.post<ProductReview>(`${this.apiUrl}/${productId}/reviews`, review);
  }

  updateProductReview(productId: string, reviewId: string, review: Partial<ProductReview>): Observable<ProductReview> {
    return this.http.put<ProductReview>(`${this.apiUrl}/${productId}/reviews/${reviewId}`, review);
  }

  deleteProductReview(productId: string, reviewId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${productId}/reviews/${reviewId}`);
  }

  markReviewHelpful(productId: string, reviewId: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/${productId}/reviews/${reviewId}/helpful`, {});
  }

  // Inventory check
  checkInventory(productId: string): Observable<{ available: boolean, quantity: number }> {
    return this.http.get<{ available: boolean, quantity: number }>(`${environment.apiUrl}${environment.services.inventoryService}/${productId}`);
  }

  // Utility methods
  getProductImageUrl(product: Product, index: number = 0): string {
    if (product.images && product.images.length > index) {
      return product.images[index];
    }
    return product.imageUrl || '/assets/images/product-placeholder.png';
  }

  calculateDiscountPercentage(originalPrice: number, currentPrice: number): number {
    return Math.round(((originalPrice - currentPrice) / originalPrice) * 100);
  }

  formatPrice(price: number, currency: string = 'USD'): string {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: currency
    }).format(price);
  }
}