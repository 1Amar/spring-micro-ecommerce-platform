# E-Commerce Frontend Application

A modern, responsive Angular frontend application for the Spring Boot microservices e-commerce platform.

## Features

### ✅ Implemented
- **Modern Angular Architecture** - Angular 17 with TypeScript
- **Keycloak Authentication** - Secure user authentication and authorization
- **Responsive Design** - Bootstrap 5 and Angular Material components
- **HTTP Interceptors** - Authentication, loading states, and error handling
- **Service Layer** - Complete API integration for all microservices
- **State Management** - Reactive services with observables
- **Type Safety** - Comprehensive TypeScript models and interfaces

### 🚧 Under Development
- Product catalog with search and filtering
- Shopping cart management
- Checkout process with payment integration
- User profile and order history
- Advanced search functionality
- Real-time notifications

## Tech Stack

- **Framework**: Angular 17
- **UI Library**: Angular Material + Bootstrap 5
- **Authentication**: Keycloak Angular
- **State Management**: RxJS Observables
- **HTTP Client**: Angular HTTP Client
- **Styling**: SCSS with CSS Custom Properties
- **Icons**: Material Icons + Font Awesome
- **Build Tool**: Angular CLI

## Project Structure

```
src/
├── app/
│   ├── core/                 # Core services and guards
│   │   ├── auth/            # Authentication logic
│   │   ├── services/        # API services
│   │   └── interceptors/    # HTTP interceptors
│   ├── shared/              # Shared components and models
│   │   ├── components/      # Reusable components
│   │   └── models/          # TypeScript interfaces
│   ├── features/            # Feature modules
│   │   ├── home/           # Home page
│   │   ├── products/       # Product catalog
│   │   ├── cart/           # Shopping cart
│   │   ├── checkout/       # Checkout process
│   │   ├── profile/        # User profile
│   │   └── orders/         # Order history
│   └── app.module.ts       # Root module
├── environments/           # Environment configurations
└── assets/                # Static assets
```

## Getting Started

### Prerequisites
- Node.js 18+ 
- npm or yarn
- Angular CLI 17+

### Installation

1. **Install dependencies**:
   ```bash
   cd ecommerce-frontend
   npm install
   ```

2. **Update environment configuration**:
   Edit `src/environments/environment.ts` to match your backend URLs:
   ```typescript
   export const environment = {
     production: false,
     apiUrl: 'http://localhost:8081/api/v1', // API Gateway URL
     keycloak: {
       url: 'http://localhost:8080',
       realm: 'ecommerce-realm',
       clientId: 'ecommerce-frontend'
     }
   };
   ```

3. **Start the development server**:
   ```bash
   npm start
   # or
   ng serve
   ```

4. **Open your browser**:
   Navigate to `http://localhost:4200`

### Build for Production

```bash
npm run build
# or
ng build --prod
```

## Environment Configuration

### Development Environment
- API Gateway: `http://localhost:8081`
- Keycloak: `http://localhost:8080`
- Local development with hot reload

### Production Environment
- Configure production URLs in `environment.prod.ts`
- Enable production optimizations
- Disable console logging

## Authentication Flow

1. **Check SSO**: Application checks for existing Keycloak session
2. **Login Redirect**: Unauthenticated users are redirected to Keycloak
3. **Token Management**: JWT tokens are automatically refreshed
4. **Route Guards**: Protected routes require authentication
5. **Logout**: Clean session termination

## API Integration

### Services Architecture
- **ProductService**: Product catalog and search
- **CartService**: Shopping cart management
- **OrderService**: Order creation and tracking  
- **AuthService**: Authentication state management

### HTTP Interceptors
- **AuthInterceptor**: Adds JWT tokens to requests
- **LoadingInterceptor**: Global loading state management
- **ErrorInterceptor**: Centralized error handling

## Responsive Design

### Breakpoints
- **Mobile**: < 576px
- **Tablet**: 576px - 768px  
- **Desktop**: 768px - 992px
- **Large Desktop**: > 992px

### Components
- Mobile-first responsive design
- Touch-friendly interfaces
- Adaptive navigation menus
- Optimized layouts for all screen sizes

## Development Guidelines

### Code Style
- Follow Angular style guide
- Use TypeScript strict mode
- Implement reactive patterns with RxJS
- Maintain component separation of concerns

### Component Structure
```typescript
@Component({
  selector: 'app-example',
  templateUrl: './example.component.html',
  styleUrls: ['./example.component.scss']
})
export class ExampleComponent implements OnInit {
  // Component logic
}
```

### Service Patterns
```typescript
@Injectable({
  providedIn: 'root'
})
export class ExampleService {
  constructor(private http: HttpClient) {}
  
  getData(): Observable<Data[]> {
    return this.http.get<Data[]>(`${this.apiUrl}/data`);
  }
}
```

## Integration with Backend

### Microservices Integration
The frontend integrates with the following backend services:

1. **API Gateway** (8081): Single entry point for all API calls
2. **Product Service**: Product catalog management
3. **Inventory Service**: Stock availability
4. **Order Service**: Order processing
5. **Payment Service**: Payment processing
6. **Notification Service**: User notifications
7. **Search Service**: Advanced product search
8. **Catalog Service**: Category management

### Error Handling
- Global error interceptor
- User-friendly error messages
- Retry mechanisms for failed requests
- Fallback UI states

## Testing

### Unit Testing
```bash
npm test
# or  
ng test
```

### E2E Testing
```bash
npm run e2e
# or
ng e2e  
```

## Performance Optimization

### Implemented Optimizations
- Lazy loading for feature modules
- OnPush change detection strategy
- Image optimization and lazy loading
- Bundle size optimization
- Tree shaking for unused code

### Monitoring
- Performance metrics tracking
- Error logging and reporting
- User interaction analytics

## Deployment

### Docker Deployment
```dockerfile
FROM node:18-alpine as build
WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist/ecommerce-frontend /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

### Kubernetes Deployment
- Horizontal pod autoscaling
- Health checks and readiness probes
- Configuration via ConfigMaps
- TLS termination at ingress

## Contributing

1. Fork the repository
2. Create a feature branch
3. Implement your changes
4. Add tests for new functionality
5. Submit a pull request

## Troubleshooting

### Common Issues

**CORS Errors**: Ensure backend CORS configuration allows frontend origin
**Authentication Failures**: Verify Keycloak realm and client configuration  
**Module Loading Errors**: Check that all dependencies are installed
**Build Failures**: Verify Node.js and Angular CLI versions

### Support
- Check the backend service logs
- Verify API Gateway connectivity
- Test authentication flow in Keycloak admin console
- Review browser developer console for errors

---

Built with ❤️ using Angular and Spring Boot microservices