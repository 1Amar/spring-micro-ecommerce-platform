import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject, takeUntil, finalize } from 'rxjs';
import { MatSnackBar } from '@angular/material/snack-bar';

import { CartService } from '@core/services/cart.service';
import { OrderService } from '@core/services/order.service';
import { AuthService } from '@core/services/auth.service';
import { LoadingService } from '@core/services/loading.service';

import { Cart, CartItem } from '@shared/models/cart.model';
import { CreateOrderRequest, CreateOrderItemRequest, Order } from '@shared/models/order.model';

@Component({
  selector: 'app-checkout',
  templateUrl: './checkout.component.html',
  styleUrls: ['./checkout.component.scss']
})
export class CheckoutComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  
  checkoutForm!: FormGroup;
  cart: Cart | null = null;
  isProcessing = false;
  currentStep = 1;
  maxSteps = 4;
  
  user: any = null;
  orderSummary = {
    subtotal: 0,
    tax: 0,
    shipping: 9.99,
    discount: 0,
    total: 0
  };

  paymentMethods = [
    { value: 'CREDIT_CARD', label: 'Credit Card', icon: 'credit_card' },
    { value: 'PAYPAL', label: 'PayPal', icon: 'payment' },
    { value: 'BANK_TRANSFER', label: 'Bank Transfer', icon: 'account_balance' }
  ];

  shippingMethods = [
    { value: 'STANDARD', label: 'Standard Shipping (5-7 days)', price: 9.99 },
    { value: 'EXPRESS', label: 'Express Shipping (2-3 days)', price: 19.99 },
    { value: 'OVERNIGHT', label: 'Overnight Shipping (1 day)', price: 39.99 }
  ];

  constructor(
    private fb: FormBuilder,
    private cartService: CartService,
    private orderService: OrderService,
    private authService: AuthService,
    private loadingService: LoadingService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {
    this.initializeForm();
  }

  ngOnInit(): void {
    this.loadUserData();
    this.loadCartData();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private initializeForm(): void {
    this.checkoutForm = this.fb.group({
      // Customer Information
      customerEmail: ['', [Validators.required, Validators.email]],
      customerPhone: [''],
      
      // Billing Address
      billingFirstName: ['', Validators.required],
      billingLastName: ['', Validators.required],
      billingCompany: [''],
      billingStreet: ['', Validators.required],
      billingCity: ['', Validators.required],
      billingState: ['', Validators.required],
      billingPostalCode: ['', Validators.required],
      billingCountry: ['United States', Validators.required],
      
      // Shipping Address
      sameAsBilling: [true],
      shippingFirstName: [''],
      shippingLastName: [''],
      shippingCompany: [''],
      shippingStreet: [''],
      shippingCity: [''],
      shippingState: [''],
      shippingPostalCode: [''],
      shippingCountry: ['United States'],
      
      // Shipping & Payment
      shippingMethod: ['STANDARD', Validators.required],
      paymentMethod: ['CREDIT_CARD', Validators.required],
      
      // Additional
      notes: [''],
      couponCode: [''],
      
      // Terms
      acceptTerms: [false, Validators.requiredTrue]
    });

    // Watch for same as billing changes
    this.checkoutForm.get('sameAsBilling')?.valueChanges.subscribe(sameAsBilling => {
      this.toggleShippingAddressValidation(!sameAsBilling);
    });

    // Watch for shipping method changes
    this.checkoutForm.get('shippingMethod')?.valueChanges.subscribe(method => {
      this.updateShippingCost(method);
    });
  }

  private loadUserData(): void {
    this.authService.userProfile$.pipe(takeUntil(this.destroy$)).subscribe(user => {
      this.user = user;
      if (user) {
        this.prefillUserData(user);
      }
    });
  }

  private loadCartData(): void {
    this.cartService.cart$.pipe(takeUntil(this.destroy$)).subscribe(cart => {
      this.cart = cart;
      this.calculateOrderSummary();
      
      if (!cart || cart.items.length === 0) {
        this.router.navigate(['/cart']);
        this.snackBar.open('Your cart is empty', 'Close', { duration: 3000 });
      }
    });
  }

  private prefillUserData(user: any): void {
    this.checkoutForm.patchValue({
      customerEmail: user.email || '',
      customerPhone: user.phone || '',
      billingFirstName: user.profile?.firstName || '',
      billingLastName: user.profile?.lastName || ''
    });
  }

  private toggleShippingAddressValidation(required: boolean): void {
    const shippingFields = [
      'shippingFirstName', 'shippingLastName', 'shippingStreet',
      'shippingCity', 'shippingState', 'shippingPostalCode'
    ];

    shippingFields.forEach(field => {
      const control = this.checkoutForm.get(field);
      if (control) {
        if (required) {
          control.setValidators([Validators.required]);
        } else {
          control.clearValidators();
        }
        control.updateValueAndValidity();
      }
    });
  }

  private updateShippingCost(shippingMethod: string): void {
    const method = this.shippingMethods.find(m => m.value === shippingMethod);
    this.orderSummary.shipping = method ? method.price : 9.99;
    this.calculateOrderSummary();
  }

  private calculateOrderSummary(): void {
    if (!this.cart) return;

    this.orderSummary.subtotal = this.cart.items.reduce(
      (sum, item) => sum + (item.unitPrice * item.quantity), 0
    );

    // Simple tax calculation (8.5%)
    this.orderSummary.tax = this.orderSummary.subtotal * 0.085;

    // Apply discount if coupon is valid
    const couponCode = this.checkoutForm.get('couponCode')?.value;
    this.orderSummary.discount = this.calculateDiscount(couponCode, this.orderSummary.subtotal);

    // Calculate total
    this.orderSummary.total = this.orderSummary.subtotal + 
                             this.orderSummary.tax + 
                             this.orderSummary.shipping - 
                             this.orderSummary.discount;
  }

  private calculateDiscount(couponCode: string, subtotal: number): number {
    if (couponCode === 'SAVE10') {
      return subtotal * 0.10;
    }
    return 0;
  }

  applyCoupon(): void {
    const couponCode = this.checkoutForm.get('couponCode')?.value;
    if (couponCode) {
      this.calculateOrderSummary();
      if (this.orderSummary.discount > 0) {
        this.snackBar.open('Coupon applied successfully!', 'Close', { 
          duration: 3000,
          panelClass: ['success-snackbar']
        });
      } else {
        this.snackBar.open('Invalid coupon code', 'Close', { duration: 3000 });
      }
    }
  }

  nextStep(): void {
    if (this.isCurrentStepValid()) {
      this.currentStep = Math.min(this.currentStep + 1, this.maxSteps);
    }
  }

  previousStep(): void {
    this.currentStep = Math.max(this.currentStep - 1, 1);
  }

  public isCurrentStepValid(): boolean {
    switch (this.currentStep) {
      case 1: // Customer & Billing
        return !!(this.checkoutForm.get('customerEmail')?.valid &&
               this.checkoutForm.get('billingFirstName')?.valid &&
               this.checkoutForm.get('billingLastName')?.valid &&
               this.checkoutForm.get('billingStreet')?.valid &&
               this.checkoutForm.get('billingCity')?.valid &&
               this.checkoutForm.get('billingState')?.valid &&
               this.checkoutForm.get('billingPostalCode')?.valid);
               
      case 2: // Shipping
        const sameAsBilling = this.checkoutForm.get('sameAsBilling')?.value;
        if (sameAsBilling) return true;
        
        return !!(this.checkoutForm.get('shippingFirstName')?.valid &&
               this.checkoutForm.get('shippingLastName')?.valid &&
               this.checkoutForm.get('shippingStreet')?.valid &&
               this.checkoutForm.get('shippingCity')?.valid &&
               this.checkoutForm.get('shippingState')?.valid &&
               this.checkoutForm.get('shippingPostalCode')?.valid);
               
      case 3: // Payment & Shipping Method
        return !!(this.checkoutForm.get('paymentMethod')?.valid &&
               this.checkoutForm.get('shippingMethod')?.valid);
               
      case 4: // Review
        return this.checkoutForm.valid;
        
      default:
        return false;
    }
  }

  placeOrder(): void {
    if (!this.checkoutForm.valid || !this.cart || this.isProcessing) {
      return;
    }

    this.isProcessing = true;
    this.loadingService.show();

    const formValue = this.checkoutForm.value;
    
    // Prepare order items
    const orderItems: CreateOrderItemRequest[] = this.cart.items.map(item => ({
      productId: item.productId,
      quantity: item.quantity,
      unitPrice: item.unitPrice,
      productName: item.productName,
      productSku: item.productId.toString(), // Fallback since productSku doesn't exist
      productImageUrl: item.imageUrl
    }));

    // Prepare create order request
    const createOrderRequest: CreateOrderRequest = {
      userId: this.user?.username || '',
      cartId: this.cart.cartId,
      items: orderItems,
      paymentMethod: formValue.paymentMethod,
      customerEmail: formValue.customerEmail,
      customerPhone: formValue.customerPhone,
      
      // Billing address
      billingFirstName: formValue.billingFirstName,
      billingLastName: formValue.billingLastName,
      billingCompany: formValue.billingCompany,
      billingStreet: formValue.billingStreet,
      billingCity: formValue.billingCity,
      billingState: formValue.billingState,
      billingPostalCode: formValue.billingPostalCode,
      billingCountry: formValue.billingCountry,
      
      // Shipping address
      shippingFirstName: formValue.sameAsBilling ? formValue.billingFirstName : formValue.shippingFirstName,
      shippingLastName: formValue.sameAsBilling ? formValue.billingLastName : formValue.shippingLastName,
      shippingCompany: formValue.sameAsBilling ? formValue.billingCompany : formValue.shippingCompany,
      shippingStreet: formValue.sameAsBilling ? formValue.billingStreet : formValue.shippingStreet,
      shippingCity: formValue.sameAsBilling ? formValue.billingCity : formValue.shippingCity,
      shippingState: formValue.sameAsBilling ? formValue.billingState : formValue.shippingState,
      shippingPostalCode: formValue.sameAsBilling ? formValue.billingPostalCode : formValue.shippingPostalCode,
      shippingCountry: formValue.sameAsBilling ? formValue.billingCountry : formValue.shippingCountry,
      
      shippingMethod: formValue.shippingMethod,
      sameAsBilling: formValue.sameAsBilling,
      notes: formValue.notes,
      couponCode: formValue.couponCode,
      expectedTotal: this.orderSummary.total
    };

    this.orderService.createOrder(createOrderRequest)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.isProcessing = false;
          this.loadingService.hide();
        })
      )
      .subscribe({
        next: (order: Order) => {
          this.snackBar.open('Order placed successfully!', 'Close', { 
            duration: 5000,
            panelClass: ['success-snackbar']
          });
          
          // Navigate to order confirmation page
          this.router.navigate(['/orders', order.id], { 
            queryParams: { 
              success: true,
              orderNumber: order.orderNumber 
            }
          });
        },
        error: (error) => {
          console.error('Order creation failed:', error);
          this.snackBar.open(
            error.error?.message || 'Failed to place order. Please try again.',
            'Close',
            { 
              duration: 5000,
              panelClass: ['error-snackbar']
            }
          );
        }
      });
  }

  getStepIcon(step: number): string {
    const icons = {
      1: 'person',
      2: 'local_shipping',
      3: 'payment',
      4: 'check_circle'
    };
    return icons[step as keyof typeof icons] || 'help';
  }

  getStepTitle(step: number): string {
    const titles = {
      1: 'Customer & Billing',
      2: 'Shipping Address',
      3: 'Payment & Shipping',
      4: 'Review & Place Order'
    };
    return titles[step as keyof typeof titles] || '';
  }

  formatPrice(price: number): string {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD'
    }).format(price);
  }
}