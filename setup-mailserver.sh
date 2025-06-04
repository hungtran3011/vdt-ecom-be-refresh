#!/bin/bash

# Create required directories
mkdir -p mail-data mail-state mail-config

# Start the mailserver first
echo "Starting mailserver..."
docker compose up -d mailserver

# Wait for mailserver to be ready
echo "Waiting for mailserver to start..."
sleep 30

# Download the setup script
curl -o setup.sh https://raw.githubusercontent.com/docker-mailserver/docker-mailserver/master/setup.sh
chmod +x setup.sh

# Create test accounts using docker exec
echo "Creating email accounts..."
docker exec mailserver setup email add test@example.test testpassword
docker exec mailserver setup email add noreply@example.test password123
docker exec mailserver setup email add orders@example.test orderpass

# Create aliases file
echo "alias1@example.test test@example.test" > mail-config/postfix-virtual.cf

# Restart mailserver to apply changes
echo "Restarting mailserver to apply changes..."
docker compose restart mailserver

echo "Mail setup complete!"