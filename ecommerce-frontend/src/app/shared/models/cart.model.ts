export interface CartItem {
  id: string;
  productId: string;
  productName: string;
  productImage: string;
  sku: string;
  price: number;
  quantity: number;
  maxQuantity: number;
  specifications?: { [key: string]: string };
  addedAt: Date;
}

export interface Cart {
  id: string;
  userId?: string;
  items: CartItem[];
  itemCount: number;
  subtotal: number;
  tax: number;
  shipping: number;
  discount: number;
  total: number;
  currency: string;
  updatedAt: Date;
}

export interface AddToCartRequest {
  productId: string;
  quantity: number;
  specifications?: { [key: string]: string };
}

export interface UpdateCartItemRequest {
  cartItemId: string;
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