#!/bin/bash

echo "🎯 Importing Grafana Dashboard using Docker..."
echo

# Get the Grafana container name/ID
GRAFANA_CONTAINER=$(docker ps --filter "name=grafana" --format "{{.Names}}" | head -1)

if [ -z "$GRAFANA_CONTAINER" ]; then
    echo "❌ Error: Grafana container not found"
    echo "   Make sure Grafana is running with Docker"
    echo "   Expected container name containing 'grafana'"
    exit 1
fi

echo "📦 Found Grafana container: $GRAFANA_CONTAINER"

# Copy dashboard file to container
echo "📂 Copying dashboard file to container..."
docker cp "$(pwd)/grafana-dashboard-microservices.json" "$GRAFANA_CONTAINER:/tmp/dashboard.json"

if [ $? -ne 0 ]; then
    echo "❌ Error: Failed to copy dashboard file"
    exit 1
fi

# Wait a moment for file to be ready
sleep 2

# Import dashboard via API inside container
echo "📤 Importing dashboard via Grafana API..."
docker exec "$GRAFANA_CONTAINER" curl -X POST \
    -H "Content-Type: application/json" \
    -u "admin:admin" \
    -d @/tmp/dashboard.json \
    http://localhost:3000/api/dashboards/db

echo
echo
echo "✅ Dashboard import completed!"
echo "🌐 Access it at: http://localhost:3000/d/microservices-overview"
echo "🔑 Login: admin/admin"
echo
echo "📋 Updated Dashboard Features:"
echo "   • ✅ Cart Service Operations (add/remove/update cart items)"
echo "   • ✅ User Service Operations (profile management)"  
echo "   • ✅ Product Service Performance (1.4M+ products response times)"
echo "   • ✅ All Service Health Monitoring (cart, user, product, api-gateway)"
echo "   • ✅ Memory, CPU, and Error Rate tracking"
echo
echo "🎉 Grafana Dashboard is ready!"
echo "💡 Tip: Use your cart service to generate some metrics data"