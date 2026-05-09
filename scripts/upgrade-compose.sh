#!/bin/bash

set -e

echo "=== Removing ALL docker-compose variants ==="

sudo rm -f /usr/local/bin/docker*
sudo rm -f /usr/bin/docker-compose
sudo rm -f ~/.docker/cli-plugins/docker-compose

echo "=== Removing aliases from shell configs ==="

sed -i '/docker-compose/d' ~/.bashrc || true
sed -i '/docker compose/d' ~/.bashrc || true

echo "=== Uninstalling snap docker if exists ==="
sudo snap remove docker 2>/dev/null || true

echo "=== Reinstalling official Docker clean ==="
curl -fsSL https://get.docker.com | sudo sh

echo "=== Installing Compose plugin clean ==="
sudo apt-get update
sudo apt-get install -y docker-compose-plugin || true

echo "=== Verification ==="
which docker
docker --version
docker compose version || true