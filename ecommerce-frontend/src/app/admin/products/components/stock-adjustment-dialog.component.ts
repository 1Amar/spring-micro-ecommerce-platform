import { Component, Inject } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { ProductDto } from '../services/admin-product.service';

export interface StockAdjustmentDialogData {
  product: ProductDto;
}

export interface StockAdjustmentResult {
  type: 'set' | 'adjust';
  adjustment: number;
}

@Component({
  selector: 'app-stock-adjustment-dialog',
  template: `
    <div class="stock-adjustment-dialog">
      
      <!-- Dialog Header -->
      <div mat-dialog-title class="dialog-header">
        <div class="title-content">
          <mat-icon>inventory</mat-icon>
          <h2>Adjust Stock</h2>
        </div>
        <button mat-icon-button (click)="onCancel()" type="button">
          <mat-icon>close</mat-icon>
        </button>
      </div>

      <!-- Dialog Content -->
      <div mat-dialog-content class="dialog-content">
        
        <!-- Product Info -->
        <div class="product-info">
          <div class="product-details">
            <h3>{{ data.product.name }}</h3>
            <p class="sku">SKU: {{ data.product.sku }}</p>
          </div>
          <div class="current-stock">
            <span class="label">Current Stock:</span>
            <span class="stock-value">{{ data.product.stockQuantity }}</span>
          </div>
        </div>

        <!-- Adjustment Form -->
        <form [formGroup]="adjustmentForm" class="adjustment-form">
          
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Adjustment Type</mat-label>
            <mat-select formControlName="type">
              <mat-option value="set">Set Stock Level</mat-option>
              <mat-option value="adjust">Adjust by Amount</mat-option>
            </mat-select>
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>{{ getAdjustmentLabel() }}</mat-label>
            <input matInput 
                   type="number" 
                   formControlName="value"
                   [min]="getMinValue()"
                   [placeholder]="getPlaceholder()">
            <mat-hint>{{ getHintText() }}</mat-hint>
            <mat-error *ngIf="adjustmentForm.get('value')?.invalid && adjustmentForm.get('value')?.touched">
              {{ getErrorMessage() }}
            </mat-error>
          </mat-form-field>

          <!-- Preview -->
          <div class="adjustment-preview" *ngIf="adjustmentForm.valid">
            <div class="preview-row">
              <span class="label">Current Stock:</span>
              <span class="value">{{ data.product.stockQuantity }}</span>
            </div>
            <div class="preview-row">
              <span class="label">{{ getPreviewLabel() }}:</span>
              <span class="value" [class.positive]="getAdjustmentAmount() > 0" 
                                 [class.negative]="getAdjustmentAmount() < 0">
                {{ getAdjustmentSign() }}{{ Math.abs(getAdjustmentAmount()) }}
              </span>
            </div>
            <div class="preview-row total">
              <span class="label">New Stock Level:</span>
              <span class="value">{{ getNewStockLevel() }}</span>
            </div>
          </div>

        </form>
      </div>

      <!-- Dialog Actions -->
      <div mat-dialog-actions class="dialog-actions">
        <button mat-button (click)="onCancel()" type="button">
          Cancel
        </button>
        <button mat-raised-button 
                color="primary"
                (click)="onSubmit()" 
                type="button"
                [disabled]="adjustmentForm.invalid">
          <mat-icon>save</mat-icon>
          Apply Adjustment
        </button>
      </div>

    </div>
  `,
  styles: [`
    .stock-adjustment-dialog {
      width: 450px;
    }

    .dialog-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 20px 24px !important;
      margin: 0 !important;
      border-bottom: 1px solid #e0e0e0;
      
      .title-content {
        display: flex;
        align-items: center;
        gap: 12px;
        
        mat-icon {
          color: #1976d2;
          font-size: 28px;
          width: 28px;
          height: 28px;
        }
        
        h2 {
          margin: 0;
          font-size: 20px;
          font-weight: 500;
          color: #424242;
        }
      }
    }

    .dialog-content {
      padding: 24px !important;
      
      .product-info {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        padding: 16px;
        background-color: #f5f5f5;
        border-radius: 8px;
        margin-bottom: 24px;
        
        .product-details {
          h3 {
            margin: 0 0 4px 0;
            font-size: 16px;
            color: #424242;
          }
          
          .sku {
            margin: 0;
            font-size: 12px;
            color: #666;
            font-family: monospace;
          }
        }
        
        .current-stock {
          text-align: right;
          
          .label {
            display: block;
            font-size: 12px;
            color: #666;
            margin-bottom: 4px;
          }
          
          .stock-value {
            font-size: 24px;
            font-weight: 600;
            color: #1976d2;
          }
        }
      }
      
      .adjustment-form {
        .full-width {
          width: 100%;
          margin-bottom: 16px;
        }
      }
      
      .adjustment-preview {
        background-color: #f9f9f9;
        border-radius: 8px;
        padding: 16px;
        margin-top: 16px;
        
        .preview-row {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 8px;
          
          &:last-child {
            margin-bottom: 0;
          }
          
          &.total {
            border-top: 1px solid #e0e0e0;
            padding-top: 8px;
            font-weight: 600;
            
            .value {
              font-size: 18px;
              color: #1976d2;
            }
          }
          
          .label {
            color: #666;
            font-size: 14px;
          }
          
          .value {
            font-weight: 500;
            
            &.positive {
              color: #4caf50;
            }
            
            &.negative {
              color: #f44336;
            }
          }
        }
      }
    }

    .dialog-actions {
      padding: 16px 24px !important;
      margin: 0 !important;
      border-top: 1px solid #e0e0e0;
      background-color: #fafafa;
      display: flex;
      justify-content: flex-end;
      gap: 12px;
      
      button mat-icon {
        margin-right: 8px;
      }
    }

    // Responsive
    @media (max-width: 480px) {
      .stock-adjustment-dialog {
        width: 95vw;
      }
      
      .dialog-content .product-info {
        flex-direction: column;
        gap: 12px;
        text-align: center;
        
        .current-stock {
          text-align: center;
        }
      }
    }
  `]
})
export class StockAdjustmentDialogComponent {
  
