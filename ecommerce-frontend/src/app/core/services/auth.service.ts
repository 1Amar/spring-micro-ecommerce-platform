import { Injectable } from '@angular/core';
import { KeycloakService } from 'keycloak-angular';
import { Observable, BehaviorSubject } from 'rxjs';
import { KeycloakProfile } from 'keycloak-js';
import { UserService } from './user.service';

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

  constructor(
    private keycloakService: KeycloakService,
    private userService: UserService
  ) {
    // Simple initialization - don't make it async to avoid timing issues
    this.updateAuthState();
  }

  public async initializeAuthState(): Promise<void> {
    try {
      this._isLoggedIn = await this.keycloakService.isLoggedIn();
      if (this._isLoggedIn) {
        this._token = await this.keycloakService.getToken() || '';
        await this.loadUserProfile();
        // Ensure user exists in our database
        await this.ensureUserExistsInDatabase();
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

  public async refreshAuthState(): Promise<void> {
    this.updateAuthState();
    // Also ensure user exists in database when refreshing auth state
    if (this._isLoggedIn) {
      await this.ensureUserExistsInDatabase();
    }
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

  /**
   * Ensures the logged-in user exists in our database
   * Creates them automatically if they don't exist
   */
  private async ensureUserExistsInDatabase(): Promise<void> {
    try {
      const keycloakInstance = this.keycloakService.getKeycloakInstance();
      const keycloakId = keycloakInstance.subject;
      const tokenParsed = keycloakInstance.idTokenParsed;

      if (!keycloakId || !tokenParsed) {
        console.log('No Keycloak ID or token available, skipping database user creation');
        return;
      }

      console.log('🔍 Checking if user exists in database:', keycloakId);

      // First, try to find user by Keycloak ID
      this.userService.getUserByKeycloakId(keycloakId).subscribe({
        next: (user) => {
          console.log('✅ User already exists in database:', user.email);
        },
        error: (error) => {
          if (error.status === 404) {
            // User not found by Keycloak ID, try by email
            const email = tokenParsed['email'];
            if (email) {
              console.log('🔍 User not found by Keycloak ID, trying by email:', email);
              this.userService.getUserByEmail(email).subscribe({
                next: (user) => {
                  console.log('✅ User found by email (different Keycloak ID):', user.email);
                },
                error: (emailError) => {
                  if (emailError.status === 404) {
                    // User doesn't exist at all, create them
                    console.log('📝 Creating new user in database');
                    this.createUserInDatabase(keycloakId, tokenParsed);
                  } else {
                    console.error('Error checking user by email:', emailError);
                  }
                }
              });
            } else {
              console.error('No email in token, cannot create user');
            }
          } else {
            console.error('Error checking user by Keycloak ID:', error);
          }
        }
      });
    } catch (error) {
      console.error('Error ensuring user exists in database:', error);
    }
  }

  /**
   * Creates a new user in our database using Keycloak token data
   */
  private createUserInDatabase(keycloakId: string, tokenParsed: any): void {
    const email = tokenParsed['email'] || `${keycloakId.substring(0, 8)}@temp.local`;
    const username = tokenParsed['preferred_username'] || tokenParsed['email'] || `user_${keycloakId.substring(0, 8)}`;
    const fullName = tokenParsed['name'] || '';
    const nameParts = fullName.split(' ');

    const createRequest = {
      keycloakId: keycloakId,
      email: email,
      username: username,
      isActive: true
    };

    console.log('Creating user with data:', createRequest);

    this.userService.createUser(createRequest).subscribe({
      next: (user) => {
        console.log('✅ User created successfully in database:', user.email);
        // Optionally create a basic profile
        if (tokenParsed['given_name'] || tokenParsed['family_name'] || tokenParsed['name']) {
          this.createBasicProfile(user, tokenParsed, nameParts);
        }
      },
      error: (error) => {
        console.error('❌ Error creating user in database:', error);
      }
    });
  }

  /**
   * Creates a basic profile for the newly created user
   */
  private createBasicProfile(user: any, tokenParsed: any, nameParts: string[]): void {
    const profileRequest = {
      firstName: tokenParsed['given_name'] || nameParts[0] || '',
      lastName: tokenParsed['family_name'] || nameParts.slice(1).join(' ') || ''
    };

    if (profileRequest.firstName || profileRequest.lastName) {
      this.userService.createUserProfile(user.id, profileRequest).subscribe({
        next: (updatedUser) => {
          console.log('✅ Basic profile created for user:', updatedUser.email);
        },
        error: (error) => {
          console.log('ℹ️ Could not create basic profile (user can create it manually):', error);
        }
      });
    }
  }
}