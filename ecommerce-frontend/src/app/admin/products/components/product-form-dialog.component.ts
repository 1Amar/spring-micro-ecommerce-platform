import { Component, Inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { ProductDto, AdminProductService } from '../services/admin-product.service';

export interface ProductFormDialogData {
  mode: 'create' | 'edit';
  product: ProductDto | null;
}

@Component({
  selector: 'app-product-form-dialog',
  templateUrl: './product-form-dialog.component.html',
  styleUrls: ['./product-form-dialog.component.scss']
})
export class ProductFormDialogComponent implements OnInit {
  
  productForm: FormGroup;
  isEditMode: boolean;
  brands: string[] = [];
  categories: any[] = []; // Will be populated later
  
  constructor(
    private fb: FormBuilder,
    private productService: AdminProductService,
    private dialogRef: MatDialogRef<ProductFormDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ProductFormDialogData
  ) {
    this.isEditMode = data.mode === 'edit';
    this.productForm = this.createForm();
  }

  ngOnInit(): void {
    this.loadBrands();
    
    if (this.isEditMode && this.data.product) {
      this.populateForm(this.data.product);
    }
    
    // Auto-generate slug when name changes
    this.productForm.get('name')?.valueChanges.subscribe(name => {
      if (!this.isEditMode && name) {
        const slug = this.productService.generateSlug(name);
        this.productForm.patchValue({ slug }, { emitEvent: false });
      }
    });
  }

  private createForm(): FormGroup {
    return this.fb.group({
      name: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(200)]],
      description: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(2000)]],
      price: [0, [Validators.required, Validators.min(0), Validators.max(999999)]],
      sku: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(50)]],
      slug: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(200)]],
      brand: [''],
      categoryId: [1], // Default to category 1 since it's required
      imageUrl: [''],
      thumbnailUrl: [''],
      stockQuantity: [0, [Validators.required, Validators.min(0), Validators.max(999999)]],
      lowStockThreshold: [10, [Validators.required, Validators.min(0), Validators.max(1000)]],
      weight: [null, [Validators.min(0)]],
      dimensions: [''],
      color: [''],
      size: [''],
      material: [''],
      trackInventory: [true],
      isActive: [true],
      isFeatured: [false],
      tags: ['']
    });
  }

  private populateForm(product: ProductDto): void {
    this.productForm.patchValue({
      name: product.name,
      description: product.description,
      price: product.price,
      sku: product.sku,
      slug: product.slug,
      brand: product.brand || '',
      categoryId: product.categoryId,
      imageUrl: product.imageUrl || '',
      thumbnailUrl: product.thumbnailUrl || '',
      stockQuantity: product.stockQuantity,
      lowStockThreshold: product.lowStockThreshold,
      weight: product.weight || null,
      dimensions: product.dimensions || '',
      color: product.color || '',
      size: product.size || '',
      material: product.material || '',
      trackInventory: product.trackInventory,
      isActive: product.isActive,
      isFeatured: product.isFeatured,
      tags: product.tags || ''
    });
  }

  private loadBrands(): void {
    this.productService.getDistinctBrands().subscribe({
      next: (brands) => this.brands = brands,
      error: (error) => console.error('Error loading brands:', error)
    });
  }

  onSubmit(): void {
    if (this.productForm.valid) {
      const formValue = this.productForm.value;
      
      // Process tags - keep as string (backend expects string, not array)
      if (typeof formValue.tags === 'string') {
        // Clean up the tags string but keep it as string
        formValue.tags = formValue.tags.trim();
      }
      
      this.dialogRef.close(formValue);
    }
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  // Utility methods
  getErrorMessage(fieldName: string): string {
    const control = this.productForm.get(fieldName);
    if (control?.hasError('required')) {
      return `${this.getFieldDisplayName(fieldName)} is required`;
    }
    if (control?.hasError('minlength')) {
      const minLength = control.getError('minlength').requiredLength;
      return `${this.getFieldDisplayName(fieldName)} must be at least ${minLength} characters`;
    }
    if (control?.hasError('maxlength')) {
      const maxLength = control.getError('maxlength').requiredLength;
      return `${this.getFieldDisplayName(fieldName)} must not exceed ${maxLength} characters`;
    }
    if (control?.hasError('min')) {
      const min = control.getError('min').min;
      return `${this.getFieldDisplayName(fieldName)} must be at least ${min}`;
    }
    if (control?.hasError('max')) {
      const max = control.getError('max').max;
      return `${this.getFieldDisplayName(fieldName)} must not exceed ${max}`;
    }
    if (control?.hasError('pattern')) {
      if (fieldName === 'imageUrl') {
        return 'Please enter a valid image URL (http:// or https://)';
      }
      return `${this.getFieldDisplayName(fieldName)} format is invalid`;
    }
    return '';
  }

  private getFieldDisplayName(fieldName: string): string {
    const displayNames: { [key: string]: string } = {
      name: 'Product Name',
      description: 'Description',
      price: 'Price',
      sku: 'SKU',
      slug: 'URL Slug',
      brand: 'Brand',
      categoryId: 'Category',
      imageUrl: 'Image URL',
      stockQuantity: 'Stock Quantity',
      lowStockThreshold: 'Low Stock Threshold',
      weight: 'Weight',
      dimensions: 'Dimensions',
      trackInventory: 'Track Inventory'
    };
    return displayNames[fieldName] || fieldName;
  }

  // Generate SKU suggestion
  onGenerateSku(): void {
    const name = this.productForm.get('name')?.value;
    const brand = this.productForm.get('brand')?.value;
    
    if (name && brand) {
      const namePart = name.substring(0, 3).toUpperCase();
      const brandPart = brand.substring(0, 3).toUpperCase();
      const randomPart = Math.floor(Math.random() * 1000).toString().padStart(3, '0');
      const suggestedSku = `${brandPart}-${namePart}-${randomPart}`;
      
      this.productForm.patchValue({ sku: suggestedSku });
    }
  }
}