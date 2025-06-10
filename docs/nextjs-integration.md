# Next.js Frontend Integration Guide

## Table of Contents
1. [Overview](#overview)
2. [Authentication Setup](#authentication-setup)
3. [API Configuration](#api-configuration)
4. [Core API Integration](#core-api-integration)
5. [Component Examples](#component-examples)
6. [State Management](#state-management)
7. [Error Handling](#error-handling)
8. [Security Best Practices](#security-best-practices)
9. [Performance Optimization](#performance-optimization)
10. [Testing](#testing)

## Overview

This guide provides comprehensive instructions for integrating a Next.js frontend with the VDT E-Commerce Spring Boot backend. The backend uses **Keycloak PKCE authentication**, PostgreSQL, Redis caching, and provides RESTful APIs for e-commerce operations.

### Backend Architecture
- **Authentication**: Keycloak with PKCE flow
- **Database**: PostgreSQL with JPA/Hibernate
- **Caching**: Redis
- **Payment**: Viettel Payment Gateway integration
- **Email**: SMTP notification system
- **API Base URL**: `http://localhost:8888/api`

## Authentication Setup

### 1. Install Keycloak Packages

```bash
npm install @keycloak/keycloak-js @auth0/nextjs-auth0
# OR using alternative Keycloak client
npm install keycloak-js
```

### 2. Environment Configuration

Create a `.env.local` file:

```env
# Keycloak Configuration
NEXT_PUBLIC_KEYCLOAK_URL=http://localhost:8080
NEXT_PUBLIC_KEYCLOAK_REALM=your-realm
NEXT_PUBLIC_KEYCLOAK_CLIENT_ID=your-frontend-client
KEYCLOAK_CLIENT_SECRET=your-client-secret

# API Configuration
NEXT_PUBLIC_API_BASE_URL=http://localhost:8888/api
NEXT_PUBLIC_API_TIMEOUT=10000

# App Configuration
NEXTAUTH_URL=http://localhost:3000
NEXTAUTH_SECRET=your-nextauth-secret
```

### 3. Keycloak Provider Setup

Create `lib/keycloak.ts`:

```typescript
import Keycloak from 'keycloak-js';

const keycloakConfig = {
  url: process.env.NEXT_PUBLIC_KEYCLOAK_URL!,
  realm: process.env.NEXT_PUBLIC_KEYCLOAK_REALM!,
  clientId: process.env.NEXT_PUBLIC_KEYCLOAK_CLIENT_ID!,
};

const keycloak = new Keycloak(keycloakConfig);

export const initKeycloak = async (): Promise<Keycloak> => {
  try {
    const authenticated = await keycloak.init({
      onLoad: 'check-sso',
      silentCheckSsoRedirectUri: window.location.origin + '/silent-check-sso.html',
      pkceMethod: 'S256', // Enable PKCE
      checkLoginIframe: false,
    });

    if (authenticated) {
      console.log('User is authenticated');
    } else {
      console.log('User is not authenticated');
    }

    // Token refresh setup
    keycloak.onTokenExpired = () => {
      keycloak.updateToken(5)
        .then((refreshed) => {
          if (refreshed) {
            console.log('Token refreshed');
          } else {
            console.warn('Token not refreshed, valid for:', 
              Math.round(keycloak.tokenParsed?.exp! + keycloak.timeSkew - new Date().getTime() / 1000) + ' seconds');
          }
        })
        .catch(() => {
          console.error('Failed to refresh token');
          keycloak.login();
        });
    };

    return keycloak;
  } catch (error) {
    console.error('Keycloak initialization failed:', error);
    throw error;
  }
};

export default keycloak;
```

### 4. Auth Context Provider

Create `contexts/AuthContext.tsx`:

```typescript
'use client';
import React, { createContext, useContext, useEffect, useState, ReactNode } from 'react';
import Keycloak from 'keycloak-js';
import { initKeycloak } from '@/lib/keycloak';

interface AuthContextType {
  keycloak: Keycloak | null;
  authenticated: boolean;
  loading: boolean;
  user: UserProfile | null;
  login: () => void;
  logout: () => void;
  token: string | null;
}

interface UserProfile {
  userId: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  roles: string[];
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [keycloak, setKeycloak] = useState<Keycloak | null>(null);
  const [authenticated, setAuthenticated] = useState(false);
  const [loading, setLoading] = useState(true);
  const [user, setUser] = useState<UserProfile | null>(null);

  useEffect(() => {
    const initAuth = async () => {
      try {
        const kc = await initKeycloak();
        setKeycloak(kc);
        setAuthenticated(kc.authenticated || false);

        if (kc.authenticated && kc.tokenParsed) {
          const userProfile: UserProfile = {
            userId: kc.tokenParsed.sub!,
            username: kc.tokenParsed.preferred_username!,
            email: kc.tokenParsed.email!,
            firstName: kc.tokenParsed.given_name!,
            lastName: kc.tokenParsed.family_name!,
            roles: kc.tokenParsed.realm_access?.roles || [],
          };
          setUser(userProfile);
        }
      } catch (error) {
        console.error('Auth initialization failed:', error);
      } finally {
        setLoading(false);
      }
    };

    initAuth();
  }, []);

  const login = () => {
    keycloak?.login();
  };

  const logout = () => {
    keycloak?.logout();
  };

  const token = keycloak?.token || null;

  return (
    <AuthContext.Provider value={{
      keycloak,
      authenticated,
      loading,
      user,
      login,
      logout,
      token,
    }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
```

## API Configuration

### 1. API Client Setup

Create `lib/api.ts`:

```typescript
import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios';

interface ApiConfig {
  baseURL: string;
  timeout: number;
}

class ApiClient {
  private client: AxiosInstance;
  private token: string | null = null;

  constructor(config: ApiConfig) {
    this.client = axios.create({
      baseURL: config.baseURL,
      timeout: config.timeout,
      headers: {
        'Content-Type': 'application/json',
      },
    });

    this.setupInterceptors();
  }

  private setupInterceptors() {
    // Request interceptor
    this.client.interceptors.request.use(
      (config) => {
        if (this.token) {
          config.headers.Authorization = `Bearer ${this.token}`;
        }
        return config;
      },
      (error) => Promise.reject(error)
    );

    // Response interceptor
    this.client.interceptors.response.use(
      (response) => response,
      (error) => {
        if (error.response?.status === 401) {
          // Handle unauthorized access
          window.location.href = '/login';
        }
        return Promise.reject(error);
      }
    );
  }

  setToken(token: string | null) {
    this.token = token;
  }

  async get<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
    const response: AxiosResponse<T> = await this.client.get(url, config);
    return response.data;
  }

  async post<T>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    const response: AxiosResponse<T> = await this.client.post(url, data, config);
    return response.data;
  }

  async put<T>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    const response: AxiosResponse<T> = await this.client.put(url, data, config);
    return response.data;
  }

  async delete<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
    const response: AxiosResponse<T> = await this.client.delete(url, config);
    return response.data;
  }
}

// Create API client instance
const apiClient = new ApiClient({
  baseURL: process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8888/api',
  timeout: parseInt(process.env.NEXT_PUBLIC_API_TIMEOUT || '10000'),
});

export default apiClient;
```

### 2. Type Definitions

Create `types/api.ts`:

```typescript
// User and Authentication Types
export interface UserProfile {
  userId: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  roles: string[];
}

export interface ProfileDto {
  userId: string;
  email: string;
  firstName: string;
  lastName: string;
  phoneNumber?: string;
  address?: string;
  dateOfBirth?: string;
  gender?: string;
}

// Product Types
export interface ProductDto {
  id: number;
  name: string;
  description: string;
  basePrice: number;
  images: string[];
  category: CategoryDto;
  categoryId: number;
  dynamicValues: ProductDynamicValueDto[];
  variations: VariationDto[];
}

export interface ProductDynamicValueDto {
  id: number;
  field: CategoryDynamicFieldDto;
  value: string;
}

export interface VariationDto {
  id: number;
  name: string;
  type: string;
  additionalPrice: number;
  dynamicValues: VariationDynamicValueDto[];
}

export interface VariationDynamicValueDto {
  id: number;
  field: CategoryDynamicFieldDto;
  value: string;
}

// Category Types
export interface CategoryDto {
  id: number;
  name: string;
  imageUrl?: string;
  dynamicFields: CategoryDynamicFieldDto[];
}

export interface CategoryDynamicFieldDto {
  id: number;
  fieldName: string;
  fieldType: FieldType;
  appliesTo: AppliesTo;
  categoryId: number;
}

export enum FieldType {
  TEXT = 'TEXT',
  NUMBER = 'NUMBER',
  BOOLEAN = 'BOOLEAN',
  DATE = 'DATE',
  SELECT = 'SELECT'
}

export enum AppliesTo {
  PRODUCT = 'PRODUCT',
  VARIATION = 'VARIATION',
  BOTH = 'BOTH'
}

// Order Types
export interface OrderDto {
  id: string;
  userId: string;
  status: OrderStatus;
  address: string;
  phone: string;
  note?: string;
  paymentMethod: PaymentMethod;
  paymentStatus: PaymentStatus;
  paymentId?: string;
  totalPrice: number;
  createdAt: string;
  updatedAt: string;
  items: OrderItemDto[];
}

export interface OrderItemDto {
  id: number;
  orderId: string;
  productId: number;
  productName: string;
  productImage: string;
  quantity: number;
  price: number;
  totalPrice: number;
}

export enum OrderStatus {
  PENDING = 'PENDING',
  CONFIRMED = 'CONFIRMED',
  PROCESSING = 'PROCESSING',
  SHIPPED = 'SHIPPED',
  DELIVERED = 'DELIVERED',
  CANCELLED = 'CANCELLED'
}

export enum PaymentMethod {
  CASH = 'CASH',
  CREDIT_CARD = 'CREDIT_CARD',
  BANK_TRANSFER = 'BANK_TRANSFER',
  VIETTEL_MONEY = 'VIETTEL_MONEY'
}

export enum PaymentStatus {
  PENDING = 'PENDING',
  PAID = 'PAID',
  FAILED = 'FAILED',
  REFUNDED = 'REFUNDED'
}

// Cart Types
export interface CartDto {
  id: number;
  userId: number;
  items: CartItemDto[];
  totalPrice: number;
  lastUpdated: string;
}

export interface CartItemDto {
  id: number;
  cartId: number;
  productId: number;
  productName: string;
  productImage: string;
  quantity: number;
  unitPrice: number;
  stockSku: string;
  selectedVariations: VariationDto[];
}

// Stock Types
export interface StockDto {
  id: number;
  productId: number;
  sku: string;
  quantity: number;
  reservedQuantity: number;
  availableQuantity: number;
  variations: VariationDto[];
}

// Payment Types
export interface ViettelPaymentRequest {
  orderId: string;
  amount: number;
  description: string;
  returnUrl: string;
  customerInfo: CustomerInfo;
}

export interface CustomerInfo {
  customerName: string;
  customerPhone: string;
  customerEmail: string;
  customerAddress: string;
}

// API Response Types
export interface ApiResponse<T> {
  data: T;
  message?: string;
  status: number;
}

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

// Error Types
export interface ApiError {
  code: string;
  message: string;
  details?: any;
  timestamp: string;
}
```

## Core API Integration

### 1. Authentication Service

Create `services/authService.ts`:

```typescript
import apiClient from '@/lib/api';
import { UserProfile, ProfileDto } from '@/types/api';

export class AuthService {
  async getCurrentUser(token: string): Promise<UserProfile> {
    apiClient.setToken(token);
    return apiClient.get<UserProfile>('/v1/auth/me');
  }

  async syncProfile(token: string): Promise<ProfileDto> {
    apiClient.setToken(token);
    return apiClient.post<ProfileDto>('/v1/profiles/me/sync');
  }

  async getProfile(userId: string, token: string): Promise<ProfileDto> {
    apiClient.setToken(token);
    return apiClient.get<ProfileDto>(`/v1/profiles/${userId}`);
  }

  async updateProfile(profile: ProfileDto, token: string): Promise<ProfileDto> {
    apiClient.setToken(token);
    return apiClient.post<ProfileDto>('/v1/profiles', profile);
  }

  async healthCheck(): Promise<{ status: string; service: string; timestamp: string }> {
    return apiClient.get('/v1/auth/health');
  }
}

export const authService = new AuthService();
```

### 2. Product Service

Create `services/productService.ts`:

```typescript
import apiClient from '@/lib/api';
import { ProductDto } from '@/types/api';

export class ProductService {
  async getAllProducts(token?: string): Promise<ProductDto[]> {
    if (token) apiClient.setToken(token);
    return apiClient.get<ProductDto[]>('/v1/products');
  }

  async getProductById(id: number, token?: string): Promise<ProductDto> {
    if (token) apiClient.setToken(token);
    return apiClient.get<ProductDto>(`/v1/products/${id}`);
  }

  async getProductsByCategory(categoryId: number, token?: string): Promise<ProductDto[]> {
    if (token) apiClient.setToken(token);
    return apiClient.get<ProductDto[]>(`/v1/products/category/${categoryId}`);
  }

  async createProduct(product: Omit<ProductDto, 'id'>, token: string): Promise<ProductDto> {
    apiClient.setToken(token);
    return apiClient.post<ProductDto>('/v1/products', product);
  }

  async updateProduct(product: ProductDto, token: string): Promise<ProductDto> {
    apiClient.setToken(token);
    return apiClient.put<ProductDto>(`/v1/products/${product.id}`, product);
  }

  async deleteProduct(id: number, token: string): Promise<void> {
    apiClient.setToken(token);
    return apiClient.delete(`/v1/products/${id}`);
  }
}

export const productService = new ProductService();
```

### 3. Category Service

Create `services/categoryService.ts`:

```typescript
import apiClient from '@/lib/api';
import { CategoryDto } from '@/types/api';

export class CategoryService {
  async getAllCategories(token?: string): Promise<CategoryDto[]> {
    if (token) apiClient.setToken(token);
    return apiClient.get<CategoryDto[]>('/v1/categories');
  }

  async getCategoryById(id: number, token?: string): Promise<CategoryDto> {
    if (token) apiClient.setToken(token);
    return apiClient.get<CategoryDto>(`/v1/categories/${id}`);
  }

  async createCategory(category: Omit<CategoryDto, 'id'>, token: string): Promise<CategoryDto> {
    apiClient.setToken(token);
    return apiClient.post<CategoryDto>('/v1/categories', category);
  }

  async updateCategory(id: number, category: CategoryDto, token: string): Promise<CategoryDto> {
    apiClient.setToken(token);
    return apiClient.put<CategoryDto>(`/v1/categories/${id}`, category);
  }

  async deleteCategory(id: number, token: string): Promise<void> {
    apiClient.setToken(token);
    return apiClient.delete(`/v1/categories/${id}`);
  }
}

export const categoryService = new CategoryService();
```

### 4. Order Service

Create `services/orderService.ts`:

```typescript
import apiClient from '@/lib/api';
import { OrderDto } from '@/types/api';

export class OrderService {
  async createOrder(order: Omit<OrderDto, 'id' | 'createdAt' | 'updatedAt'>, token: string): Promise<OrderDto> {
    apiClient.setToken(token);
    return apiClient.post<OrderDto>('/v1/orders', order);
  }

  async getAllOrders(token: string): Promise<OrderDto[]> {
    apiClient.setToken(token);
    return apiClient.get<OrderDto[]>('/v1/orders');
  }

  async getOrderById(id: string, token: string): Promise<OrderDto> {
    apiClient.setToken(token);
    return apiClient.get<OrderDto>(`/v1/orders/${id}`);
  }

  async updateOrder(id: string, order: Partial<OrderDto>, token: string): Promise<OrderDto> {
    apiClient.setToken(token);
    return apiClient.put<OrderDto>(`/v1/orders/${id}`, order);
  }

  async deleteOrder(id: string, token: string): Promise<void> {
    apiClient.setToken(token);
    return apiClient.delete(`/v1/orders/${id}`);
  }
}

export const orderService = new OrderService();
```

### 5. Cart Service

Create `services/cartService.ts`:

```typescript
import apiClient from '@/lib/api';
import { CartDto } from '@/types/api';

export class CartService {
  async createCart(cart: Omit<CartDto, 'id'>, token: string): Promise<CartDto> {
    apiClient.setToken(token);
    return apiClient.post<CartDto>('/v1/cart', cart);
  }

  async getCart(id: number, token: string): Promise<CartDto> {
    apiClient.setToken(token);
    return apiClient.get<CartDto>(`/v1/cart/${id}`);
  }

  async updateCart(id: number, cart: Partial<CartDto>, token: string): Promise<CartDto> {
    apiClient.setToken(token);
    return apiClient.put<CartDto>(`/v1/cart/${id}`, cart);
  }

  async deleteCart(id: number, token: string): Promise<void> {
    apiClient.setToken(token);
    return apiClient.delete(`/v1/cart/${id}`);
  }
}

export const cartService = new CartService();
```

### 6. Payment Service

Create `services/paymentService.ts`:

```typescript
import apiClient from '@/lib/api';
import { ViettelPaymentRequest } from '@/types/api';

export class PaymentService {
  async createViettelPayment(paymentRequest: ViettelPaymentRequest, token: string): Promise<{ paymentUrl: string }> {
    apiClient.setToken(token);
    return apiClient.post<{ paymentUrl: string }>('/api/payment/viettel/create', paymentRequest);
  }

  async confirmViettelPayment(orderId: string, token: string): Promise<{ status: string; message: string }> {
    apiClient.setToken(token);
    return apiClient.post(`/api/payment/viettel/confirm/${orderId}`);
  }

  async getPaymentStatus(orderId: string, token: string): Promise<{ status: string; paymentId?: string }> {
    apiClient.setToken(token);
    return apiClient.get(`/api/payment/viettel/status/${orderId}`);
  }
}

export const paymentService = new PaymentService();
```

## Component Examples

### 1. Login Component

Create `components/auth/LoginButton.tsx`:

```typescript
'use client';
import { useAuth } from '@/contexts/AuthContext';
import { Button } from '@/components/ui/button';

export const LoginButton: React.FC = () => {
  const { authenticated, login, logout, user, loading } = useAuth();

  if (loading) {
    return <Button disabled>Loading...</Button>;
  }

  if (authenticated && user) {
    return (
      <div className="flex items-center gap-4">
        <span>Welcome, {user.firstName}!</span>
        <Button onClick={logout} variant="outline">
          Logout
        </Button>
      </div>
    );
  }

  return (
    <Button onClick={login}>
      Login
    </Button>
  );
};
```

### 2. Product List Component

Create `components/products/ProductList.tsx`:

```typescript
'use client';
import React, { useEffect, useState } from 'react';
import { ProductDto } from '@/types/api';
import { productService } from '@/services/productService';
import { useAuth } from '@/contexts/AuthContext';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import Image from 'next/image';

export const ProductList: React.FC = () => {
  const [products, setProducts] = useState<ProductDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const { token } = useAuth();

  useEffect(() => {
    const fetchProducts = async () => {
      try {
        setLoading(true);
        const data = await productService.getAllProducts(token || undefined);
        setProducts(data);
      } catch (err) {
        setError('Failed to fetch products');
        console.error('Error fetching products:', err);
      } finally {
        setLoading(false);
      }
    };

    fetchProducts();
  }, [token]);

  if (loading) {
    return <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
      {[...Array(6)].map((_, i) => (
        <Card key={i} className="animate-pulse">
          <div className="h-48 bg-gray-200"></div>
          <CardContent>
            <div className="h-4 bg-gray-200 mb-2"></div>
            <div className="h-4 bg-gray-200 w-3/4"></div>
          </CardContent>
        </Card>
      ))}
    </div>;
  }

  if (error) {
    return <div className="text-red-500 text-center p-4">{error}</div>;
  }

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      {products.map((product) => (
        <Card key={product.id} className="overflow-hidden hover:shadow-lg transition-shadow">
          <div className="relative h-48">
            {product.images && product.images.length > 0 ? (
              <Image
                src={product.images[0]}
                alt={product.name}
                fill
                className="object-cover"
              />
            ) : (
              <div className="w-full h-full bg-gray-200 flex items-center justify-center">
                No Image
              </div>
            )}
          </div>
          <CardHeader>
            <CardTitle className="line-clamp-2">{product.name}</CardTitle>
            <p className="text-sm text-gray-600">{product.category.name}</p>
          </CardHeader>
          <CardContent>
            <p className="text-sm text-gray-700 line-clamp-3 mb-4">
              {product.description}
            </p>
            <div className="flex justify-between items-center">
              <span className="text-2xl font-bold text-green-600">
                ${product.basePrice.toFixed(2)}
              </span>
              <Button>Add to Cart</Button>
            </div>
          </CardContent>
        </Card>
      ))}
    </div>
  );
};
```

### 3. User Profile Component

Create `components/profile/UserProfile.tsx`:

```typescript
'use client';
import React, { useEffect, useState } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import { authService } from '@/services/authService';
import { ProfileDto } from '@/types/api';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';

export const UserProfile: React.FC = () => {
  const [profile, setProfile] = useState<ProfileDto | null>(null);
  const [editing, setEditing] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const { user, token, authenticated } = useAuth();

  useEffect(() => {
    if (authenticated && token && user) {
      fetchProfile();
    }
  }, [authenticated, token, user]);

  const fetchProfile = async () => {
    if (!token || !user) return;
    
    try {
      setLoading(true);
      const profileData = await authService.getProfile(user.userId, token);
      setProfile(profileData);
    } catch (error) {
      console.error('Error fetching profile:', error);
      // Try to sync profile if not found
      try {
        const syncedProfile = await authService.syncProfile(token);
        setProfile(syncedProfile);
      } catch (syncError) {
        console.error('Error syncing profile:', syncError);
      }
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async () => {
    if (!profile || !token) return;

    try {
      setSaving(true);
      const updatedProfile = await authService.updateProfile(profile, token);
      setProfile(updatedProfile);
      setEditing(false);
    } catch (error) {
      console.error('Error updating profile:', error);
    } finally {
      setSaving(false);
    }
  };

  const handleInputChange = (field: keyof ProfileDto, value: string) => {
    if (profile) {
      setProfile({ ...profile, [field]: value });
    }
  };

  if (!authenticated) {
    return <div>Please log in to view your profile.</div>;
  }

  if (loading) {
    return <div>Loading profile...</div>;
  }

  if (!profile) {
    return <div>Profile not found.</div>;
  }

  return (
    <Card className="max-w-2xl mx-auto">
      <CardHeader>
        <CardTitle>User Profile</CardTitle>
        <div className="flex justify-end">
          {editing ? (
            <div className="space-x-2">
              <Button onClick={handleSave} disabled={saving}>
                {saving ? 'Saving...' : 'Save'}
              </Button>
              <Button variant="outline" onClick={() => setEditing(false)}>
                Cancel
              </Button>
            </div>
          ) : (
            <Button onClick={() => setEditing(true)}>Edit</Button>
          )}
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="grid grid-cols-2 gap-4">
          <div>
            <Label htmlFor="firstName">First Name</Label>
            <Input
              id="firstName"
              value={profile.firstName}
              onChange={(e) => handleInputChange('firstName', e.target.value)}
              disabled={!editing}
            />
          </div>
          <div>
            <Label htmlFor="lastName">Last Name</Label>
            <Input
              id="lastName"
              value={profile.lastName}
              onChange={(e) => handleInputChange('lastName', e.target.value)}
              disabled={!editing}
            />
          </div>
        </div>
        
        <div>
          <Label htmlFor="email">Email</Label>
          <Input
            id="email"
            type="email"
            value={profile.email}
            onChange={(e) => handleInputChange('email', e.target.value)}
            disabled={!editing}
          />
        </div>

        <div>
          <Label htmlFor="phoneNumber">Phone Number</Label>
          <Input
            id="phoneNumber"
            value={profile.phoneNumber || ''}
            onChange={(e) => handleInputChange('phoneNumber', e.target.value)}
            disabled={!editing}
          />
        </div>

        <div>
          <Label htmlFor="address">Address</Label>
          <Input
            id="address"
            value={profile.address || ''}
            onChange={(e) => handleInputChange('address', e.target.value)}
            disabled={!editing}
          />
        </div>

        <div className="grid grid-cols-2 gap-4">
          <div>
            <Label htmlFor="dateOfBirth">Date of Birth</Label>
            <Input
              id="dateOfBirth"
              type="date"
              value={profile.dateOfBirth || ''}
              onChange={(e) => handleInputChange('dateOfBirth', e.target.value)}
              disabled={!editing}
            />
          </div>
          <div>
            <Label htmlFor="gender">Gender</Label>
            <select
              id="gender"
              value={profile.gender || ''}
              onChange={(e) => handleInputChange('gender', e.target.value)}
              disabled={!editing}
              className="w-full p-2 border rounded"
            >
              <option value="">Select Gender</option>
              <option value="MALE">Male</option>
              <option value="FEMALE">Female</option>
              <option value="OTHER">Other</option>
            </select>
          </div>
        </div>
      </CardContent>
    </Card>
  );
};
```

## State Management

### 1. React Query Setup

Install React Query:

```bash
npm install @tanstack/react-query @tanstack/react-query-devtools
```

Create `lib/react-query.tsx`:

```typescript
'use client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';
import { useState } from 'react';

export function ReactQueryProvider({ children }: { children: React.ReactNode }) {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            staleTime: 5 * 60 * 1000, // 5 minutes
            retry: (failureCount, error: any) => {
              if (error?.response?.status === 404) return false;
              return failureCount < 2;
            },
          },
        },
      })
  );

  return (
    <QueryClientProvider client={queryClient}>
      {children}
      <ReactQueryDevtools initialIsOpen={false} />
    </QueryClientProvider>
  );
}
```

### 2. Custom Hooks with React Query

Create `hooks/useProducts.ts`:

```typescript
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { productService } from '@/services/productService';
import { useAuth } from '@/contexts/AuthContext';
import { ProductDto } from '@/types/api';

export const useProducts = () => {
  const { token } = useAuth();
  
  return useQuery({
    queryKey: ['products'],
    queryFn: () => productService.getAllProducts(token || undefined),
    enabled: true,
  });
};

export const useProduct = (id: number) => {
  const { token } = useAuth();
  
  return useQuery({
    queryKey: ['products', id],
    queryFn: () => productService.getProductById(id, token || undefined),
    enabled: !!id,
  });
};

export const useProductsByCategory = (categoryId: number) => {
  const { token } = useAuth();
  
  return useQuery({
    queryKey: ['products', 'category', categoryId],
    queryFn: () => productService.getProductsByCategory(categoryId, token || undefined),
    enabled: !!categoryId,
  });
};

export const useCreateProduct = () => {
  const queryClient = useQueryClient();
  const { token } = useAuth();

  return useMutation({
    mutationFn: (product: Omit<ProductDto, 'id'>) => 
      productService.createProduct(product, token!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['products'] });
    },
  });
};

export const useUpdateProduct = () => {
  const queryClient = useQueryClient();
  const { token } = useAuth();

  return useMutation({
    mutationFn: (product: ProductDto) => 
      productService.updateProduct(product, token!),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['products'] });
      queryClient.setQueryData(['products', data.id], data);
    },
  });
};

export const useDeleteProduct = () => {
  const queryClient = useQueryClient();
  const { token } = useAuth();

  return useMutation({
    mutationFn: (id: number) => productService.deleteProduct(id, token!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['products'] });
    },
  });
};
```

## Error Handling

### 1. Error Boundary Component

Create `components/ErrorBoundary.tsx`:

```typescript
'use client';
import React from 'react';
import { AlertCircle } from 'lucide-react';
import { Button } from '@/components/ui/button';

interface ErrorBoundaryState {
  hasError: boolean;
  error?: Error;
}

export class ErrorBoundary extends React.Component<
  React.PropsWithChildren<{}>,
  ErrorBoundaryState
> {
  constructor(props: React.PropsWithChildren<{}>) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
    console.error('Error caught by boundary:', error, errorInfo);
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="min-h-screen flex items-center justify-center bg-gray-50">
          <div className="max-w-md w-full bg-white shadow-lg rounded-lg p-6">
            <div className="flex items-center space-x-3 mb-4">
              <AlertCircle className="h-8 w-8 text-red-500" />
              <h1 className="text-xl font-semibold text-gray-900">
                Something went wrong
              </h1>
            </div>
            <p className="text-gray-600 mb-6">
              We encountered an unexpected error. Please try refreshing the page.
            </p>
            <div className="space-y-3">
              <Button 
                onClick={() => window.location.reload()} 
                className="w-full"
              >
                Refresh Page
              </Button>
              <Button 
                variant="outline" 
                onClick={() => this.setState({ hasError: false })}
                className="w-full"
              >
                Try Again
              </Button>
            </div>
            {process.env.NODE_ENV === 'development' && this.state.error && (
              <details className="mt-4">
                <summary className="cursor-pointer text-sm text-gray-500">
                  Error Details
                </summary>
                <pre className="mt-2 text-xs bg-gray-100 p-2 rounded overflow-auto">
                  {this.state.error.stack}
                </pre>
              </details>
            )}
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}
```

### 2. API Error Handling

Create `utils/errorHandling.ts`:

```typescript
import { ApiError } from '@/types/api';

export const handleApiError = (error: any): string => {
  if (error.response?.data) {
    const apiError: ApiError = error.response.data;
    return apiError.message || 'An unexpected error occurred';
  }
  
  if (error.message) {
    return error.message;
  }
  
  return 'Network error. Please check your connection.';
};

export const isAuthError = (error: any): boolean => {
  return error.response?.status === 401 || error.response?.status === 403;
};

export const logError = (error: any, context?: string) => {
  console.error(`Error${context ? ` in ${context}` : ''}:`, {
    message: error.message,
    status: error.response?.status,
    data: error.response?.data,
    stack: error.stack,
  });
};
```

## Security Best Practices

### 1. Environment Variables

```env
# Public variables (prefixed with NEXT_PUBLIC_)
NEXT_PUBLIC_KEYCLOAK_URL=http://localhost:8080
NEXT_PUBLIC_KEYCLOAK_REALM=your-realm
NEXT_PUBLIC_KEYCLOAK_CLIENT_ID=your-frontend-client
NEXT_PUBLIC_API_BASE_URL=http://localhost:8888/api

# Server-side only variables (no NEXT_PUBLIC_ prefix)
KEYCLOAK_CLIENT_SECRET=your-client-secret
NEXTAUTH_SECRET=your-nextauth-secret
DATABASE_URL=your-database-url
```

### 2. Token Security

Create `utils/tokenSecurity.ts`:

```typescript
export const isTokenExpired = (token: string): boolean => {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    const currentTime = Math.floor(Date.now() / 1000);
    return payload.exp < currentTime;
  } catch {
    return true;
  }
};

export const getTokenPayload = (token: string) => {
  try {
    return JSON.parse(atob(token.split('.')[1]));
  } catch {
    return null;
  }
};

export const hasRole = (token: string, role: string): boolean => {
  const payload = getTokenPayload(token);
  return payload?.realm_access?.roles?.includes(role) || false;
};
```

### 3. Route Protection

Create `middleware.ts` in the root directory:

```typescript
import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

const publicPaths = [
  '/',
  '/login',
  '/register',
  '/products',
  '/categories',
];

const adminPaths = [
  '/admin',
  '/dashboard',
];

export function middleware(request: NextRequest) {
  const pathname = request.nextUrl.pathname;
  const authToken = request.cookies.get('auth-token')?.value;

  // Allow public paths
  if (publicPaths.some(path => pathname.startsWith(path))) {
    return NextResponse.next();
  }

  // Check if user is authenticated
  if (!authToken) {
    return NextResponse.redirect(new URL('/login', request.url));
  }

  // Check admin routes
  if (adminPaths.some(path => pathname.startsWith(path))) {
    // Add admin role check here
    // This would require decoding the JWT token or making an API call
  }

  return NextResponse.next();
}

export const config = {
  matcher: [
    '/((?!api|_next/static|_next/image|favicon.ico).*)',
  ],
};
```

## Performance Optimization

### 1. Image Optimization

Create `next.config.js`:

```javascript
/** @type {import('next').NextConfig} */
const nextConfig = {
  images: {
    domains: ['localhost', 'your-api-domain.com'],
    formats: ['image/webp', 'image/avif'],
  },
  env: {
    CUSTOM_KEY: process.env.CUSTOM_KEY,
  },
};

module.exports = nextConfig;
```

### 2. Code Splitting and Lazy Loading

```typescript
// Lazy load heavy components
import dynamic from 'next/dynamic';

const AdminDashboard = dynamic(() => import('@/components/admin/Dashboard'), {
  loading: () => <div>Loading dashboard...</div>,
  ssr: false,
});

const ProductEditor = dynamic(() => import('@/components/products/ProductEditor'), {
  loading: () => <div>Loading editor...</div>,
});
```

### 3. Caching Strategy

```typescript
// API with SWR for real-time data
import useSWR from 'swr';

const fetcher = (url: string) => apiClient.get(url);

export const useRealtimeOrders = () => {
  return useSWR('/v1/orders', fetcher, {
    refreshInterval: 30000, // Refresh every 30 seconds
    revalidateOnFocus: true,
  });
};
```

## Testing

### 1. Jest Configuration

Create `jest.config.js`:

```javascript
const nextJest = require('next/jest');

const createJestConfig = nextJest({
  dir: './',
});

const customJestConfig = {
  setupFilesAfterEnv: ['<rootDir>/jest.setup.js'],
  moduleNameMapping: {
    '^@/(.*)$': '<rootDir>/$1',
  },
  testEnvironment: 'jest-environment-jsdom',
};

module.exports = createJestConfig(customJestConfig);
```

### 2. Service Tests

Create `__tests__/services/productService.test.ts`:

```typescript
import { productService } from '@/services/productService';
import apiClient from '@/lib/api';

// Mock the API client
jest.mock('@/lib/api');
const mockedApiClient = apiClient as jest.Mocked<typeof apiClient>;

describe('ProductService', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('getAllProducts', () => {
    it('should fetch all products successfully', async () => {
      const mockProducts = [
        { id: 1, name: 'Product 1', basePrice: 100 },
        { id: 2, name: 'Product 2', basePrice: 200 },
      ];

      mockedApiClient.get.mockResolvedValue(mockProducts);

      const result = await productService.getAllProducts();

      expect(mockedApiClient.get).toHaveBeenCalledWith('/v1/products');
      expect(result).toEqual(mockProducts);
    });

    it('should handle API errors', async () => {
      const errorMessage = 'API Error';
      mockedApiClient.get.mockRejectedValue(new Error(errorMessage));

      await expect(productService.getAllProducts()).rejects.toThrow(errorMessage);
    });
  });
});
```

### 3. Component Tests

Create `__tests__/components/ProductList.test.tsx`:

```typescript
import { render, screen, waitFor } from '@testing-library/react';
import { ProductList } from '@/components/products/ProductList';
import { useAuth } from '@/contexts/AuthContext';
import { productService } from '@/services/productService';

// Mock dependencies
jest.mock('@/contexts/AuthContext');
jest.mock('@/services/productService');

const mockedUseAuth = useAuth as jest.MockedFunction<typeof useAuth>;
const mockedProductService = productService as jest.Mocked<typeof productService>;

describe('ProductList', () => {
  beforeEach(() => {
    mockedUseAuth.mockReturnValue({
      authenticated: true,
      token: 'mock-token',
      user: null,
      loading: false,
      login: jest.fn(),
      logout: jest.fn(),
      keycloak: null,
    });
  });

  it('should render products when data is loaded', async () => {
    const mockProducts = [
      {
        id: 1,
        name: 'Test Product',
        description: 'Test Description',
        basePrice: 99.99,
        images: ['test-image.jpg'],
        category: { id: 1, name: 'Test Category', dynamicFields: [] },
        categoryId: 1,
        dynamicValues: [],
        variations: [],
      },
    ];

    mockedProductService.getAllProducts.mockResolvedValue(mockProducts);

    render(<ProductList />);

    await waitFor(() => {
      expect(screen.getByText('Test Product')).toBeInTheDocument();
      expect(screen.getByText('$99.99')).toBeInTheDocument();
    });
  });

  it('should show loading state initially', () => {
    mockedProductService.getAllProducts.mockImplementation(
      () => new Promise(resolve => setTimeout(resolve, 1000))
    );

    render(<ProductList />);

    expect(screen.getByTestId('product-list-loading')).toBeInTheDocument();
  });
});
```

## Deployment Checklist

### 1. Environment Setup

- [ ] Configure production Keycloak instance
- [ ] Set up production API endpoints
- [ ] Configure CORS settings on backend
- [ ] Set up SSL certificates
- [ ] Configure environment variables

### 2. Performance Optimization

- [ ] Enable gzip compression
- [ ] Configure CDN for static assets
- [ ] Set up caching headers
- [ ] Optimize bundle size
- [ ] Enable image optimization

### 3. Security

- [ ] Implement CSP headers
- [ ] Configure secure cookies
- [ ] Set up rate limiting
- [ ] Enable HTTPS only
- [ ] Validate all user inputs

### 4. Monitoring

- [ ] Set up error tracking (Sentry, etc.)
- [ ] Configure analytics
- [ ] Set up performance monitoring
- [ ] Create health check endpoints
- [ ] Set up logging

This comprehensive guide provides everything needed to integrate a Next.js frontend with your Spring Boot e-commerce backend. The implementation includes proper authentication, type safety, error handling, and follows modern React patterns with hooks and functional components.
