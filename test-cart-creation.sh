#!/bin/bash

# Test cart creation for e-commerce system
BASE_URL="http://localhost:8080"

echo "=== Testing Cart Creation ==="

# 1. Create a new cart for user ID 1
echo "1. Creating cart for user ID 1..."
curl -X POST "${BASE_URL}/v1/cart" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "items": [
      {
        "productId": 1,
        "productName": "Test Product",
        "selectedVariations": [],
        "quantity": 2,
        "unitPrice": 29.99,
        "subtotal": 59.98,
        "stockSku": "TEST-SKU-001"
      }
    ],
    "totalPrice": 59.98
  }' | jq '.'

echo -e "\n"

# 2. Get cart by user ID (this will create if not exists)
echo "2. Getting cart for user ID 1..."
curl -X GET "${BASE_URL}/v1/cart/user/1" \
  -H "Content-Type: application/json" | jq '.'

echo -e "\n"

# 3. Create an empty cart (just with user ID)
echo "3. Creating empty cart for user ID 2..."
curl -X POST "${BASE_URL}/v1/cart" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 2,
    "items": [],
    "totalPrice": 0.00
  }' | jq '.'

echo -e "\n=== Cart Creation Tests Complete ==="
