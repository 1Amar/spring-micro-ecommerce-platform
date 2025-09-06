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