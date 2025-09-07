import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AdminLayoutComponent } from './layout/admin-layout.component';
import { AdminDashboardComponent } from './dashboard/admin-dashboard.component';
import { AuthGuard } from '../core/auth/auth.guard';
import { AdminGuard } from '../core/guards/admin.guard';

const routes: Routes = [
  {
    path: '',
    component: AdminLayoutComponent,
    canActivate: [AuthGuard, AdminGuard],
    children: [
      {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full'
      },
      {
        path: 'dashboard',
        component: AdminDashboardComponent,
        data: { title: 'Admin Dashboard' }
      },
      {
        path: 'products',
        loadChildren: () => import('./products/admin-products.module').then(m => m.AdminProductsModule),
        data: { title: 'Product Management' }
      },
      {
        path: 'inventory',
        loadChildren: () => import('./inventory/admin-inventory.module').then(m => m.AdminInventoryModule),
        data: { title: 'Inventory Management' }
      },
      {
        path: 'users',
        loadChildren: () => import('./users/admin-users.module').then(m => m.AdminUsersModule),
        data: { title: 'User Management' }
      },
      {
        path: 'reports',
        loadChildren: () => import('./reports/admin-reports.module').then(m => m.AdminReportsModule),
        data: { title: 'Reports & Analytics' }
      }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class AdminRoutingModule { }