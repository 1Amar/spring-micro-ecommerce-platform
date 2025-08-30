import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, BehaviorSubject, map, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  User,
  UserProfile,
  UserAddress,
  CreateUserRequest,
  UpdateUserRequest,
  CreateUserProfileRequest,
  UpdateUserProfileRequest,
  CreateUserAddressRequest,
  UpdateUserAddressRequest,
  UserStats,
  PageResponse
} from '../../shared/models/user.model';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private readonly apiUrl = `${environment.apiUrl}${environment.services.userService}`;
  
  // Current user state management
  private currentUserSubject = new BehaviorSubject<User | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor(private http: HttpClient) {}

  // Current user state management
  setCurrentUser(user: User | null): void {
    this.currentUserSubject.next(user);
  }

  getCurrentUser(): User | null {
    return this.currentUserSubject.value;
  }

  // User CRUD operations
  createUser(request: CreateUserRequest): Observable<User> {
    return this.http.post<User>(this.apiUrl, request);
  }

  getUserById(id: number): Observable<User> {
    return this.http.get<User>(`${this.apiUrl}/${id}`);
  }

  getUserByKeycloakId(keycloakId: string): Observable<User> {
    return this.http.get<User>(`${this.apiUrl}/keycloak/${keycloakId}`).pipe(
      tap(user => this.setCurrentUser(user))
    );
  }

  getUserByEmail(email: string): Observable<User> {
    return this.http.get<User>(`${this.apiUrl}/email/${encodeURIComponent(email)}`);
  }

  getUserByUsername(username: string): Observable<User> {
    return this.http.get<User>(`${this.apiUrl}/username/${username}`);
  }

  updateUser(id: number, request: UpdateUserRequest): Observable<User> {
    return this.http.put<User>(`${this.apiUrl}/${id}`, request).pipe(
      tap(user => {
        if (this.currentUserSubject.value?.id === id) {
          this.setCurrentUser(user);
        }
      })
    );
  }

  deactivateUser(id: number): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/${id}/deactivate`, {});
  }

  activateUser(id: number): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/${id}/activate`, {});
  }

  deleteUser(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  // User search and pagination
  getAllUsers(page: number = 0, size: number = 20, sort: string = 'createdAt,desc'): Observable<PageResponse<User>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);
    
    return this.http.get<PageResponse<User>>(this.apiUrl, { params });
  }

  getActiveUsers(page: number = 0, size: number = 20, sort: string = 'createdAt,desc'): Observable<PageResponse<User>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);
    
    return this.http.get<PageResponse<User>>(`${this.apiUrl}/active`, { params });
  }

  getUsersByStatus(isActive: boolean, page: number = 0, size: number = 20, sort: string = 'createdAt,desc'): Observable<PageResponse<User>> {
    let params = new HttpParams()
      .set('isActive', isActive.toString())
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);
    
    return this.http.get<PageResponse<User>>(`${this.apiUrl}/status`, { params });
  }

  searchUsers(query: string, activeOnly: boolean = false, page: number = 0, size: number = 20, sort: string = 'createdAt,desc'): Observable<PageResponse<User>> {
    let params = new HttpParams()
      .set('q', query)
      .set('activeOnly', activeOnly.toString())
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);
    
    return this.http.get<PageResponse<User>>(`${this.apiUrl}/search`, { params });
  }

  getUserStats(): Observable<UserStats> {
    return this.http.get<UserStats>(`${this.apiUrl}/stats`);
  }

  // User validation
  checkEmailExists(email: string): Observable<{ exists: boolean }> {
    const params = new HttpParams().set('email', email);
    return this.http.get<{ exists: boolean }>(`${this.apiUrl}/exists/email`, { params });
  }

  checkUsernameExists(username: string): Observable<{ exists: boolean }> {
    const params = new HttpParams().set('username', username);
    return this.http.get<{ exists: boolean }>(`${this.apiUrl}/exists/username`, { params });
  }

  checkKeycloakIdExists(keycloakId: string): Observable<{ exists: boolean }> {
    const params = new HttpParams().set('keycloakId', keycloakId);
    return this.http.get<{ exists: boolean }>(`${this.apiUrl}/exists/keycloak`, { params });
  }

  // User Profile Management
  createUserProfile(userId: number, request: CreateUserProfileRequest): Observable<User> {
    return this.http.post<User>(`${this.apiUrl}/${userId}/profile`, request).pipe(
      tap(user => {
        if (this.currentUserSubject.value?.id === userId) {
          this.setCurrentUser(user);
        }
      })
    );
  }

  updateUserProfile(userId: number, request: UpdateUserProfileRequest): Observable<User> {
    return this.http.put<User>(`${this.apiUrl}/${userId}/profile`, request).pipe(
      tap(user => {
        if (this.currentUserSubject.value?.id === userId) {
          this.setCurrentUser(user);
        }
      })
    );
  }

  // User Address Management
  addUserAddress(userId: number, request: CreateUserAddressRequest): Observable<User> {
    return this.http.post<User>(`${this.apiUrl}/${userId}/addresses`, request).pipe(
      tap(user => {
        if (this.currentUserSubject.value?.id === userId) {
          this.setCurrentUser(user);
        }
      })
    );
  }

  updateUserAddress(userId: number, addressId: number, request: UpdateUserAddressRequest): Observable<User> {
    return this.http.put<User>(`${this.apiUrl}/${userId}/addresses/${addressId}`, request).pipe(
      tap(user => {
        if (this.currentUserSubject.value?.id === userId) {
          this.setCurrentUser(user);
        }
      })
    );
  }

  deleteUserAddress(userId: number, addressId: number): Observable<User> {
    return this.http.delete<User>(`${this.apiUrl}/${userId}/addresses/${addressId}`).pipe(
      tap(user => {
        if (this.currentUserSubject.value?.id === userId) {
          this.setCurrentUser(user);
        }
      })
    );
  }

  setDefaultAddress(userId: number, addressId: number): Observable<User> {
    return this.http.put<User>(`${this.apiUrl}/${userId}/addresses/${addressId}/default`, {}).pipe(
      tap(user => {
        if (this.currentUserSubject.value?.id === userId) {
          this.setCurrentUser(user);
        }
      })
    );
  }

  // Service health check
  checkServiceHealth(): Observable<any> {
    return this.http.get(`${this.apiUrl}/health`);
  }

  // Helper methods
  getDisplayName(user: User): string {
    if (user.profile?.fullName) {
      return user.profile.fullName;
    }
    if (user.profile?.firstName && user.profile?.lastName) {
      return `${user.profile.firstName} ${user.profile.lastName}`;
    }
    if (user.profile?.firstName) {
      return user.profile.firstName;
    }
    return user.username || user.email;
  }

  getInitials(user: User): string {
    if (user.profile?.initials) {
      return user.profile.initials;
    }
    if (user.profile?.firstName && user.profile?.lastName) {
      return `${user.profile.firstName.charAt(0)}${user.profile.lastName.charAt(0)}`.toUpperCase();
    }
    if (user.profile?.firstName) {
      return user.profile.firstName.charAt(0).toUpperCase();
    }
    return user.username?.charAt(0).toUpperCase() || user.email.charAt(0).toUpperCase();
  }

  formatAddress(address: UserAddress): string {
    if (address.formattedAddress) {
      return address.formattedAddress;
    }
    return `${address.street}, ${address.city}, ${address.state} ${address.zipCode}, ${address.country}`;
  }

  getShortAddress(address: UserAddress): string {
    if (address.shortAddress) {
      return address.shortAddress;
    }
    return `${address.city}, ${address.state}`;
  }
}