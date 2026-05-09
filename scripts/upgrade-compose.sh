#!/bin/bash

set -e

echo "=============================="
echo "🧹 Cleaning broken Docker Compose installs"
echo "=============================="

sudo rm -f /usr/bin/docker-compose || true
sudo rm -f /usr/local/bin/docker-compose || true

sudo rm -f /usr/lib/docker/cli-plugins/docker-compose || true
sudo rm -f /usr/local/lib/docker/cli-plugins/docker-compose || true

echo "=============================="
echo "📦 Installing Docker Engine (official)"
echo "=============================="

curl -fsSL https://get.docker.com | sudo sh

echo "=============================="
echo "📦 Installing official Docker Compose plugin (v2)"
echo "=============================="

sudo apt-get update
sudo apt-get install -y docker-compose-plugin || true

# Fallback if apt plugin fails (common on broken VMs)
if ! docker compose version >/dev/null 2>&1; then
  echo "⚠️ apt plugin failed, installing manual Compose v2..."

  mkdir -p ~/.docker/cli-plugins

  curl -SL https://github.com/docker/compose/releases/download/v2.27.0/docker-compose-linux-x86_64 \
    -o ~/.docker/cli-plugins/docker-compose

  chmod +x ~/.docker/cli-plugins/docker-compose
fi

echo "=============================="
echo "🔍 Verification"
echo "=============================="

docker --version || true
docker compose version || true

echo "=============================="
echo "✅ DONE"
echo "=============================="