  adjustmentForm: FormGroup;
  Math = Math;

  constructor(
    private fb: FormBuilder,
    private dialogRef: MatDialogRef<StockAdjustmentDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: StockAdjustmentDialogData
  ) {
    this.adjustmentForm = this.createForm();
    this.setupFormValidation();
  }

  private createForm(): FormGroup {
    return this.fb.group({
      type: ['set', Validators.required],
      value: [this.data.product.stockQuantity, [Validators.required, Validators.min(0)]]
    });
  }

  private setupFormValidation(): void {
    // Update validators when type changes
    this.adjustmentForm.get('type')?.valueChanges.subscribe(type => {
      const valueControl = this.adjustmentForm.get('value');
      
      if (type === 'set') {
        valueControl?.setValidators([Validators.required, Validators.min(0)]);
        valueControl?.setValue(this.data.product.stockQuantity);
      } else {
        // For adjust type, allow negative values but ensure final stock >= 0
        const minAdjustment = -this.data.product.stockQuantity;
        valueControl?.setValidators([Validators.required, Validators.min(minAdjustment)]);
        valueControl?.setValue(0);
      }
      
      valueControl?.updateValueAndValidity();
    });
  }

  getAdjustmentLabel(): string {
    const type = this.adjustmentForm.get('type')?.value;
    return type === 'set' ? 'New Stock Level' : 'Adjustment Amount';
  }

  getPlaceholder(): string {
    const type = this.adjustmentForm.get('type')?.value;
    return type === 'set' ? 'Enter new stock level' : 'Enter adjustment (+/-)';
  }

  getHintText(): string {
    const type = this.adjustmentForm.get('type')?.value;
    if (type === 'set') {
      return 'Set the exact stock level';
    } else {
      return 'Use positive numbers to add stock, negative to reduce';
    }
  }

  getMinValue(): number {
    const type = this.adjustmentForm.get('type')?.value;
    return type === 'set' ? 0 : -this.data.product.stockQuantity;
  }

  getPreviewLabel(): string {
    const type = this.adjustmentForm.get('type')?.value;
    return type === 'set' ? 'Change' : 'Adjustment';
  }

  getAdjustmentAmount(): number {
    const type = this.adjustmentForm.get('type')?.value;
    const value = this.adjustmentForm.get('value')?.value || 0;
    
    if (type === 'set') {
      return value - this.data.product.stockQuantity;
    } else {
      return value;
    }
  }

  getAdjustmentSign(): string {
    const adjustment = this.getAdjustmentAmount();
    if (adjustment > 0) return '+';
    if (adjustment < 0) return '';
    return '';
  }

  getNewStockLevel(): number {
    const type = this.adjustmentForm.get('type')?.value;
    const value = this.adjustmentForm.get('value')?.value || 0;
    
    if (type === 'set') {
      return value;
    } else {
      return this.data.product.stockQuantity + value;
    }
  }

  getErrorMessage(): string {
    const control = this.adjustmentForm.get('value');
    const type = this.adjustmentForm.get('type')?.value;
    
    if (control?.hasError('required')) {
      return 'Value is required';
    }
    
    if (control?.hasError('min')) {
      if (type === 'set') {
        return 'Stock level cannot be negative';
      } else {
        return `Cannot reduce by more than ${this.data.product.stockQuantity} (current stock)`;
      }
    }
    
    return '';
  }

  onSubmit(): void {
    if (this.adjustmentForm.valid) {
      const type = this.adjustmentForm.get('type')?.value;
      const value = this.adjustmentForm.get('value')?.value;
      
      const result: StockAdjustmentResult = {
        type: type,
        adjustment: type === 'set' ? value : value
      };
      
      this.dialogRef.close(result);
    }
  }

  onCancel(): void {
    this.dialogRef.close();
  }
}