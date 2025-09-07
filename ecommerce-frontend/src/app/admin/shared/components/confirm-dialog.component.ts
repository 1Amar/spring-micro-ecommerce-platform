import { Component, Inject } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';

export interface ConfirmDialogData {
  title: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  icon?: string;
  color?: 'primary' | 'accent' | 'warn';
}

@Component({
  selector: 'app-confirm-dialog',
  template: `
    <div class="confirm-dialog">
      
      <!-- Dialog Header -->
      <div mat-dialog-title class="dialog-header">
        <div class="title-content">
          <mat-icon [color]="data.color || 'primary'" class="dialog-icon">
            {{ data.icon || 'help_outline' }}
          </mat-icon>
          <h2>{{ data.title }}</h2>
        </div>
      </div>

      <!-- Dialog Content -->
      <div mat-dialog-content class="dialog-content">
        <p>{{ data.message }}</p>
      </div>

      <!-- Dialog Actions -->
      <div mat-dialog-actions class="dialog-actions">
        <button mat-button (click)="onCancel()" type="button">
          {{ data.cancelText || 'Cancel' }}
        </button>
        <button mat-raised-button 
                [color]="data.color || 'primary'"
                (click)="onConfirm()" 
                type="button">
          {{ data.confirmText || 'Confirm' }}
        </button>
      </div>

    </div>
  `,
  styles: [`
    .confirm-dialog {
      width: 400px;
      max-width: 95vw;
    }

    .dialog-header {
      padding: 24px 24px 0 24px !important;
      margin: 0 !important;
      
      .title-content {
        display: flex;
        align-items: center;
        gap: 12px;
        
        .dialog-icon {
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
      padding: 20px 24px !important;
      
      p {
        margin: 0;
        color: #666;
        line-height: 1.5;
      }
    }

    .dialog-actions {
      padding: 0 24px 24px 24px !important;
      margin: 0 !important;
      display: flex;
      justify-content: flex-end;
      gap: 12px;
    }

    // Responsive
    @media (max-width: 480px) {
      .dialog-actions {
        flex-direction: column-reverse;
        
        button {
          width: 100%;
          margin-bottom: 8px;
          
          &:last-child {
            margin-bottom: 0;
          }
        }
      }
    }
  `]
})
export class ConfirmDialogComponent {

  constructor(
    private dialogRef: MatDialogRef<ConfirmDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ConfirmDialogData
  ) {}

  onConfirm(): void {
    this.dialogRef.close(true);
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }
}