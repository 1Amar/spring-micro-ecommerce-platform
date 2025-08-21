export const environment = {
  production: true,
  apiUrl: 'https://api.ecommerce.com/api/v1', // Production API Gateway URL
  keycloak: {
    url: 'https://auth.ecommerce.com',
    realm: 'ecommerce-realm',
    clientId: 'ecommerce-frontend'
  },
  services: {
    productService: '/products',
    inventoryService: '/inventory',
    orderService: '/orders',
    paymentService: '/payments',
    notificationService: '/notifications',
    catalogService: '/catalog',
    searchService: '/search'
  },
  logging: {
    level: 'error',
    enableConsoleLogging: false
  }
};