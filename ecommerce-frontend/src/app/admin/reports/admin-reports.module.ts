import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AdminReportsComponent } from './admin-reports.component';

@NgModule({
  declarations: [
    AdminReportsComponent
  ],
  imports: [
    CommonModule,
    RouterModule.forChild([
      { path: '', component: AdminReportsComponent }
    ])
  ]
})
export class AdminReportsModule { }