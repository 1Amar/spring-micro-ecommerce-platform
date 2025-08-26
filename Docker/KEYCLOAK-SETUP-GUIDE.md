# Keycloak Realm and Theme Setup Guide

## Overview
This guide covers setting up Keycloak with custom themes and realm configuration for production deployment.

## Pre-requisites
- Keycloak 22.x running in Docker
- Admin access to Keycloak (default: admin/admin)
- Custom theme files and realm export JSON

## Method 1: UI-Based Setup (Recommended for Production)

### Step 1: Setup Custom Theme
1. **Copy theme files to container:**
   ```bash
   docker cp keycloak-themes/ecommerce keycloak_ecom:/opt/keycloak/themes/
   ```

2. **Restart Keycloak to load themes:**
   ```bash
   docker restart keycloak_ecom
   ```

3. **Verify theme is loaded:**
   ```bash
   docker exec keycloak_ecom sh -c "ls /opt/keycloak/themes/"
   ```

### Step 2: Import Realm via Admin UI
1. **Access Admin Console:** http://localhost:8080
2. **Login:** admin/admin
3. **Import Realm:**
   - Click "Import" button (or select realm dropdown → "Add realm")
   - Upload `keycloak-realm-export.json`
   - Click "Create"

### Step 3: Configure Theme via Admin UI
1. **Navigate to Realm Settings → Themes**
2. **Set themes:**
   - Login Theme: `ecommerce`
   - Account Theme: `keycloak` (or custom if available)
   - Admin Theme: `keycloak`
   - Email Theme: `keycloak`
3. **Click "Save"**

### Step 4: Verification
1. **Test login page:** http://localhost:8080/realms/ecommerce-realm/account
2. **Verify custom styling is applied**
3. **Test user login with:**
   - admin@ecommerce.com / admin123
   - customer@ecommerce.com / customer123

## Method 2: REST API Setup (Automation)

### Step 1: Get Admin Token
```bash
TOKEN=$(curl -d "client_id=admin-cli" -d "username=admin" -d "password=admin" -d "grant_type=password" \
  "http://localhost:8080/realms/master/protocol/openid-connect/token" | \
  grep -o '"access_token":"[^"]*"' | cut -d'"' -f4)
```

### Step 2: Import Realm
```bash
curl -X POST \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d @keycloak-realm-export.json \
  "http://localhost:8080/admin/realms"
```

### Step 3: Update Realm Theme
```bash
curl -X PUT \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"loginTheme":"ecommerce"}' \
  "http://localhost:8080/admin/realms/ecommerce-realm"
```

## Method 3: Docker Volume Mount (Development)

### Step 1: Update docker-compose.yml
```yaml
keycloak:
  image: quay.io/keycloak/keycloak:22.0.1
  volumes:
    - ./keycloak-themes:/opt/keycloak/themes/custom
    - ./keycloak-realm-export.json:/opt/keycloak/data/import/realm.json
  environment:
    KC_IMPORT: /opt/keycloak/data/import/realm.json
```

### Step 2: Restart with Import
```bash
docker-compose down
docker-compose up -d
```

## Troubleshooting

### Theme Not Loading
1. **Check theme files exist:**
   ```bash
   docker exec keycloak_ecom ls -la /opt/keycloak/themes/ecommerce/
   ```

2. **Verify theme structure:**
   ```
   ecommerce/
   ├── login/
   │   ├── login.ftl
   │   ├── theme.properties
   │   └── resources/
   │       └── css/
   │           └── material-design.css
   ```

3. **Check theme.properties:**
   ```properties
   parent=base
   import=common/keycloak
   styles=css/material-design.css
   ```

4. **Restart Keycloak after theme changes:**
   ```bash
   docker restart keycloak_ecom
   ```

### Theme Selection Not Working via API
- **Issue:** API theme updates may not reflect immediately
- **Solution:** Use Admin UI for theme selection (Method 1)
- **Alternative:** Clear browser cache and restart Keycloak

### Realm Import Fails
1. **Check JSON syntax:**
   ```bash
   jq . keycloak-realm-export.json > /dev/null
   ```

2. **Verify realm doesn't exist:**
   ```bash
   curl "http://localhost:8080/realms/ecommerce-realm"
   ```

3. **Delete existing realm if needed:**
   ```bash
   curl -X DELETE \
     -H "Authorization: Bearer $TOKEN" \
     "http://localhost:8080/admin/realms/ecommerce-realm"
   ```

## Production Checklist

### Before Deployment:
- [ ] Custom theme files copied and verified
- [ ] Realm configuration imported
- [ ] Theme applied via Admin UI
- [ ] Test users can login
- [ ] Frontend redirects configured correctly
- [ ] SSL/HTTPS certificates configured
- [ ] Database persistence configured
- [ ] Backup strategy in place

### Post Deployment:
- [ ] Admin password changed
- [ ] Default users password changed
- [ ] Realm settings reviewed
- [ ] Client configurations verified
- [ ] Performance monitoring enabled

## Files Referenced
- `keycloak-themes/ecommerce/` - Custom Material Design theme
- `keycloak-realm-export.json` - Realm configuration with users, roles, clients
- `docker-compose.yml` - Container orchestration

## Support
- Keycloak Docs: https://www.keycloak.org/docs/
- Theme Development: https://www.keycloak.org/docs/latest/server_development/
- REST API: https://www.keycloak.org/docs-api/

---
**Note:** Always use Admin UI (Method 1) for theme configuration in production environments as it's the most reliable approach.