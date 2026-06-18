#!/bin/bash

curl -X POST "http://localhost:8080/api/v1/orders" \
  -H "Content-Type: application/json" \
  -d '{"customerEmail":"test@example.com","items":[{"productId":1,"quantity":1}],"description":"Test order"}'