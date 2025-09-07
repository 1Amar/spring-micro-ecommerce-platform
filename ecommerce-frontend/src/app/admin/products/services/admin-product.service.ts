import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { environment } from '../../../../environments/environment';

export interface ProductDto {
  id?: number;
  name: string;
  description: string;
  sku: string;
  slug: string;
  price: number;
  compareAtPrice?: number;
  cost?: number;
  stockQuantity: number;
  lowStockThreshold: number;
  trackInventory: boolean;
  isActive: boolean;
  isFeatured: boolean;
  weight?: number;
  dimensions?: string;
  color?: string;
  size?: string;
  material?: string;
  brand?: string;
  metaTitle?: string;
  metaDescription?: string;
  tags?: string;
  thumbnailUrl?: string;
  sortOrder?: number;
  imageUrl?: string;
  stars?: number;
  reviewCount?: number;
  boughtInLastMonth?: number;
  isBestSeller?: boolean;
  categoryId: number;
  categoryName?: string;
  inStock?: boolean;
  lowStock?: boolean;
  onSale?: boolean;
  savingsAmount?: number;
  savingsPercentage?: number;
  createdAt?: string;
  updatedAt?: string;
  inventory?: any;
  outOfStockInInventory?: boolean;
  lowStockInInventory?: boolean;
  inventoryStockStatus?: string;
  availableInInventory?: boolean;
  stockMessage?: string;
  availableQuantity?: number;
}

export interface ProductSearchFilters {
  name?: string;
  categoryId?: number;
  brand?: string;
  minPrice?: number;
  maxPrice?: number;
  inStock?: boolean;
  featured?: boolean;
}

export interface ProductPage {
  content: ProductDto[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  numberOfElements: number;
  first: boolean;
  last: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class AdminProductService {
  private baseUrl = `${environment.apiUrl}/products/catalog`;
  private refreshSubject = new BehaviorSubject<boolean>(false);
  
  public refresh$ = this.refreshSubject.asObservable();

  constructor(private http: HttpClient) { }

  // Get all products with pagination
  getAllProducts(page: number = 0, size: number = 20, sortBy: string = 'createdAt', sortDir: string = 'desc'): Observable<ProductPage> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('sortDir', sortDir);

    return this.http.get<ProductPage>(`${this.baseUrl}`, { params });
  }

  // Search products with filters
  searchProducts(filters: ProductSearchFilters, page: number = 0, size: number = 20, sortBy: string = 'name', sortDir: string = 'asc'): Observable<ProductPage> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('sortDir', sortDir);

    // Add filter parameters
    if (filters.name) params = params.set('name', filters.name);
    if (filters.categoryId) params = params.set('categoryId', filters.categoryId.toString());
    if (filters.brand) params = params.set('brand', filters.brand);
    if (filters.minPrice) params = params.set('minPrice', filters.minPrice.toString());
    if (filters.maxPrice) params = params.set('maxPrice', filters.maxPrice.toString());
    if (filters.inStock !== undefined) params = params.set('inStock', filters.inStock.toString());

    return this.http.get<ProductPage>(`${this.baseUrl}/search/advanced`, { params });
  }

  // Get product by ID
  getProduct(id: number): Observable<ProductDto> {
    return this.http.get<ProductDto>(`${this.baseUrl}/${id}`);
  }

  // Get product by SKU
  getProductBySku(sku: string): Observable<ProductDto> {
    return this.http.get<ProductDto>(`${this.baseUrl}/sku/${sku}`);
  }

  // Create new product
  createProduct(product: ProductDto): Observable<ProductDto> {
    return this.http.post<ProductDto>(`${this.baseUrl}`, product).pipe(
      tap(() => this.triggerRefresh())
    );
  }

  // Update existing product
  updateProduct(id: number, product: ProductDto): Observable<ProductDto> {
    return this.http.put<ProductDto>(`${this.baseUrl}/${id}`, product).pipe(
      tap(() => this.triggerRefresh())
    );
  }

  // Delete product (soft delete)
  deleteProduct(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`).pipe(
      tap(() => this.triggerRefresh())
    );
  }

  // Restore product
  restoreProduct(id: number): Observable<void> {
    return this.http.patch<void>(`${this.baseUrl}/${id}/restore`, {}).pipe(
      tap(() => this.triggerRefresh())
    );
  }

  // Toggle featured status
  toggleFeaturedStatus(id: number): Observable<ProductDto> {
    return this.http.patch<ProductDto>(`${this.baseUrl}/${id}/featured`, {}).pipe(
      tap(() => this.triggerRefresh())
    );
  }

  // Update stock quantity
  updateStockQuantity(id: number, quantity: number): Observable<void> {
    let params = new HttpParams().set('quantity', quantity.toString());
    return this.http.patch<void>(`${this.baseUrl}/${id}/stock`, {}, { params }).pipe(
      tap(() => this.triggerRefresh())
    );
  }

  // Adjust stock quantity
  adjustStockQuantity(id: number, adjustment: number): Observable<void> {
    let params = new HttpParams().set('adjustment', adjustment.toString());
    return this.http.patch<void>(`${this.baseUrl}/${id}/stock/adjust`, {}, { params }).pipe(
      tap(() => this.triggerRefresh())
    );
  }

  // Get low stock products
  getLowStockProducts(): Observable<ProductDto[]> {
    return this.http.get<ProductDto[]>(`${this.baseUrl}/inventory/low-stock`);
  }

  // Get out of stock products
  getOutOfStockProducts(): Observable<ProductDto[]> {
    return this.http.get<ProductDto[]>(`${this.baseUrl}/inventory/out-of-stock`);
  }

  // Get products on sale
  getProductsOnSale(page: number = 0, size: number = 20): Observable<ProductPage> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<ProductPage>(`${this.baseUrl}/sale`, { params });
  }

  // Get featured products
  getFeaturedProducts(page: number = 0, size: number = 20): Observable<ProductPage> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<ProductPage>(`${this.baseUrl}/featured`, { params });
  }

  // Get products by category
  getProductsByCategory(categoryId: number, page: number = 0, size: number = 20): Observable<ProductPage> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<ProductPage>(`${this.baseUrl}/category/${categoryId}`, { params });
  }

  // Get catalog statistics
  getCatalogStats(): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/stats`);
  }

  // Get distinct brands
  getDistinctBrands(): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseUrl}/brands`);
  }

  // Get price range
  getPriceRange(): Observable<number[]> {
    return this.http.get<number[]>(`${this.baseUrl}/price-range`);
  }

  // Bulk operations
  bulkUpdateProducts(productIds: number[], updates: Partial<ProductDto>): Observable<any> {
    // This would be implemented if the backend supports bulk operations
    const requests = productIds.map(id => this.updateProduct(id, { ...updates } as ProductDto));
    // For now, we'll do individual updates
    return new Observable(observer => {
      Promise.all(requests.map(req => req.toPromise())).then(results => {
        observer.next(results);
        observer.complete();
        this.triggerRefresh();
      }).catch(error => observer.error(error));
    });
  }

  // Trigger refresh for components listening to changes
  private triggerRefresh(): void {
    this.refreshSubject.next(true);
  }


  // Helper method to generate slug from product name
  generateSlug(name: string): string {
    return name
      .toLowerCase()
      .replace(/[^a-z0-9 -]/g, '') // Remove special characters
      .replace(/\s+/g, '-') // Replace spaces with hyphens
      .replace(/-+/g, '-') // Replace multiple hyphens with single
      .trim();
  }
}