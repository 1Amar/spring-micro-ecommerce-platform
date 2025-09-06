import { Component, OnInit, OnDestroy, ViewChild, ElementRef } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, FormGroup } from '@angular/forms';
import { PageEvent } from '@angular/material/paginator';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import { takeUntil, finalize, catchError } from 'rxjs/operators';
import { of } from 'rxjs';

import { SearchService } from '@core/services/search.service';
import { CartService } from '@core/services/cart.service';
import { AuthService } from '@core/services/auth.service';
import { LoadingService } from '@core/services/loading.service';

import { 
  ProductSearchDto, 
  SearchResponseDto, 
  SearchFilters 
} from '@shared/models/search.model';
import { AddToCartRequest } from '@shared/models/cart.model';

@Component({
  selector: 'app-search',
  templateUrl: './search.component.html',
  styleUrls: ['./search.component.scss']
})
export class SearchComponent implements OnInit, OnDestroy {
  @ViewChild('searchInput', { static: false }) searchInput!: ElementRef;

  private destroy$ = new Subject<void>();
  private searchSubject = new Subject<string>();

  // Search state
  searchResponse: SearchResponseDto | null = null;
  products: ProductSearchDto[] = [];
  isLoading = false;
  hasError = false;
  errorMessage = '';

  // Search parameters
  currentQuery = '';
  currentPage = 0;
  pageSize = 20;
  totalElements = 0;
  totalPages = 0;

  // Filter form
  filterForm: FormGroup;
  showFilters = false;

  // Sorting
  sortOptions = [
    { value: 'name', label: 'Name' },
    { value: 'price', label: 'Price' },
    { value: 'createdAt', label: 'Newest' },
    { value: 'updatedAt', label: 'Recently Updated' }
  ];
  sortBy = 'name';
  sortDirection = 'asc';

  // UI state
  viewMode: 'grid' | 'list' = 'grid';

  // Recent searches
  recentSearches: string[] = [];
  showRecentSearches = false;

  // Categories and brands for filtering (will be populated from search results)
  availableCategories: string[] = [];
  availableBrands: string[] = [];

  // Math reference for template
  Math = Math;

  constructor(
    private searchService: SearchService,
    private cartService: CartService,
    private authService: AuthService,
    private loadingService: LoadingService,
    private route: ActivatedRoute,
    private router: Router,
    private formBuilder: FormBuilder
  ) {
    this.filterForm = this.createFilterForm();
  }

  ngOnInit(): void {
    this.initializeComponent();
    this.setupSearchSubscription();
    this.loadRecentSearches();
    this.handleRouteParams();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get isAuthenticated(): boolean {
    return this.authService.isLoggedIn;
  }

  private initializeComponent(): void {
    // Initialize recent searches
    this.recentSearches = this.searchService.getRecentSearches();
    
    // Subscribe to recent searches updates
    this.searchService.recentSearches$
      .pipe(takeUntil(this.destroy$))
      .subscribe(searches => {
        this.recentSearches = searches;
      });

    // Subscribe to current search query
    this.searchService.searchQuery$
      .pipe(takeUntil(this.destroy$))
      .subscribe(query => {
        if (query && query !== this.currentQuery) {
          this.currentQuery = query;
        }
      });
  }

  private setupSearchSubscription(): void {
    this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      takeUntil(this.destroy$)
    ).subscribe(query => {
      if (query.trim()) {
        this.performSearch(query);
      }
    });
  }

  private handleRouteParams(): void {
    this.route.queryParams
      .pipe(takeUntil(this.destroy$))
      .subscribe(params => {
        const query = params['q'];
        if (query) {
          this.currentQuery = query;
          this.performSearch(query);
        }
      });
  }

  private createFilterForm(): FormGroup {
    return this.formBuilder.group({
      category: [''],
      brand: [''],
      minPrice: [''],
      maxPrice: [''],
      sortBy: [this.sortBy],
      sortDirection: [this.sortDirection]
    });
  }

  private loadRecentSearches(): void {
    this.recentSearches = this.searchService.getRecentSearches();
  }

  onSearch(query: string = ''): void {
    const searchQuery = query || this.currentQuery;
    if (searchQuery && searchQuery.trim()) {
      this.currentQuery = searchQuery.trim();
      this.currentPage = 0; // Reset to first page
      this.searchSubject.next(this.currentQuery);
      
      // Update URL without triggering navigation
      this.router.navigate([], {
        relativeTo: this.route,
        queryParams: { q: this.currentQuery },
        queryParamsHandling: 'merge'
      });
    }
  }

  onRecentSearchClick(query: string): void {
    this.currentQuery = query;
    this.showRecentSearches = false;
    this.onSearch(query);
  }

  clearRecentSearches(): void {
    this.searchService.clearRecentSearches();
    this.recentSearches = [];
  }

