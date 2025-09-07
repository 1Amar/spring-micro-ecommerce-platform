import { Component, OnInit, ViewChild, OnDestroy } from '@angular/core';
import { MatTableDataSource } from '@angular/material/table';
import { MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SelectionModel } from '@angular/cdk/collections';
import { FormBuilder, FormGroup } from '@angular/forms';
import { Subject, debounceTime, distinctUntilChanged, takeUntil } from 'rxjs';
import { AdminProductService, ProductDto, ProductSearchFilters } from './services/admin-product.service';
import { InventoryService, InventoryDto } from '../../core/services/inventory.service';
import { ProductFormDialogComponent } from './components/product-form-dialog.component';
import { ConfirmDialogComponent } from '../shared/components/confirm-dialog.component';
import { StockAdjustmentDialogComponent } from './components/stock-adjustment-dialog.component';

@Component({
  selector: 'app-admin-products',
  templateUrl: './admin-products.component.html',
  styleUrls: ['./admin-products.component.scss']
})
export class AdminProductsComponent implements OnInit, OnDestroy {
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  private destroy$ = new Subject<void>();
  
  displayedColumns: string[] = [
    'select', 'image', 'name', 'sku', 'brand', 'category', 
    'price', 'stock', 'status', 'featured', 'actions'
  ];
  
  dataSource = new MatTableDataSource<ProductDto>();
  selection = new SelectionModel<ProductDto>(true, []);
  
  // Pagination
  totalElements = 0;
  pageSize = 20;
  pageIndex = 0;
  pageSizeOptions = [10, 20, 50, 100];
  
  // Loading states
  loading = false;
  
  // Search and filters
  searchForm: FormGroup;
  brands: string[] = [];
  categories: any[] = []; // Will be populated from category service later
  
  constructor(
    private productService: AdminProductService,
    private inventoryService: InventoryService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar,
    private fb: FormBuilder
  ) {
    this.searchForm = this.createSearchForm();
  }

  ngOnInit(): void {
    this.loadProducts();
    this.loadBrands();
    this.setupSearchListener();
    
    // Listen for refresh events
    this.productService.refresh$
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => this.loadProducts());
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private createSearchForm(): FormGroup {
    return this.fb.group({
      name: [''],
      brand: [''],
      categoryId: [''],
      minPrice: [''],
      maxPrice: [''],
      inStock: [''],
      featured: ['']
    });
  }

  private setupSearchListener(): void {
    this.searchForm.valueChanges
      .pipe(
        debounceTime(500),
        distinctUntilChanged(),
        takeUntil(this.destroy$)
      )
      .subscribe(() => {
        this.pageIndex = 0;
        this.loadProducts();
      });
  }

  private loadProducts(): void {
    this.loading = true;
    const filters = this.getFiltersFromForm();
    const hasFilters = Object.values(filters).some(value => value !== undefined && value !== '' && value !== null);
    
    const request = hasFilters 
      ? this.productService.searchProducts(filters, this.pageIndex, this.pageSize, 'name', 'asc')
      : this.productService.getAllProducts(this.pageIndex, this.pageSize, 'createdAt', 'desc');

    request.subscribe({
      next: (response) => {
        // Enrich products with real-time inventory data
        this.enrichProductsWithInventory(response.content);
        this.totalElements = response.totalElements;
      },
      error: (error) => {
        console.error('Error loading products:', error);
        this.showSnackBar('Error loading products. Please try again.');
        this.loading = false;
      }
    });
  }

