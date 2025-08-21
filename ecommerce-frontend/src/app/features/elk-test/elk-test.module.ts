import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';
import { HttpClientModule } from '@angular/common/http';

import { ELKTestComponent } from './elk-test.component';

const routes: Routes = [
  { path: '', component: ELKTestComponent }
];

@NgModule({
  declarations: [
    ELKTestComponent
  ],
  imports: [
    CommonModule,
    HttpClientModule,
    RouterModule.forChild(routes)
  ]
})
export class ELKTestModule { }