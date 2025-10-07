#!/bin/bash
set -e

echo "Building Yoga App for Render..."

# Make gradlew executable
chmod +x ./gradlew

# Build the application (skip tests for faster deployment)
./gradlew build -x test

echo "Build completed successfully!"