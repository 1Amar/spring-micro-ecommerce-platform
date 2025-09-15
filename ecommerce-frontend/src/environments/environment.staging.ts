export const environment = {
  production: false,
  apiUrl: 'https://staging-api.company.com/api/v1', // Staging API Gateway URL
  keycloak: {
    url: 'http://staging-keycloak:8080',
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
    level: 'info',
    enableConsoleLogging: true
  }
};