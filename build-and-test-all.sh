#!/bin/bash

echo "----------------------------------------"
echo "BUILDING CORE..."
echo "----------------------------------------"
cd core
./gradlew clean test publishToMavenLocal --no-daemon

echo "----------------------------------------"
echo "BUILDING PLUGIN..."
echo "----------------------------------------"
cd ../gradle-plugin
./gradlew clean test publishToMavenLocal --no-daemon

echo "----------------------------------------"
echo "TESTING..."
echo "----------------------------------------"
cd ../test-suite 
./gradlew clean test --no-daemon
