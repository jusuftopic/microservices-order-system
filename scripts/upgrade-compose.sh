#!/bin/bash

set -e

echo "=== Removing old Docker versions (if any) ==="
sudo apt-get remove -y docker docker-engine docker.io containerd runc || true

echo "=== Installing prerequisites ==="
sudo apt-get update
sudo apt-get install -y ca-certificates curl gnupg

echo "=== Adding Docker GPG key ==="
sudo install -m 0755 -d /etc/apt/keyrings

curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg

sudo chmod a+r /etc/apt/keyrings/docker.gpg

echo "=== Adding Docker repository ==="
echo \
"deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
$(. /etc/os-release && echo $VERSION_CODENAME) stable" | \
sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

echo "=== Installing Docker Engine + Compose V2 ==="
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

echo "=== Enabling Docker service ==="
sudo systemctl enable docker
sudo systemctl start docker

echo "=== Verifying installation ==="
docker --version || true
docker compose version || true

echo "=== DONE ==="