export interface Order {
  id: string;
  userId: string;
  orderNumber: string;
  status: OrderStatus;
  items: OrderItem[];
  subtotal: number;
  taxAmount: number;
  shippingCost: number;
  discountAmount: number;
  totalAmount: number;
  paymentMethod: string;
  paymentStatus: PaymentStatus;
  paymentTransactionId?: string;
  customerEmail: string;
  customerPhone?: string;
  
  // Billing address
  billingFirstName?: string;
  billingLastName?: string;
  billingCompany?: string;
  billingStreet?: string;
  billingCity?: string;
  billingState?: string;
  billingPostalCode?: string;
  billingCountry?: string;
  
  // Shipping address  
  shippingFirstName?: string;
  shippingLastName?: string;
  shippingCompany?: string;
  shippingStreet?: string;
  shippingCity?: string;
  shippingState?: string;
  shippingPostalCode?: string;
  shippingCountry?: string;
  
  shippingMethod?: string;
  trackingNumber?: string;
  carrier?: string;
  
  createdAt: Date;
  updatedAt: Date;
  shippedAt?: Date;
  deliveredAt?: Date;
  cancelledAt?: Date;
  
  notes?: string;
  adminNotes?: string;
  cancellationReason?: string;
  fulfillmentStatus?: string;
}

export interface OrderItem {
  id: string;
  orderId: string;
  productId: number;
  productName: string;
  productSku: string;
  productDescription?: string;
  productImageUrl?: string;
  productBrand?: string;
  productCategory?: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
  listPrice?: number;
  discountAmount?: number;
  taxAmount?: number;
  fulfillmentStatus?: string;
  quantityShipped?: number;
  quantityDelivered?: number;
  quantityCancelled?: number;
  quantityReturned?: number;
}

export enum OrderStatus {
  PENDING = 'PENDING',
  CONFIRMED = 'CONFIRMED',
  PROCESSING = 'PROCESSING',
  SHIPPED = 'SHIPPED',
  DELIVERED = 'DELIVERED',
  CANCELLED = 'CANCELLED',
  REFUNDED = 'REFUNDED'
}

export enum PaymentStatus {
  PENDING = 'PENDING',
  PROCESSING = 'PROCESSING',
  PAID = 'PAID',
  FAILED = 'FAILED',
  REFUNDED = 'REFUNDED',
  PARTIALLY_REFUNDED = 'PARTIALLY_REFUNDED'
}

export interface Address {
  id?: string;
  type: 'shipping' | 'billing';
  firstName: string;
  lastName: string;
  company?: string;
  street: string;
  apartment?: string;
  city: string;
  state: string;
  zipCode: string;
  country: string;
  phone?: string;
  isDefault?: boolean;
}

export interface PaymentMethod {
  id: string;
  type: 'credit_card' | 'debit_card' | 'paypal' | 'bank_transfer';
  provider?: string;
  lastFour?: string;
  expiryMonth?: number;
  expiryYear?: number;
  isDefault?: boolean;
}

export interface CreateOrderRequest {
  userId: string;
  cartId?: string;
  items: CreateOrderItemRequest[];
  paymentMethod: string;
  customerEmail: string;
  customerPhone?: string;
  
  // Billing address
  billingFirstName: string;
  billingLastName: string;
  billingCompany?: string;
  billingStreet: string;
  billingCity: string;
  billingState: string;
  billingPostalCode: string;
  billingCountry: string;
  
  // Shipping address
  shippingFirstName: string;
  shippingLastName: string;
  shippingCompany?: string;
  shippingStreet: string;
  shippingCity: string;
  shippingState: string;
  shippingPostalCode: string;
  shippingCountry: string;
  
  shippingMethod?: string;
  sameAsBilling: boolean;
  notes?: string;
  couponCode?: string;
  expectedTotal?: number;
}

export interface CreateOrderItemRequest {
  productId: number;
  quantity: number;
  unitPrice: number;
  productName?: string;
  productSku?: string;
  productImageUrl?: string;
}

export interface OrderSummary {
  subtotal: number;
  tax: number;
  shipping: number;
  discount: number;
  total: number;
  currency: string;
}