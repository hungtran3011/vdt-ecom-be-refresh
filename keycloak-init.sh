#!/bin/bash

# Keycloak initialization script
# This script ensures the realm is properly imported and available

set -e

echo "Starting Keycloak initialization..."

# Wait for Keycloak admin interface to be ready
echo "Waiting for Keycloak to be ready..."
timeout=300
count=0
while ! curl -sf http://keycloak:8080/admin/ > /dev/null 2>&1; do
  if [ $count -ge $timeout ]; then
    echo "❌ Timeout waiting for Keycloak admin interface"
    exit 1
  fi
  echo "⏳ Keycloak admin interface not ready yet... ($count/$timeout)"
  sleep 2
  count=$((count + 2))
done

echo "✅ Keycloak admin interface is ready"

# Check if realm exists
echo "Checking if 'ecom' realm exists..."
if curl -sf -H "Content-Type: application/json" \
   -u "${KC_BOOTSTRAP_ADMIN_USERNAME}:${KC_BOOTSTRAP_ADMIN_PASSWORD}" \
   "http://keycloak:8080/admin/realms/ecom" > /dev/null 2>&1; then
  echo "✅ Realm 'ecom' already exists"
else
  echo "⚠️ Realm 'ecom' does not exist, attempting to import..."
  
  # Get admin access token
  echo "Getting admin access token..."
  ADMIN_TOKEN=$(curl -sf -X POST \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "username=${KC_BOOTSTRAP_ADMIN_USERNAME}" \
    -d "password=${KC_BOOTSTRAP_ADMIN_PASSWORD}" \
    -d "grant_type=password" \
    -d "client_id=admin-cli" \
    "http://keycloak:8080/realms/master/protocol/openid-connect/token" | \
    jq -r '.access_token')
  
  if [ "$ADMIN_TOKEN" = "null" ] || [ -z "$ADMIN_TOKEN" ]; then
    echo "❌ Failed to get admin access token"
    exit 1
  fi
  
  echo "✅ Got admin access token"
  
  # Import realm
  echo "Importing realm from /opt/keycloak/data/import/ecom-realm.json..."
  if curl -sf -X POST \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H "Content-Type: application/json" \
    -d @/opt/keycloak/data/import/ecom-realm.json \
    "http://keycloak:8080/admin/realms"; then
    echo "✅ Realm imported successfully"
  else
    echo "❌ Failed to import realm"
    exit 1
  fi
fi

# Verify realm is accessible
echo "Verifying realm endpoints are accessible..."
if curl -sf "http://keycloak:8080/realms/ecom/.well-known/openid_configuration" > /dev/null 2>&1; then
  echo "✅ Realm 'ecom' is accessible and configured properly"
else
  echo "❌ Realm 'ecom' is not accessible"
  exit 1
fi

echo "🎉 Keycloak initialization completed successfully!"
