import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, BehaviorSubject, of } from 'rxjs';
import { map, catchError, debounceTime, distinctUntilChanged, switchMap } from 'rxjs/operators';
import { environment } from '@environments/environment';
import { 
  ProductSearchDto, 
  SearchResponseDto, 
  SearchFilters,
  SearchStats,
  SearchSuggestion 
} from '@shared/models/search.model';

@Injectable({
  providedIn: 'root'
})
export class SearchService {
  // Using API Gateway routes for authentication
  private readonly searchApiUrl = environment.apiUrl + '/search';
  
  // For development/testing, can use direct search service
  // private readonly searchApiUrl = 'http://localhost:8086/api/v1/search';

  // Search state management
  private searchQuerySubject = new BehaviorSubject<string>('');
  public searchQuery$ = this.searchQuerySubject.asObservable();

  private recentSearches = new BehaviorSubject<string[]>([]);
  public recentSearches$ = this.recentSearches.asObservable();

  constructor(private http: HttpClient) {
    this.loadRecentSearches();
  }

  /**
   * Main search method - searches products by query
   */
  searchProducts(
    query: string, 
    page: number = 0, 
    size: number = 20,
    sortBy: string = 'name',
    sortDir: string = 'asc'
  ): Observable<SearchResponseDto> {
    const params = new HttpParams()
      .set('query', query)
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('sortDir', sortDir);

    // Update search query state
    this.searchQuerySubject.next(query);
    this.addToRecentSearches(query);

    return this.http.get<SearchResponseDto>(`${this.searchApiUrl}/products`, { params })
      .pipe(
        map(response => this.transformSearchResponse(response)),
        catchError(error => {
          console.error('Search failed:', error);
          throw this.handleSearchError(error, query);
        })
      );
  }

  /**
   * Search products by brand
   */
  searchByBrand(
    brand: string,
    page: number = 0,
    size: number = 20
  ): Observable<SearchResponseDto> {
    const params = new HttpParams()
      .set('brand', brand)
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<SearchResponseDto>(`${this.searchApiUrl}/products/brand`, { params })
      .pipe(
        map(response => this.transformSearchResponse(response)),
        catchError(error => {
          console.error('Brand search failed:', error);
          throw this.handleSearchError(error, `brand:${brand}`);
        })
      );
  }

  /**
   * Search products by category
   */
  searchByCategory(
    category: string,
    page: number = 0,
    size: number = 20
  ): Observable<SearchResponseDto> {
    const params = new HttpParams()
      .set('category', category)
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<SearchResponseDto>(`${this.searchApiUrl}/products/category`, { params })
      .pipe(
        map(response => this.transformSearchResponse(response)),
        catchError(error => {
          console.error('Category search failed:', error);
          throw this.handleSearchError(error, `category:${category}`);
        })
      );
  }

  /**
   * Get search service statistics
   */
  getSearchStats(): Observable<SearchStats> {
    return this.http.get<SearchStats>(`${this.searchApiUrl}/stats`)
      .pipe(
        catchError(error => {
          console.error('Failed to get search stats:', error);
          throw error;
        })
      );
  }

  /**
   * Check search service health
   */
  checkSearchHealth(): Observable<any> {
    return this.http.get(`${this.searchApiUrl}/health`)
      .pipe(
        catchError(error => {
          console.error('Search service health check failed:', error);
          throw error;
        })
      );
  }

  /**
   * Get search suggestions for real-time autocomplete
   */
  getSearchSuggestions(query: string): Observable<SearchSuggestion[]> {
    if (!query || query.trim().length < 2) {
      // Return recent searches for very short queries
      return of(this.getRecentSearches().slice(0, 5).map(search => ({
        text: search,
        type: 'PRODUCT' as const,
        count: 0
      })));
    }

    const params = new HttpParams()
      .set('query', query.trim())
      .set('limit', '8');

    return this.http.get<SearchSuggestion[]>(`${this.searchApiUrl}/suggestions`, { params })
      .pipe(
        map(suggestions => this.transformSuggestions(suggestions, query)),
        catchError(error => {
          console.error('Search suggestions failed:', error);
          return of(this.getFallbackSuggestions(query));
        })
      );
  }

  /**
   * Get search suggestions with debouncing for real-time input
   */
  getSearchSuggestionsDebounced(query$: Observable<string>): Observable<SearchSuggestion[]> {
    return query$.pipe(
      debounceTime(300), // Wait 300ms after user stops typing
      distinctUntilChanged(), // Only emit if query actually changed
      switchMap(query => this.getSearchSuggestions(query))
    );
  }

