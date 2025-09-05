export const environment = {
  production: false,
  apiUrl: 'http://localhost:8081/api/v1', // API Gateway URL
  keycloak: {
    url: 'http://localhost:8080',
    realm: 'ecommerce-realm',
    clientId: 'ecommerce-frontend'
  },
  services: {
    productService: '/products',
    userService: '/users',
    cartService: '/cart',
    inventoryService: '/inventory',
    orderService: '/order-management',
    paymentService: '/payments',
    notificationService: '/notifications',
    catalogService: '/catalog',
    searchService: '/search'
  },
  logging: {
    level: 'debug',
    enableConsoleLogging: true
  }
};