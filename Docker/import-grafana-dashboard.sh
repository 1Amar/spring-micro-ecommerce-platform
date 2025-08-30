#!/bin/bash

# Script to import the updated Grafana dashboard
# Make sure Grafana is running and accessible

GRAFANA_URL="http://localhost:3000"
GRAFANA_USER="admin" 
GRAFANA_PASS="admin"
DASHBOARD_FILE="grafana-dashboard-microservices.json"

echo "🎯 Importing Grafana Dashboard for Microservices..."
echo "📊 Dashboard: $DASHBOARD_FILE"
echo "🔗 Grafana URL: $GRAFANA_URL"

# Check if Grafana is accessible
if ! curl -s -f "$GRAFANA_URL/api/health" > /dev/null; then
    echo "❌ Error: Cannot connect to Grafana at $GRAFANA_URL"
    echo "   Make sure Grafana is running in Docker"
    exit 1
fi

# Import the dashboard
response=$(curl -s -X POST \
    -H "Content-Type: application/json" \
    -u "$GRAFANA_USER:$GRAFANA_PASS" \
    -d @"$DASHBOARD_FILE" \
    "$GRAFANA_URL/api/dashboards/db")

# Check response
if echo "$response" | grep -q '"status":"success"'; then
    echo "✅ Dashboard imported successfully!"
    echo "🌐 Access it at: $GRAFANA_URL/d/microservices-overview"
    echo ""
    echo "📋 New Features in Updated Dashboard:"
    echo "   • Cart Service Operations monitoring"
    echo "   • User Service Operations tracking"
    echo "   • Product Service Performance metrics"
    echo "   • Updated service health checks for all active services"
    echo ""
else
    echo "❌ Error importing dashboard:"
    echo "$response"
    exit 1
fi

echo "🎉 Dashboard setup complete!"
echo "💡 Tip: The dashboard will show data once services start receiving requests"