  /**
   * Recent searches management
   */
  getRecentSearches(): string[] {
    return this.recentSearches.getValue();
  }

  private addToRecentSearches(query: string): void {
    if (!query || query.trim().length < 2) return;

    const current = this.getRecentSearches();
    const trimmedQuery = query.trim().toLowerCase();
    
    // Remove if already exists
    const filtered = current.filter(search => search.toLowerCase() !== trimmedQuery);
    
    // Add to beginning
    const updated = [query.trim(), ...filtered].slice(0, 10); // Keep max 10
    
    this.recentSearches.next(updated);
    this.saveRecentSearches(updated);
  }

  clearRecentSearches(): void {
    this.recentSearches.next([]);
    localStorage.removeItem('recent-searches');
  }

  private loadRecentSearches(): void {
    try {
      const saved = localStorage.getItem('recent-searches');
      if (saved) {
        const searches = JSON.parse(saved);
        if (Array.isArray(searches)) {
          this.recentSearches.next(searches);
        }
      }
    } catch (error) {
      console.error('Failed to load recent searches:', error);
    }
  }

  private saveRecentSearches(searches: string[]): void {
    try {
      localStorage.setItem('recent-searches', JSON.stringify(searches));
    } catch (error) {
      console.error('Failed to save recent searches:', error);
    }
  }

  /**
   * Transform backend response to frontend model
   */
  private transformSearchResponse(response: any): SearchResponseDto {
    // Handle both direct search service response and API gateway response
    return {
      products: response.products || [],
      totalElements: response.totalElements || 0,
      totalPages: response.totalPages || 0,
      currentPage: response.currentPage || 0,
      size: response.size || 20,
      query: response.query || '',
      searchDuration: response.searchDuration || 0,
      hasError: response.hasError || false,
      errorMessage: response.errorMessage
    };
  }

  /**
   * Handle search errors with user-friendly messages
   */
  private handleSearchError(error: any, query: string): Error {
    let message = 'Search failed. Please try again.';
    
    if (error.status === 401) {
      message = 'Authentication required to search products.';
    } else if (error.status === 403) {
      message = 'Not authorized to search products.';
    } else if (error.status === 404) {
      message = 'Search service not available.';
    } else if (error.status === 500) {
      message = 'Search service error. Please try again later.';
    } else if (error.status === 0) {
      message = 'Unable to connect to search service.';
    }

    return new Error(message);
  }

  /**
   * Transform backend suggestion response with highlighting
   */
  private transformSuggestions(suggestions: SearchSuggestion[], originalQuery: string): SearchSuggestion[] {
    return suggestions.map(suggestion => ({
      ...suggestion,
      highlightedText: this.highlightQuery(suggestion.text, originalQuery)
    }));
  }

  /**
   * Highlight matching query text in suggestions
   */
  private highlightQuery(text: string, query: string): string {
    if (!query || query.length < 2) return text;
    
    const regex = new RegExp(`(${query})`, 'gi');
    return text.replace(regex, '<strong>$1</strong>');
  }

  /**
   * Get fallback suggestions when API fails
   */
  private getFallbackSuggestions(query: string): SearchSuggestion[] {
    const recentMatches = this.getRecentSearches()
      .filter(search => search.toLowerCase().includes(query.toLowerCase()))
      .slice(0, 3)
      .map(search => ({
        text: search,
        type: 'PRODUCT' as const,
        count: 0,
        highlightedText: this.highlightQuery(search, query)
      }));

    // Add the current query as a search suggestion
    const currentQuerySuggestion: SearchSuggestion = {
      text: query,
      type: 'PRODUCT',
      count: 0,
      highlightedText: `Search for "${query}"`
    };

    return [currentQuerySuggestion, ...recentMatches];
  }

  /**
   * Convert ProductSearchDto to Product model for compatibility with existing components
   */
  convertToProduct(searchProduct: ProductSearchDto): any {
    return {
      id: searchProduct.productId,
      name: searchProduct.name,
      description: searchProduct.description || '',
      price: searchProduct.price,
      brand: searchProduct.brand || '',
      categoryName: searchProduct.categoryName || '',
      imageUrl: searchProduct.imageUrl,
      sku: searchProduct.sku || '',
      createdAt: searchProduct.createdAt || '',
      updatedAt: searchProduct.updatedAt || '',
      // Default values for missing properties
      stockQuantity: 1,
      inStock: true,
      isActive: true,
      isFeatured: false,
      onSale: false,
      sortOrder: 0,
      trackInventory: false,
      lowStockThreshold: 0,
      categoryId: 0,
      lowStock: false,
      slug: ''
    };
  }
}