// Cart item matching backend CartItemDto
export interface CartItem {
  productId: number;
  productName: string;
  imageUrl: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
  addedAt: string; // ISO string from backend
}

// Cart matching backend CartDto
export interface Cart {
  cartId: string;
  userId?: string;
  sessionId?: string;
  items: CartItem[];
  totalAmount: number;
  createdAt: string; // ISO string from backend
  updatedAt: string; // ISO string from backend
  itemCount: number; // Computed property from backend
}

// Request DTOs matching backend
export interface AddToCartRequest {
  productId: number;
  quantity: number;
}

export interface UpdateCartItemRequest {
  productId: number;
  quantity: number;
}

export interface ApplyCouponRequest {
  couponCode: string;
}

export interface Coupon {
  id: string;
  code: string;
  description: string;
  discountType: 'percentage' | 'fixed';
  discountValue: number;
  minOrderAmount?: number;
  maxDiscountAmount?: number;
  expiryDate: Date;
  usageLimit?: number;
  usedCount: number;
  isActive: boolean;
}