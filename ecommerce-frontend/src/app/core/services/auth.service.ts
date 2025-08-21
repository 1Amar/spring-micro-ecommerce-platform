import { Injectable } from '@angular/core';
import { KeycloakService } from 'keycloak-angular';
import { Observable, BehaviorSubject } from 'rxjs';
import { KeycloakProfile } from 'keycloak-js';

export interface UserProfile extends KeycloakProfile {
  roles?: string[];
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private userProfileSubject = new BehaviorSubject<UserProfile | null>(null);
  public userProfile$: Observable<UserProfile | null> = this.userProfileSubject.asObservable();
  
  private _isLoggedIn = false;
  private _token = '';

  constructor(private keycloakService: KeycloakService) {
    // Simple initialization - don't make it async to avoid timing issues
    this.updateAuthState();
  }

  public async initializeAuthState(): Promise<void> {
    try {
      this._isLoggedIn = await this.keycloakService.isLoggedIn();
      if (this._isLoggedIn) {
        this._token = await this.keycloakService.getToken() || '';
        await this.loadUserProfile();
      } else {
        this._token = '';
        this.userProfileSubject.next(null);
      }
    } catch (error) {
      console.error('Error initializing auth state:', error);
      this._isLoggedIn = false;
      this._token = '';
      this.userProfileSubject.next(null);
    }
  }

  public get isLoggedIn(): boolean {
    return this._isLoggedIn;
  }

  public get userRoles(): string[] {
    return this.keycloakService.getUserRoles();
  }

  public get username(): string {
    return this.keycloakService.getUsername();
  }

  public get token(): string {
    return this._token;
  }

  public async login(): Promise<void> {
    try {
      await this.keycloakService.login();
    } catch (error) {
      console.error('Login failed:', error);
    }
  }

  public async logout(): Promise<void> {
    await this.keycloakService.logout();
    this._isLoggedIn = false;
    this._token = '';
    this.userProfileSubject.next(null);
  }

  public async loadUserProfile(): Promise<void> {
    if (this._isLoggedIn) {
      try {
        const profile = await this.keycloakService.loadUserProfile();
        const userProfile: UserProfile = {
          ...profile,
          roles: this.userRoles
        };
        this.userProfileSubject.next(userProfile);
      } catch (error) {
        console.warn('Could not load user profile from Keycloak account endpoint. Using token claims instead.');
        // Fallback to basic profile from token claims
        try {
          const basicProfile: UserProfile = {
            username: this.username,
            roles: this.userRoles,
            firstName: this.keycloakService.getKeycloakInstance()?.idTokenParsed?.['given_name'],
            lastName: this.keycloakService.getKeycloakInstance()?.idTokenParsed?.['family_name'],
            email: this.keycloakService.getKeycloakInstance()?.idTokenParsed?.['email']
          };
          this.userProfileSubject.next(basicProfile);
        } catch (tokenError) {
          console.error('Could not extract user profile from token:', tokenError);
          this.userProfileSubject.next({
            username: this.username || 'Unknown',
            roles: this.userRoles
          });
        }
      }
    } else {
      this.userProfileSubject.next(null);
    }
  }

  public hasRole(role: string): boolean {
    return this.userRoles.includes(role);
  }

  public hasAnyRole(roles: string[]): boolean {
    return roles.some(role => this.hasRole(role));
  }

  public async refreshToken(): Promise<boolean> {
    try {
      const refreshed = await this.keycloakService.updateToken(30);
      if (refreshed) {
        this._token = await this.keycloakService.getToken() || '';
      }
      return refreshed;
    } catch (error) {
      console.error('Failed to refresh token:', error);
      return false;
    }
  }

  public getCurrentUser(): UserProfile | null {
    return this.userProfileSubject.value;
  }

  public refreshAuthState(): void {
    this.updateAuthState();
  }

  private updateAuthState(): void {
    // Simple synchronous state update
    try {
      // Use direct Keycloak instance access to avoid async issues
      const keycloakInstance = this.keycloakService.getKeycloakInstance();
      this._isLoggedIn = keycloakInstance?.authenticated || false;
      this._token = keycloakInstance?.token || '';
      
      if (this._isLoggedIn) {
        this.loadUserProfileSync();
      } else {
        this.userProfileSubject.next(null);
      }
    } catch (error) {
      console.error('Error updating auth state:', error);
      this._isLoggedIn = false;
      this._token = '';
      this.userProfileSubject.next(null);
    }
  }
  
  private loadUserProfileSync(): void {
    try {
      const keycloakInstance = this.keycloakService.getKeycloakInstance();
      const profile: UserProfile = {
        username: keycloakInstance?.tokenParsed?.['preferred_username'] || 'Unknown',
        firstName: keycloakInstance?.tokenParsed?.['given_name'],
        lastName: keycloakInstance?.tokenParsed?.['family_name'], 
        email: keycloakInstance?.tokenParsed?.['email'],
        roles: this.userRoles
      };
      this.userProfileSubject.next(profile);
    } catch (error) {
      console.error('Error loading user profile:', error);
      this.userProfileSubject.next({
        username: 'Unknown',
        roles: []
      });
    }
  }
}