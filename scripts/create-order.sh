#!/bin/bash

URL="http://localhost:8080/api/v1/orders"

curl -X POST "$URL" \
  -H "Content-Type: application/json" \
  -d '{
    "customerEmail": "test@example.com",
    "amount": 120.00,
    "description": "Test order from script"
  }'
