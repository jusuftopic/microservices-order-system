#!/bin/bash

curl --fail-with-body --silent --show-error \
  -X GET "http://localhost:8080/api/v1/investigations/orders/9001" \
  -H "Accept: application/json"
