import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Subject, takeUntil, finalize } from 'rxjs';
import { MatSnackBar } from '@angular/material/snack-bar';
import { KeycloakService } from 'keycloak-angular';

import { UserService } from '../../core/services/user.service';
import { LoadingService } from '../../core/services/loading.service';
import { 
  User, 
  UserProfile, 
  UserAddress, 
  CreateUserProfileRequest, 
  UpdateUserProfileRequest,
  CreateUserAddressRequest,
  UpdateUserAddressRequest,
  UpdateUserRequest
} from '../../shared/models/user.model';

@Component({
  selector: 'app-profile',
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss']
})
export class ProfileComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  
  currentUser: User | null = null;
  profileForm!: FormGroup;
  addressForm!: FormGroup;
  
  isEditingProfile = false;
  isEditingAddress = false;
  isAddingAddress = false;
  selectedAddressIndex = -1;
  
  loading = false;
  profileLoading = false;
  addressLoading = false;

  genderOptions = [
    { value: 'MALE', label: 'Male' },
    { value: 'FEMALE', label: 'Female' },
    { value: 'OTHER', label: 'Other' },
    { value: 'PREFER_NOT_TO_SAY', label: 'Prefer not to say' }
  ];

  addressTypes = [
    { value: 'HOME', label: 'Home' },
    { value: 'WORK', label: 'Work' },
    { value: 'OTHER', label: 'Other' }
  ];

  constructor(
    private fb: FormBuilder,
    private userService: UserService,
    private loadingService: LoadingService,
    private snackBar: MatSnackBar,
    private keycloak: KeycloakService
  ) {
    this.initializeForms();
  }

  ngOnInit(): void {
    this.loadCurrentUser();
    
    // Subscribe to user changes
    this.userService.currentUser$
      .pipe(takeUntil(this.destroy$))
      .subscribe(user => {
        this.currentUser = user;
        if (user) {
          this.updateProfileForm(user.profile);
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private initializeForms(): void {
    this.profileForm = this.fb.group({
      firstName: ['', [Validators.maxLength(100)]],
      lastName: ['', [Validators.maxLength(100)]],
      phoneNumber: ['', [Validators.maxLength(15)]],
      dateOfBirth: [''],
      gender: [''],
      bio: ['', [Validators.maxLength(500)]],
      avatarUrl: ['', [Validators.maxLength(255), Validators.pattern('https?://.+')]]
    });

    this.addressForm = this.fb.group({
      type: ['HOME', [Validators.required]],
      street: ['', [Validators.required, Validators.maxLength(255)]],
      city: ['', [Validators.required, Validators.maxLength(100)]],
      state: ['', [Validators.required, Validators.maxLength(100)]],
      zipCode: ['', [Validators.required, Validators.maxLength(20)]],
      country: ['United States', [Validators.required, Validators.maxLength(100)]],
      isDefault: [false]
    });
  }

  private async loadCurrentUser(): Promise<void> {
    try {
      this.loading = true;
      
      if (await this.keycloak.isLoggedIn()) {
        // Get Keycloak user info from token claims (no CORS issues)
        const keycloakInstance = this.keycloak.getKeycloakInstance();
        const keycloakId = keycloakInstance.subject;
        const tokenParsed = keycloakInstance.idTokenParsed;
        
        if (keycloakId && tokenParsed) {
          console.log('=== DEBUG: Current Keycloak Session ===');
          console.log('Keycloak ID from token:', keycloakId);
          console.log('User email from token:', tokenParsed['email']);
          console.log('Username from token:', tokenParsed['preferred_username']);
          console.log('Full token subject:', keycloakInstance.subject);
          console.log('=====================================');
          
          this.userService.getUserByKeycloakId(keycloakId)
            .pipe(
              takeUntil(this.destroy$),
              finalize(() => this.loading = false)
            )
            .subscribe({
              next: (user) => {
                this.currentUser = user;
                this.userService.setCurrentUser(user);
                this.updateProfileForm(user.profile);
              },
              error: (error) => {
                console.error('Error loading user profile by Keycloak ID:', error);
                
                // Try to find user by email as fallback
                const email = tokenParsed['email'];
                if (email && error.status === 404) {
                  console.log('Trying to find user by email:', email);
                  this.userService.getUserByEmail(email)
                    .pipe(takeUntil(this.destroy$))
                    .subscribe({
                      next: (user) => {
                        console.log('Found user by email:', user);
                        console.log('User has different Keycloak ID in DB:', user.keycloakId, 'vs current session:', keycloakId);
                        
                        // Use the existing user data without updating Keycloak ID
                        // The important thing is we found the user and can load their profile
                        this.currentUser = user;
                        this.userService.setCurrentUser(user);
                        this.updateProfileForm(user.profile);
                        this.showSuccess('Profile loaded successfully!');
                      },
                      error: (emailError) => {
                        console.log('User not found by email either, creating new user');
                        // Create user if not exists - use token data from properly configured Keycloak
                        const email = tokenParsed['email'] || `${keycloakId.substring(0, 8)}@temp.local`;
                        const username = tokenParsed['preferred_username'] || tokenParsed['email'] || `user_${keycloakId.substring(0, 8)}`;
                        const fullName = tokenParsed['name'] || '';
                        const nameParts = fullName.split(' ');
                        
                        const keycloakProfile = {
                          email: email,
                          username: username,
                          firstName: tokenParsed['given_name'] || nameParts[0] || '',
                          lastName: tokenParsed['family_name'] || nameParts.slice(1).join(' ') || ''
                        };
                        
                        console.log('Creating user with profile:', keycloakProfile);
                        this.createUserFromKeycloak(keycloakProfile, keycloakId);
                      }
                    });
                } else {
                  // No email or different error, show error message
                  this.showError('Unable to load user profile. Please try logging in again.');
                }
              }
            });
        }
      }
    } catch (error) {
      console.error('Error loading current user:', error);
      this.loading = false;
      this.showError('Failed to load user profile');
    }
  }

  private createUserFromKeycloak(keycloakProfile: any, keycloakId: string): void {
    const createRequest = {
      keycloakId: keycloakId,
      email: keycloakProfile.email || '',
      username: keycloakProfile.username || keycloakProfile.email || '',
      isActive: true
    };

    this.userService.createUser(createRequest)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.loading = false)
      )
      .subscribe({
        next: (user) => {
          this.currentUser = user;
          this.userService.setCurrentUser(user);
          
          // Refresh user data to get complete profile information
          this.userService.getUserById(user.id)
            .pipe(takeUntil(this.destroy$))
            .subscribe({
              next: (refreshedUser) => {
                this.currentUser = refreshedUser;
                this.userService.setCurrentUser(refreshedUser);
                this.updateProfileForm(refreshedUser.profile);
              },
              error: (refreshError) => {
                console.error('Error refreshing user data:', refreshError);
                // Continue with original user data
                this.updateProfileForm(user.profile);
              }
            });
          
          this.showSuccess('Welcome! Your profile has been created.');
        },
        error: (error) => {
          console.error('Error creating user:', error);
          this.showError('Failed to create user profile');
        }
      });
  }

  private updateProfileForm(profile?: UserProfile): void {
    if (profile) {
      this.profileForm.patchValue({
        firstName: profile.firstName || '',
        lastName: profile.lastName || '',
        phoneNumber: profile.phoneNumber || '',
        dateOfBirth: profile.dateOfBirth ? new Date(profile.dateOfBirth) : '',
        gender: profile.gender || '',
        bio: profile.bio || '',
        avatarUrl: profile.avatarUrl || ''
      });
    }
  }

  // Profile Management
  startEditingProfile(): void {
    this.isEditingProfile = true;
  }

  cancelEditProfile(): void {
    this.isEditingProfile = false;
    this.updateProfileForm(this.currentUser?.profile);
  }

  saveProfile(): void {
    if (this.profileForm.invalid || !this.currentUser) {
      return;
    }

    this.profileLoading = true;
    const formValue = this.profileForm.value;
    
    const request: CreateUserProfileRequest | UpdateUserProfileRequest = {
      firstName: formValue.firstName || undefined,
      lastName: formValue.lastName || undefined,
      phoneNumber: formValue.phoneNumber || undefined,
      dateOfBirth: formValue.dateOfBirth ? new Date(formValue.dateOfBirth) : undefined,
      gender: formValue.gender || undefined,
      bio: formValue.bio || undefined,
      avatarUrl: formValue.avatarUrl || undefined
    };

    const operation = this.currentUser.profile?.id 
      ? this.userService.updateUserProfile(this.currentUser.id, request)
      : this.userService.createUserProfile(this.currentUser.id, request);

    operation
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.profileLoading = false)
      )
      .subscribe({
        next: (updatedUser) => {
          this.currentUser = updatedUser;
          this.userService.setCurrentUser(updatedUser);
          this.isEditingProfile = false;
          this.showSuccess('Profile updated successfully!');
        },
        error: (error) => {
          console.error('Error updating profile:', error);
          
          // Smart retry logic: if CREATE failed because profile exists, try UPDATE
          // if UPDATE failed because profile doesn't exist, try CREATE
          const errorMessage = error?.error?.message || '';
          const isProfileExistsError = errorMessage.includes('Profile already exists');
          const isProfileNotFoundError = errorMessage.includes('Profile not found') || error?.status === 404;
          
          if (isProfileExistsError || isProfileNotFoundError) {
            console.log('Retrying with opposite operation...');
            const retryOperation = isProfileExistsError 
              ? this.userService.updateUserProfile(this.currentUser!.id, request)
              : this.userService.createUserProfile(this.currentUser!.id, request);
            
            retryOperation
              .pipe(takeUntil(this.destroy$))
              .subscribe({
                next: (updatedUser) => {
                  this.currentUser = updatedUser;
                  this.userService.setCurrentUser(updatedUser);
                  this.isEditingProfile = false;
                  this.showSuccess('Profile updated successfully!');
                },
                error: (retryError) => {
                  console.error('Retry also failed:', retryError);
                  this.showError('Failed to update profile');
                }
              });
          } else {
            this.showError('Failed to update profile');
          }
        }
      });
  }

  // Address Management
  startAddingAddress(): void {
    this.isAddingAddress = true;
    this.addressForm.reset({
      type: 'HOME',
      country: 'United States',
      isDefault: !this.currentUser?.addresses?.length // First address is default
    });
  }

  startEditingAddress(index: number): void {
    const address = this.currentUser?.addresses?.[index];
    if (address) {
      this.isEditingAddress = true;
      this.selectedAddressIndex = index;
      this.addressForm.patchValue({
        type: address.type,
        street: address.street,
        city: address.city,
        state: address.state,
        zipCode: address.zipCode,
        country: address.country,
        isDefault: address.isDefault || false
      });
    }
  }

  cancelAddressEdit(): void {
    this.isAddingAddress = false;
    this.isEditingAddress = false;
    this.selectedAddressIndex = -1;
    this.addressForm.reset();
  }

  saveAddress(): void {
    if (this.addressForm.invalid || !this.currentUser) {
      return;
    }

    this.addressLoading = true;
    const formValue = this.addressForm.value;

    if (this.isAddingAddress) {
      const request: CreateUserAddressRequest = {
        type: formValue.type,
        street: formValue.street,
        city: formValue.city,
        state: formValue.state,
        zipCode: formValue.zipCode,
        country: formValue.country,
        isDefault: formValue.isDefault
      };

      this.userService.addUserAddress(this.currentUser.id, request)
        .pipe(
          takeUntil(this.destroy$),
          finalize(() => this.addressLoading = false)
        )
        .subscribe({
          next: (updatedUser) => {
            this.currentUser = updatedUser;
            this.userService.setCurrentUser(updatedUser);
            this.cancelAddressEdit();
            this.showSuccess('Address added successfully!');
          },
          error: (error) => {
            console.error('Error adding address:', error);
            this.showError('Failed to add address');
          }
        });
    } else if (this.isEditingAddress) {
      const address = this.currentUser.addresses?.[this.selectedAddressIndex];
      if (address?.id) {
        const request: UpdateUserAddressRequest = {
          type: formValue.type,
          street: formValue.street,
          city: formValue.city,
          state: formValue.state,
          zipCode: formValue.zipCode,
          country: formValue.country,
          isDefault: formValue.isDefault
        };

        this.userService.updateUserAddress(this.currentUser.id, address.id, request)
          .pipe(
            takeUntil(this.destroy$),
            finalize(() => this.addressLoading = false)
          )
          .subscribe({
            next: (updatedUser) => {
              this.currentUser = updatedUser;
              this.userService.setCurrentUser(updatedUser);
              this.cancelAddressEdit();
              this.showSuccess('Address updated successfully!');
            },
            error: (error) => {
              console.error('Error updating address:', error);
              this.showError('Failed to update address');
            }
          });
      }
    }
  }

  deleteAddress(index: number): void {
    const address = this.currentUser?.addresses?.[index];
    if (address?.id && this.currentUser) {
      if (confirm('Are you sure you want to delete this address?')) {
        this.addressLoading = true;
        
        this.userService.deleteUserAddress(this.currentUser.id, address.id)
          .pipe(
            takeUntil(this.destroy$),
            finalize(() => this.addressLoading = false)
          )
          .subscribe({
            next: (updatedUser) => {
              this.currentUser = updatedUser;
              this.userService.setCurrentUser(updatedUser);
              this.showSuccess('Address deleted successfully!');
            },
            error: (error) => {
              console.error('Error deleting address:', error);
              this.showError('Failed to delete address');
            }
          });
      }
    }
  }

  setDefaultAddress(index: number): void {
    const address = this.currentUser?.addresses?.[index];
    if (address?.id && this.currentUser) {
      this.addressLoading = true;
      
      this.userService.setDefaultAddress(this.currentUser.id, address.id)
        .pipe(
          takeUntil(this.destroy$),
          finalize(() => this.addressLoading = false)
        )
        .subscribe({
          next: (updatedUser) => {
            this.currentUser = updatedUser;
            this.userService.setCurrentUser(updatedUser);
            this.showSuccess('Default address updated!');
          },
          error: (error) => {
            console.error('Error setting default address:', error);
            this.showError('Failed to set default address');
          }
        });
    }
  }

  // Helper methods
  getDisplayName(): string {
    return this.currentUser ? this.userService.getDisplayName(this.currentUser) : '';
  }

  getInitials(): string {
    return this.currentUser ? this.userService.getInitials(this.currentUser) : '';
  }

  formatAddress(address: UserAddress): string {
    return this.userService.formatAddress(address);
  }

  getShortAddress(address: UserAddress): string {
    return this.userService.getShortAddress(address);
  }

  private showSuccess(message: string): void {
    this.snackBar.open(message, 'Close', {
      duration: 5000,
      panelClass: ['success-snackbar']
    });
  }

  private showError(message: string): void {
    this.snackBar.open(message, 'Close', {
      duration: 5000,
      panelClass: ['error-snackbar']
    });
  }
}