  private performSearch(query: string): void {
    this.isLoading = true;
    this.hasError = false;
    this.errorMessage = '';

    const filters = this.getFiltersFromForm();

    this.searchService.searchProducts(
      query,
      this.currentPage,
      this.pageSize,
      filters.sortBy || this.sortBy,
      filters.sortDir || this.sortDirection
    ).pipe(
      takeUntil(this.destroy$),
      finalize(() => {
        this.isLoading = false;
      }),
      catchError(error => {
        console.error('Search failed:', error);
        this.hasError = true;
        this.errorMessage = error.message || 'Search failed. Please try again.';
        return of(null);
      })
    ).subscribe(response => {
      if (response) {
        this.handleSearchResponse(response);
      }
    });
  }

  private handleSearchResponse(response: SearchResponseDto): void {
    this.searchResponse = response;
    this.products = response.products || [];
    this.totalElements = response.totalElements;
    this.totalPages = response.totalPages;
    
    // Extract unique categories and brands for filtering
    this.extractFilterOptions();
    
    // Show error if response indicates error
    if (response.hasError && response.errorMessage) {
      this.hasError = true;
      this.errorMessage = response.errorMessage;
    }
  }

  private extractFilterOptions(): void {
    const categories = new Set<string>();
    const brands = new Set<string>();

    this.products.forEach(product => {
      if (product.categoryName) categories.add(product.categoryName);
      if (product.brand) brands.add(product.brand);
    });

    this.availableCategories = Array.from(categories).sort();
    this.availableBrands = Array.from(brands).sort();
  }

  private getFiltersFromForm(): SearchFilters {
    const formValue = this.filterForm.value;
    return {
      category: formValue.category || undefined,
      brand: formValue.brand || undefined,
      minPrice: formValue.minPrice ? parseFloat(formValue.minPrice) : undefined,
      maxPrice: formValue.maxPrice ? parseFloat(formValue.maxPrice) : undefined,
      sortBy: formValue.sortBy || this.sortBy,
      sortDir: formValue.sortDirection || this.sortDirection
    };
  }

  onFilterChange(): void {
    const filters = this.getFiltersFromForm();
    this.sortBy = filters.sortBy || this.sortBy;
    this.sortDirection = filters.sortDir || this.sortDirection;
    this.currentPage = 0; // Reset to first page when filtering
    
    if (this.currentQuery) {
      this.performSearch(this.currentQuery);
    }
  }

  onPageChange(event: PageEvent): void {
    this.currentPage = event.pageIndex;
    this.pageSize = event.pageSize;
    
    if (this.currentQuery) {
      this.performSearch(this.currentQuery);
    }
  }

  onSortChange(sortBy: string): void {
    if (this.sortBy === sortBy) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortBy = sortBy;
      this.sortDirection = 'asc';
    }
    
    this.filterForm.patchValue({
      sortBy: this.sortBy,
      sortDirection: this.sortDirection
    });
    
    this.onFilterChange();
  }

  toggleViewMode(): void {
    this.viewMode = this.viewMode === 'grid' ? 'list' : 'grid';
  }

  toggleFilters(): void {
    this.showFilters = !this.showFilters;
  }

  clearFilters(): void {
    this.filterForm.reset({
      sortBy: 'name',
      sortDirection: 'asc'
    });
    this.sortBy = 'name';
    this.sortDirection = 'asc';
    this.onFilterChange();
  }

  onAddToCart(product: ProductSearchDto): void {
    if (!product) return;

    const addToCartRequest: AddToCartRequest = {
      productId: product.productId,
      quantity: 1
    };

    this.cartService.addToCart(addToCartRequest).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: () => {
        // Show success message - for now just log
        console.log(`${product.name} added to cart!`);
        // You could add a toast notification service here later
      },
      error: (error: any) => {
        console.error('Failed to add item to cart:', error);
        // You could add error notification here later
      }
    });
  }

  onViewProduct(product: ProductSearchDto): void {
    if (!product) return;
    
    // For now, just navigate to products page
    // Later you can implement a product detail dialog
    console.log('View product:', product);
  }

  onSearchByBrand(brand: string): void {
    this.filterForm.patchValue({ brand });
    this.onFilterChange();
  }

  onSearchByCategory(category: string): void {
    this.filterForm.patchValue({ category });
    this.onFilterChange();
  }

  retrySearch(): void {
    if (this.currentQuery) {
      this.performSearch(this.currentQuery);
    }
  }

  getFormattedPrice(price: number): string {
    return price ? `$${price.toFixed(2)}` : 'N/A';
  }

  getTruncatedDescription(description: string, maxLength: number = 100): string {
    if (!description) return '';
    return description.length > maxLength 
      ? description.substring(0, maxLength) + '...' 
      : description;
  }

  trackByProductId(index: number, product: ProductSearchDto): number {
    return product.productId;
  }

  setDefaultImage(event: any): void {
    (event.target as HTMLImageElement).src = '/assets/images/no-image.png';
  }
}