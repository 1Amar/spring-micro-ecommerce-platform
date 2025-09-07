import { Component, OnInit } from '@angular/core';
import { Router, NavigationEnd, ActivatedRoute } from '@angular/router';
import { KeycloakService } from 'keycloak-angular';
import { filter, map } from 'rxjs/operators';
import { Observable } from 'rxjs';

interface MenuItem {
  icon: string;
  label: string;
  route: string;
  badge?: number;
}

@Component({
  selector: 'app-admin-layout',
  templateUrl: './admin-layout.component.html',
  styleUrls: ['./admin-layout.component.scss']
})
export class AdminLayoutComponent implements OnInit {
  
  sidenavOpened = true;
  currentUser: any;
  pageTitle$: Observable<string>;
  
  menuItems: MenuItem[] = [
    {
      icon: 'dashboard',
      label: 'Dashboard',
      route: '/admin/dashboard'
    },
    {
      icon: 'inventory_2',
      label: 'Products',
      route: '/admin/products'
    },
    {
      icon: 'warehouse',
      label: 'Inventory',
      route: '/admin/inventory'
    },
    {
      icon: 'people',
      label: 'Users',
      route: '/admin/users'
    },
    {
      icon: 'analytics',
      label: 'Reports',
      route: '/admin/reports'
    }
  ];

  constructor(
    private keycloakService: KeycloakService,
    private router: Router,
    private activatedRoute: ActivatedRoute
  ) {
    this.pageTitle$ = this.router.events.pipe(
      filter(event => event instanceof NavigationEnd),
      map(() => {
        let child = this.activatedRoute.firstChild;
        while (child?.firstChild) {
          child = child.firstChild;
        }
        return child?.snapshot?.data?.['title'] || 'Admin Panel';
      })
    );
  }

  async ngOnInit() {
    try {
      // Get user info directly from token claims (avoids CORS issues with Keycloak account endpoint)
      const keycloakInstance = this.keycloakService.getKeycloakInstance();
      const tokenParsed = keycloakInstance?.idTokenParsed || keycloakInstance?.tokenParsed;
      
      if (tokenParsed) {
        this.currentUser = {
          username: tokenParsed['preferred_username'] || tokenParsed['email'] || 'Unknown',
          firstName: tokenParsed['given_name'] || '',
          lastName: tokenParsed['family_name'] || '',
          email: tokenParsed['email'] || ''
        };
      } else {
        console.warn('No token data available for user profile');
        this.currentUser = { username: 'Unknown User' };
      }
    } catch (error) {
      console.error('Error loading user profile from token claims:', error);
      this.currentUser = { username: 'Unknown User' };
    }
  }

  toggleSidenav() {
    this.sidenavOpened = !this.sidenavOpened;
  }

  isActiveRoute(route: string): boolean {
    return this.router.url.startsWith(route);
  }

  async logout() {
    try {
      await this.keycloakService.logout(window.location.origin);
    } catch (error) {
      console.error('Error during logout:', error);
    }
  }

  goToMainSite() {
    this.router.navigate(['/home']);
  }
}