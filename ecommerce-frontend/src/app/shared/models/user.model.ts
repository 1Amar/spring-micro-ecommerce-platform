export interface User {
  id: number;
  keycloakId?: string;
  email: string;
  username: string;
  isActive: boolean;
  createdAt: Date;
  updatedAt: Date;
  profile?: UserProfile;
  addresses?: UserAddress[];
  fullName?: string;
  defaultAddress?: UserAddress;
}

export interface UserProfile {
  id?: number;
  firstName?: string;
  lastName?: string;
  phoneNumber?: string;
  dateOfBirth?: Date;
  gender?: string;
  bio?: string;
  avatarUrl?: string;
  createdAt?: Date;
  updatedAt?: Date;
  fullName?: string;
  initials?: string;
}

export interface UserAddress {
  id?: number;
  type: 'HOME' | 'WORK' | 'OTHER';
  street: string;
  city: string;
  state: string;
  zipCode: string;
  country: string;
  isDefault?: boolean;
  createdAt?: Date;
  updatedAt?: Date;
  formattedAddress?: string;
  shortAddress?: string;
}

export interface CreateUserRequest {
  keycloakId?: string;
  email: string;
  username: string;
  isActive?: boolean;
}

export interface UpdateUserRequest {
  keycloakId?: string;
  email?: string;
  username?: string;
  isActive?: boolean;
}

export interface CreateUserProfileRequest {
  firstName?: string;
  lastName?: string;
  phoneNumber?: string;
  dateOfBirth?: Date;
  gender?: string;
  bio?: string;
  avatarUrl?: string;
}

export interface UpdateUserProfileRequest {
  firstName?: string;
  lastName?: string;
  phoneNumber?: string;
  dateOfBirth?: Date;
  gender?: string;
  bio?: string;
  avatarUrl?: string;
}

export interface CreateUserAddressRequest {
  type: 'HOME' | 'WORK' | 'OTHER';
  street: string;
  city: string;
  state: string;
  zipCode: string;
  country: string;
  isDefault?: boolean;
}

export interface UpdateUserAddressRequest {
  type?: 'HOME' | 'WORK' | 'OTHER';
  street?: string;
  city?: string;
  state?: string;
  zipCode?: string;
  country?: string;
  isDefault?: boolean;
}

export interface UserStats {
  totalUsers: number;
  activeUsers: number;
  inactiveUsers: number;
  usersWithProfiles: number;
  usersWithoutProfiles: number;
}

// Response interfaces
export interface PageResponse<T> {
  content: T[];
  pageable: {
    sort: {
      empty: boolean;
      sorted: boolean;
      unsorted: boolean;
    };
    offset: number;
    pageSize: number;
    pageNumber: number;
    paged: boolean;
    unpaged: boolean;
  };
  last: boolean;
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  sort: {
    empty: boolean;
    sorted: boolean;
    unsorted: boolean;
  };
  first: boolean;
  numberOfElements: number;
  empty: boolean;
}

export interface ApiResponse<T> {
  data: T;
  message?: string;
  success: boolean;
}