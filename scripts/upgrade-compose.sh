#!/bin/bash

set -e

echo "=== Removing old docker-compose (V1) if installed via apt ==="
sudo apt-get remove -y docker-compose || true

echo "=== Installing Docker Compose V2 plugin ==="
sudo apt-get update

sudo apt-get install -y docker-compose-plugin

echo "=== Verifying installation ==="
docker compose version

echo "=== Done ==="