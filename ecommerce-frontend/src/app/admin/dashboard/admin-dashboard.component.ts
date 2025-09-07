import { Component, OnInit } from '@angular/core';
import { forkJoin, Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

interface DashboardStats {
  totalProducts: number;
  lowStockAlerts: number;
  totalUsers: number;
  activeUsers: number;
  outOfStockProducts: number;
  recentRegistrations: number;
}

interface QuickAction {
  icon: string;
  title: string;
  description: string;
  route: string;
  color: string;
}

interface RecentActivity {
  type: string;
  message: string;
  timestamp: Date;
  icon: string;
  color: string;
}

@Component({
  selector: 'app-admin-dashboard',
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.scss']
})
export class AdminDashboardComponent implements OnInit {

  loading = true;
  stats: DashboardStats = {
    totalProducts: 0,
    lowStockAlerts: 0,
    totalUsers: 0,
    activeUsers: 0,
    outOfStockProducts: 0,
    recentRegistrations: 0
  };

  quickActions: QuickAction[] = [
    {
      icon: 'add_circle',
      title: 'Add Product',
      description: 'Create a new product',
      route: '/admin/products/create',
      color: 'primary'
    },
    {
      icon: 'inventory',
      title: 'Stock Adjustment',
      description: 'Adjust inventory levels',
      route: '/admin/inventory/adjust',
      color: 'accent'
    },
    {
      icon: 'person_add',
      title: 'Manage Users',
      description: 'User administration',
      route: '/admin/users',
      color: 'primary'
    },
    {
      icon: 'analytics',
      title: 'View Reports',
      description: 'Generate reports',
      route: '/admin/reports',
      color: 'accent'
    }
  ];

  recentActivities: RecentActivity[] = [
    {
      type: 'product',
      message: 'New product "Samsung Galaxy S24" added to inventory',
      timestamp: new Date(Date.now() - 1000 * 60 * 15), // 15 minutes ago
      icon: 'add_shopping_cart',
      color: 'primary'
    },
    {
      type: 'stock',
      message: 'Low stock alert for "iPhone 15 Pro" (5 units remaining)',
      timestamp: new Date(Date.now() - 1000 * 60 * 30), // 30 minutes ago
      icon: 'warning',
      color: 'warn'
    },
    {
      type: 'user',
      message: 'New user registration: john.doe@example.com',
      timestamp: new Date(Date.now() - 1000 * 60 * 45), // 45 minutes ago
      icon: 'person_add',
      color: 'accent'
    },
    {
      type: 'order',
      message: 'Order #1234 processed successfully',
      timestamp: new Date(Date.now() - 1000 * 60 * 60), // 1 hour ago
      icon: 'shopping_bag',
      color: 'primary'
    },
    {
      type: 'stock',
      message: 'Stock adjustment: +50 units for "MacBook Air M2"',
      timestamp: new Date(Date.now() - 1000 * 60 * 90), // 1.5 hours ago
      icon: 'inventory_2',
      color: 'accent'
    }
  ];

  constructor() { }

  ngOnInit(): void {
    this.loadDashboardData();
  }

  private loadDashboardData(): void {
    this.loading = true;

    // Simulate API calls for dashboard data
    // In a real implementation, these would be actual HTTP calls
    setTimeout(() => {
      this.stats = {
        totalProducts: 1456789,  // Using the actual product count from your database
        lowStockAlerts: 23,
        totalUsers: 1247,
        activeUsers: 892,
        outOfStockProducts: 12,
        recentRegistrations: 45
      };
      this.loading = false;
    }, 1000);
  }

  getRelativeTime(date: Date): string {
    const now = new Date();
    const diffInMs = now.getTime() - date.getTime();
    const diffInMinutes = Math.floor(diffInMs / (1000 * 60));
    
    if (diffInMinutes < 1) return 'Just now';
    if (diffInMinutes < 60) return `${diffInMinutes} minutes ago`;
    
    const diffInHours = Math.floor(diffInMinutes / 60);
    if (diffInHours < 24) return `${diffInHours} hours ago`;
    
    const diffInDays = Math.floor(diffInHours / 24);
    return `${diffInDays} days ago`;
  }

  refreshStats(): void {
    this.loadDashboardData();
  }

  trackActivity(index: number, activity: RecentActivity): any {
    return activity.timestamp;
  }
}