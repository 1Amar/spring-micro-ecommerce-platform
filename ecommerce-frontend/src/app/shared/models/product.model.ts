import { Page } from './page.model';

// Updated to match backend ProductDto
export interface Product {
  id: number;
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
  brand: string;
  metaTitle?: string;
  metaDescription?: string;
  tags?: string;
  sortOrder: number;
  categoryId: number;
  categoryName: string;
  inStock: boolean;
  lowStock: boolean;
  onSale: boolean;
  savingsAmount?: number;
  savingsPercentage?: number;
  stars?: number;
  reviewCount?: number;
  imageUrl?: string;
  boughtInLastMonth?: number;
  isBestSeller?: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ProductAvailability {
  inStock: boolean;
  quantity: number;
  lowStock?: boolean;
  lowStockThreshold?: number;
}

// Updated to match backend CategoryDto  
export interface ProductCategory {
  id: number;
  name: string;
  slug: string;
  description?: string;
  parentId?: number;
  displayOrder: number;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
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

// Use Page interface for search results
export type ProductSearchResult = Page<Product>;