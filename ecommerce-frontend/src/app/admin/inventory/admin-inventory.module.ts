import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AdminInventoryComponent } from './admin-inventory.component';

@NgModule({
  declarations: [
    AdminInventoryComponent
  ],
  imports: [
    CommonModule,
    RouterModule.forChild([
      { path: '', component: AdminInventoryComponent }
    ])
  ]
})
export class AdminInventoryModule { }