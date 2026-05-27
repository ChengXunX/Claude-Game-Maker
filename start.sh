#!/bin/bash

# ChengXun Game Maker Startup Script

echo "Starting ChengXun Game Maker..."

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "Error: Java is not installed"
    exit 1
fi

# Check if Claude CLI is installed
if ! command -v claude &> /dev/null; then
    echo "Warning: Claude CLI is not installed at /usr/bin/claude"
fi

# Create data directories if they don't exist
mkdir -p data/contexts data/memory data/projects

# Build the project if needed
if [ ! -f "target/game-maker-1.0-SNAPSHOT.jar" ]; then
    echo "Building project..."
    mvn clean package -DskipTests
fi

# Run the application
echo "Starting application..."
java -jar target/game-maker-1.0-SNAPSHOT.jar
