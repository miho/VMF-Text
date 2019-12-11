#!/bin/bash

echo "----------------------------------------"
echo "BUILDING CORE..."
echo "----------------------------------------"
cd core
./gradlew clean publishtoMavenLocal --no-daemon

echo "----------------------------------------"
echo "BUILDING PLUGIN..."
echo "----------------------------------------"
cd ../gradle-plugin
./gradlew clean publishtoMavenLocal --no-daemon

cd ..