  private enrichProductsWithInventory(products: ProductDto[]): void {
    if (products.length === 0) {
      this.dataSource.data = products;
      this.loading = false;
      return;
    }

    // Get inventory data for all products
    const productIds = products.map(p => p.id!);
    
    this.inventoryService.getInventoryForProducts(productIds).subscribe({
      next: (inventoryData: InventoryDto[]) => {
        // Map inventory data to products
        const inventoryMap = new Map<number, InventoryDto>();
        inventoryData.forEach(inv => {
          if (inv.productId) {
            inventoryMap.set(inv.productId, inv);
          }
        });

        // Update products with real inventory data
        const enrichedProducts = products.map(product => {
          const inventory = inventoryMap.get(product.id!);
          if (inventory) {
            const stockStatus = this.inventoryService.getStockStatus(inventory);
            return {
              ...product,
              stockQuantity: inventory.availableQuantity || 0,
              inStock: stockStatus.inStock,
              lowStock: stockStatus.isLowStock,
              inventoryStockStatus: stockStatus.stockStatus,
              availableQuantity: inventory.availableQuantity,
              stockMessage: `${inventory.availableQuantity} available`
            };
          }
          return product;
        });

        this.dataSource.data = enrichedProducts;
        this.loading = false;
      },
      error: (error) => {
        console.warn('Could not load inventory data, using product service data:', error);
        // Fallback to original products if inventory service fails
        this.dataSource.data = products;
        this.loading = false;
      }
    });
  }

  private loadBrands(): void {
    this.productService.getDistinctBrands().subscribe({
      next: (brands) => this.brands = brands,
      error: (error) => console.error('Error loading brands:', error)
    });
  }

  private getFiltersFromForm(): ProductSearchFilters {
    const formValue = this.searchForm.value;
    const filters: ProductSearchFilters = {};
    
    if (formValue.name?.trim()) filters.name = formValue.name.trim();
    if (formValue.brand) filters.brand = formValue.brand;
    if (formValue.categoryId) filters.categoryId = Number(formValue.categoryId);
    if (formValue.minPrice) filters.minPrice = Number(formValue.minPrice);
    if (formValue.maxPrice) filters.maxPrice = Number(formValue.maxPrice);
    if (formValue.inStock === true || formValue.inStock === false) filters.inStock = formValue.inStock;
    if (formValue.featured === true || formValue.featured === false) filters.featured = formValue.featured;
    
    return filters;
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadProducts();
  }

  onClearFilters(): void {
    this.searchForm.reset();
    this.selection.clear();
  }

  // Selection methods
  isAllSelected(): boolean {
    const numSelected = this.selection.selected.length;
    const numRows = this.dataSource.data.length;
    return numSelected === numRows;
  }

  masterToggle(): void {
    this.isAllSelected() ?
      this.selection.clear() :
      this.dataSource.data.forEach(row => this.selection.select(row));
  }

