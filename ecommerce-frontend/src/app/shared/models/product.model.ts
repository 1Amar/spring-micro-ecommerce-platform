export interface Product {
  id: string;
  name: string;
  description: string;
  price: number;
  originalPrice?: number;
  discount?: number;
  imageUrl: string;
  images?: string[];
  category: string;
  subcategory?: string;
  brand: string;
  sku: string;
  tags?: string[];
  specifications?: { [key: string]: string };
  rating: number;
  reviewCount: number;
  availability: ProductAvailability;
  createdAt: Date;
  updatedAt: Date;
}

export interface ProductAvailability {
  inStock: boolean;
  quantity: number;
  lowStock?: boolean;
  lowStockThreshold?: number;
}

export interface ProductCategory {
  id: string;
  name: string;
  description?: string;
  parentId?: string;
  children?: ProductCategory[];
  imageUrl?: string;
}

export interface ProductReview {
  id: string;
  productId: string;
  userId: string;
  username: string;
  rating: number;
  title: string;
  comment: string;
  helpful: number;
  verified: boolean;
  createdAt: Date;
}

export interface ProductSearchFilter {
  category?: string;
  minPrice?: number;
  maxPrice?: number;
  brand?: string[];
  rating?: number;
  inStock?: boolean;
  sortBy?: 'name' | 'price' | 'rating' | 'newest';
  sortOrder?: 'asc' | 'desc';
}

export interface ProductSearchResult {
  products: Product[];
  totalItems: number;
  totalPages: number;
  currentPage: number;
  filters: ProductSearchFilter;
}