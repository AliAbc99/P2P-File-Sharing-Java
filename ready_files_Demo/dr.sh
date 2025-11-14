#!/bin/bash

# Ensure Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "Docker is not running. Please start Docker first."
    exit 1
fi

# Ensure X11 permissions are set (Linux only)
if [ "$(uname)" == "Linux" ]; then
    xhost +local:docker
fi

# Create or ensure the custom network exists
NETWORK_NAME="my_custom_network"
if ! docker network inspect "$NETWORK_NAME" > /dev/null 2>&1; then
    docker network create "$NETWORK_NAME"
    echo "Custom network $NETWORK_NAME created."
else
    echo "Custom network $NETWORK_NAME already exists."
fi

# Generate a unique container name using the current timestamp
CONTAINER_NAME="java-gui-app-container-$(date +%s)"

# Always rebuild the Docker image to include the latest JAR
echo "Rebuilding Docker image..."
docker build -t java-gui-app .
if [ $? -eq 0 ]; then
    echo "Docker image rebuilt successfully."
else
    echo "Failed to rebuild the Docker image."
    exit 1
fi

# Run the Docker container with the necessary settings for GUI support
docker run --rm --name "$CONTAINER_NAME" --network "$NETWORK_NAME" \
  -e DISPLAY=$DISPLAY -v /tmp/.X11-unix:/tmp/.X11-unix java-gui-app

# Cleanup X11 permissions (Linux only)
if [ "$(uname)" == "Linux" ]; then
    xhost -local:docker
fi
