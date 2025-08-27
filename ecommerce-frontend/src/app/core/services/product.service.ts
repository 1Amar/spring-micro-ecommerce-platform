import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, BehaviorSubject, of } from 'rxjs';
import { map, tap, catchError } from 'rxjs/operators';
import { environment } from '@environments/environment';
import { Product, ProductCategory, ProductReview, ProductSearchFilter, ProductSearchResult } from '@shared/models/product.model';

@Injectable({
  providedIn: 'root'
})
export class ProductService {
  // Use API Gateway routes - properly secured with Keycloak
  private readonly apiUrl = environment.apiUrl + '/products';
  private readonly categoryUrl = environment.apiUrl + '/categories'; 

  private categoriesSubject = new BehaviorSubject<ProductCategory[]>([]);
  public categories$ = this.categoriesSubject.asObservable();

  constructor(private http: HttpClient) {
    this.loadCategories();
  }

  // Product CRUD operations - aligned with backend Spring Data Page
  getProducts(page: number = 0, size: number = 20, sortBy: string = 'createdAt', sortDir: string = 'desc'): Observable<ProductSearchResult> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('sortDir', sortDir);

    return this.http.get<ProductSearchResult>(`${this.apiUrl}/catalog`, { params });
  }

  getProduct(id: number): Observable<Product> {
    return this.http.get<Product>(`${this.apiUrl}/catalog/${id}`);
  }

  getProductBySku(sku: string): Observable<Product> {
    return this.http.get<Product>(`${this.apiUrl}/catalog/sku/${sku}`);
  }

  getProductBySlug(slug: string): Observable<Product> {
    return this.http.get<Product>(`${this.apiUrl}/catalog/slug/${slug}`);
  }

  // Search and filtering - through API Gateway
  searchProducts(query: string, page: number = 0, size: number = 20, sortBy: string = 'name', sortDir: string = 'asc'): Observable<ProductSearchResult> {
    const params = new HttpParams()
      .set('name', query)
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('sortDir', sortDir);

    return this.http.get<ProductSearchResult>(`${this.apiUrl}/catalog/search`, { params });
  }

  // Advanced search with filters
  searchProductsAdvanced(filters: ProductSearchFilter, page: number = 0, size: number = 20): Observable<ProductSearchResult> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (filters.category) params = params.set('name', filters.category);
    if (filters.minPrice !== undefined) params = params.set('minPrice', filters.minPrice.toString());
    if (filters.maxPrice !== undefined) params = params.set('maxPrice', filters.maxPrice.toString());
    if (filters.brand?.length) params = params.set('brand', filters.brand[0]); // Backend expects single brand
    if (filters.inStock !== undefined) params = params.set('inStock', filters.inStock.toString());
    if (filters.sortBy) params = params.set('sortBy', filters.sortBy);
    if (filters.sortOrder) params = params.set('sortDir', filters.sortOrder);

    return this.http.get<ProductSearchResult>(`${this.apiUrl}/catalog/search/advanced`, { params });
  }

  getProductsByCategory(categoryId: number, page: number = 0, size: number = 20, sortBy: string = 'sortOrder', sortDir: string = 'asc'): Observable<ProductSearchResult> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('sortDir', sortDir);

    return this.http.get<ProductSearchResult>(`${this.apiUrl}/catalog/category/${categoryId}`, { params });
  }

  getFeaturedProducts(page: number = 0, size: number = 10, sortBy: string = 'createdAt', sortDir: string = 'desc'): Observable<ProductSearchResult> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('sortDir', sortDir);
    return this.http.get<ProductSearchResult>(`${this.apiUrl}/catalog/featured`, { params });
  }

  getRelatedProducts(productId: number, limit: number = 5): Observable<Product[]> {
    const params = new HttpParams().set('limit', limit.toString());
    return this.http.get<Product[]>(`${this.apiUrl}/catalog/${productId}/related`, { params });
  }

  // Categories - through API Gateway 
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
    // Get categories through API Gateway - properly authenticated
    return this.http.get<any>(`${this.categoryUrl}`)
      .pipe(
        map(response => {
          console.log('Categories response:', response);
          // Handle Spring Data Page response format
          if (response && response.content && Array.isArray(response.content)) {
            return response.content;
          } 
          // Handle direct array response (fallback)
          else if (Array.isArray(response)) {
            return response;
          }
          // Handle error or empty response
          console.warn('Unexpected categories response format:', response);
          return [];
        }),
        catchError(error => {
          console.error('Failed to load categories:', error);
          // Return empty array on error to prevent app crash
          return of([]);
        })
      );
  }

  getCategoriesWithPagination(page: number = 0, size: number = 20, sortBy: string = 'displayOrder', sortDir: string = 'asc'): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('sortDir', sortDir);
    return this.http.get<any>(`${this.categoryUrl}`, { params });
  }

  getCategory(id: number): Observable<ProductCategory> {
    return this.http.get<ProductCategory>(`${this.categoryUrl}/${id}`);
  }

  getCategoryBySlug(slug: string): Observable<ProductCategory> {
    return this.http.get<ProductCategory>(`${this.categoryUrl}/slug/${slug}`);
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

  // Product statistics
  getCatalogStats(): Observable<any> {
    return this.http.get(`${this.apiUrl}/catalog/stats`);
  }

  getDistinctBrands(): Observable<string[]> {
    return this.http.get<string[]>(`${this.apiUrl}/catalog/brands`);
  }

  getPriceRange(): Observable<{min: number, max: number}> {
    return this.http.get<{min: number, max: number}>(`${this.apiUrl}/catalog/price-range`);
  }

  // Utility methods
  getProductImageUrl(product: Product): string {
    return product.imageUrl || '/assets/images/product-placeholder.svg';
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

  // Check if product is on sale
  isOnSale(product: Product): boolean {
    return product.onSale || (product.compareAtPrice && product.compareAtPrice > product.price) || false;
  }

  getSavingsAmount(product: Product): number {
    return product.savingsAmount || (product.compareAtPrice ? product.compareAtPrice - product.price : 0);
  }
}