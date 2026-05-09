#!/bin/bash

set -e

echo "=============================="
echo "🧨 FULL RESET OF DOCKER COMPOSE"
echo "=============================="

echo "1. Removing Ubuntu Compose plugin"
sudo apt-get remove -y docker-compose-plugin || true

echo "2. Removing ALL local overrides"
sudo rm -f /usr/local/bin/docker-compose || true
sudo rm -f /usr/bin/docker-compose || true
sudo rm -rf ~/.docker/cli-plugins || true
sudo rm -rf /usr/lib/docker/cli-plugins/docker-compose || true

echo "3. Reinstall Docker Engine cleanly"
curl -fsSL https://get.docker.com | sudo sh

echo "4. Installing OFFICIAL Compose v2 binary (bypasses Ubuntu packages)"
mkdir -p ~/.docker/cli-plugins

curl -SL https://github.com/docker/compose/releases/download/v2.27.0/docker-compose-linux-x86_64 \
  -o ~/.docker/cli-plugins/docker-compose

chmod +x ~/.docker/cli-plugins/docker-compose

echo "5. Ensuring correct PATH priority"
export PATH=$HOME/.docker/cli-plugins:$PATH

echo "=============================="
echo "🔍 VERIFICATION"
echo "=============================="

which docker
docker --version

docker compose version

echo "=============================="
echo "✅ DONE - Compose should now be v2.x"
echo "=============================="