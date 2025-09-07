import { Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot, Router } from '@angular/router';
import { KeycloakService } from 'keycloak-angular';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AdminGuard implements CanActivate {

  constructor(
    private keycloakService: KeycloakService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {}

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Observable<boolean> | Promise<boolean> | boolean {
    
    // Check if user is authenticated
    if (!this.keycloakService.isLoggedIn()) {
      this.router.navigate(['/home']);
      return false;
    }

    // Check if user has admin role
    const hasAdminRole = this.keycloakService.isUserInRole('admin') || 
                        this.keycloakService.isUserInRole('ADMIN') ||
                        this.keycloakService.isUserInRole('administrator');

    if (!hasAdminRole) {
      console.warn('Admin Guard: Access denied - User does not have admin role');
      this.snackBar.open('Access Denied: You do not have admin privileges', 'Close', {
        duration: 5000,
        panelClass: ['error-snackbar']
      });
      this.router.navigate(['/home']);
      return false;
    }

    return true;
  }
}