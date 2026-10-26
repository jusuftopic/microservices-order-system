curl --request POST \
  http://localhost:8081/realms/order-system/protocol/openid-connect/token \
  --header 'Content-Type: application/x-www-form-urlencoded' \
  --data-urlencode 'grant_type=password' \
  --data-urlencode 'client_id=local-cli' \
  --data-urlencode 'username=alice' \
  --data-urlencode 'password=alice-local-only' \
  --data-urlencode 'scope=orders.read orders.write investigations.read investigations.ask investigations.feedback'