  // CRUD Operations
  onCreateProduct(): void {
    const dialogRef = this.dialog.open(ProductFormDialogComponent, {
      width: '800px',
      maxHeight: '90vh',
      data: { mode: 'create', product: null }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.createProduct(result);
      }
    });
  }

  onEditProduct(product: ProductDto): void {
    const dialogRef = this.dialog.open(ProductFormDialogComponent, {
      width: '800px',
      maxHeight: '90vh',
      data: { mode: 'edit', product: { ...product } }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.updateProduct(product.id!, result);
      }
    });
  }

  onDeleteProduct(product: ProductDto): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '400px',
      data: {
        title: 'Delete Product',
        message: `Are you sure you want to delete "${product.name}"? This action can be undone.`,
        confirmText: 'Delete',
        cancelText: 'Cancel'
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.deleteProduct(product.id!);
      }
    });
  }

  onToggleFeatured(product: ProductDto): void {
    this.productService.toggleFeaturedStatus(product.id!).subscribe({
      next: (updatedProduct) => {
        this.showSnackBar(`Product ${updatedProduct.isFeatured ? 'marked as featured' : 'removed from featured'}`);
      },
      error: (error) => {
        console.error('Error toggling featured status:', error);
        this.showSnackBar('Error updating featured status. Please try again.');
      }
    });
  }

  onAdjustStock(product: ProductDto): void {
    const dialogRef = this.dialog.open(StockAdjustmentDialogComponent, {
      width: '400px',
      data: { product }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.adjustStock(product.id!, result.adjustment, result.type);
      }
    });
  }

  // Bulk operations
  onBulkDelete(): void {
    const selectedProducts = this.selection.selected;
    if (selectedProducts.length === 0) {
      this.showSnackBar('Please select products to delete.');
      return;
    }

    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '400px',
      data: {
        title: 'Bulk Delete Products',
        message: `Are you sure you want to delete ${selectedProducts.length} selected products?`,
        confirmText: 'Delete All',
        cancelText: 'Cancel'
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.bulkDeleteProducts(selectedProducts.map(p => p.id!));
      }
    });
  }

  onBulkToggleFeatured(): void {
    const selectedProducts = this.selection.selected;
    if (selectedProducts.length === 0) {
      this.showSnackBar('Please select products to update.');
      return;
    }

    selectedProducts.forEach(product => {
      this.productService.toggleFeaturedStatus(product.id!).subscribe({
        error: (error) => console.error('Error in bulk featured toggle:', error)
      });
    });
    
    this.showSnackBar(`Updated featured status for ${selectedProducts.length} products`);
    this.selection.clear();
  }

  // API calls
  private createProduct(productData: ProductDto): void {
    this.productService.createProduct(productData).subscribe({
      next: (product) => {
        this.showSnackBar('Product created successfully!');
      },
      error: (error) => {
        console.error('Error creating product:', error);
        this.showSnackBar('Error creating product. Please try again.');
      }
    });
  }

  private updateProduct(id: number, productData: ProductDto): void {
    this.productService.updateProduct(id, productData).subscribe({
      next: (product) => {
        this.showSnackBar('Product updated successfully!');
      },
      error: (error) => {
        console.error('Error updating product:', error);
        this.showSnackBar('Error updating product. Please try again.');
      }
    });
  }

  private deleteProduct(id: number): void {
    this.productService.deleteProduct(id).subscribe({
      next: () => {
        this.showSnackBar('Product deleted successfully!');
      },
      error: (error) => {
        console.error('Error deleting product:', error);
        this.showSnackBar('Error deleting product. Please try again.');
      }
    });
  }

  private adjustStock(id: number, adjustment: number, type: 'set' | 'adjust'): void {
    const request = type === 'set' 
      ? this.productService.updateStockQuantity(id, adjustment)
      : this.productService.adjustStockQuantity(id, adjustment);

    request.subscribe({
      next: () => {
        this.showSnackBar(`Stock ${type === 'set' ? 'updated' : 'adjusted'} successfully!`);
      },
      error: (error) => {
        console.error('Error adjusting stock:', error);
        this.showSnackBar('Error adjusting stock. Please try again.');
      }
    });
  }

  private bulkDeleteProducts(productIds: number[]): void {
    const deletePromises = productIds.map(id => 
      this.productService.deleteProduct(id).toPromise()
    );

    Promise.all(deletePromises).then(() => {
      this.showSnackBar(`Successfully deleted ${productIds.length} products`);
      this.selection.clear();
    }).catch(error => {
      console.error('Error in bulk delete:', error);
      this.showSnackBar('Some products could not be deleted. Please try again.');
    });
  }

  // Utility methods
  getStockStatusClass(product: ProductDto): string {
    // Use the backend's inStock and lowStock flags if available
    if (product.inStock === false || product.stockQuantity === 0) return 'out-of-stock';
    if (product.lowStock === true || (product.stockQuantity <= product.lowStockThreshold)) return 'low-stock';
    return 'in-stock';
  }

  getStockStatusText(product: ProductDto): string {
    // Use the backend's inStock and lowStock flags if available
    if (product.inStock === false || product.stockQuantity === 0) return 'Out of Stock';
    if (product.lowStock === true || (product.stockQuantity <= product.lowStockThreshold)) return 'Low Stock';
    return 'In Stock';
  }

  onImageError(event: any): void {
    event.target.src = '/assets/images/product-placeholder.svg';
  }

  private showSnackBar(message: string): void {
    this.snackBar.open(message, 'Close', {
      duration: 3000,
      horizontalPosition: 'right',
      verticalPosition: 'top'
    });
  }
}