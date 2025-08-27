// Spring Data Page DTO interface
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number; // 0-indexed page number
  first: boolean;
  last: boolean;
  empty: boolean;
  numberOfElements: number;
  pageable: {
    sort: {
      empty: boolean;
      sorted: boolean;
      unsorted: boolean;
    };
    offset: number;
    pageSize: number;
    pageNumber: number;
    paged: boolean;
    unpaged: boolean;
  };
  sort: {
    empty: boolean;
    sorted: boolean;
    unsorted: boolean;
  };
}

// Pagination parameters for API calls
export interface PageRequest {
  page: number;
  size: number;
  sortBy?: string;
  sortDir?: 'asc' | 'desc';
}

// Search and filter parameters
export interface ProductFilters {
  categoryId?: string;
  searchQuery?: string;
  minPrice?: number;
  maxPrice?: number;
  inStock?: boolean;
  brand?: string;
}
