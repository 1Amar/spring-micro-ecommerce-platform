// Search-specific models matching backend search service DTOs

export interface ProductSearchDto {
  productId: number;
  name: string;
  description?: string;
  brand?: string;
  categoryName?: string;
  price: number;
  imageUrl?: string;
  sku?: string;
  createdAt?: string;
  updatedAt?: string;
  // Stock information
  availableQuantity?: number;
  reservedQuantity?: number;
  inStock?: boolean;
  isLowStock?: boolean;
  reorderLevel?: number;
  stockStatus?: 'IN_STOCK' | 'LOW_STOCK' | 'OUT_OF_STOCK';
}

export interface SearchResponseDto {
  products: ProductSearchDto[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  size: number;
  query: string;
  searchDuration: number;
  hasError: boolean;
  errorMessage?: string;
}

export interface SearchFilters {
  query?: string;
  category?: string;
  brand?: string;
  minPrice?: number;
  maxPrice?: number;
  sortBy?: string;
  sortDir?: string;
}

export interface SearchStats {
  service: string;
  totalProductsIndexed: number;
  elasticsearchStatus: string;
  timestamp: number;
}

export interface SearchSuggestion {
  text: string;
  type: 'PRODUCT' | 'BRAND' | 'CATEGORY';
  count?: number;
  imageUrl?: string;
  highlightedText?: string;
}