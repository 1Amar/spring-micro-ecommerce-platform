import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';
import { FormsModule } from '@angular/forms';

import { WebSocketTestComponent } from './websocket-test.component';

const routes: Routes = [
  { path: '', component: WebSocketTestComponent }
];

@NgModule({
  declarations: [
    WebSocketTestComponent
  ],
  imports: [
    CommonModule,
    FormsModule,
    RouterModule.forChild(routes)
  ]
})
export class WebSocketTestModule { }