#!/bin/bash

# AJailFix Build Script
# Requires: JDK 17+ and Maven

echo "========================================="
echo "  AJailFix Build Script"
echo "========================================="

# Check for JDK
if ! command -v java &> /dev/null; then
    echo "ERROR: Java not found! Please install JDK 17+"
    echo "Download: https://adoptium.net/"
    exit 1
fi

# Check for Maven
if ! command -v mvn &> /dev/null; then
    echo "ERROR: Maven not found! Please install Maven"
    echo "Download: https://maven.apache.org/download.cgi"
    exit 1
fi

# Display versions
echo ""
echo "Java Version:"
java -version
echo ""
echo "Maven Version:"
mvn -version
echo ""

# Clean and build
echo "Building AJailFix..."
mvn clean package -DskipTests

# Check result
if [ -f "target/ajailfix-*.jar" ]; then
    echo ""
    echo "========================================="
    echo "  BUILD SUCCESSFUL!"
    echo "========================================="
    echo ""
    echo "JAR file created:"
    ls -la target/ajailfix-*.jar
    echo ""
    echo "Location: $(pwd)/target/ajailfix-26.3.5.jar"
else
    echo ""
    echo "========================================="
    echo "  BUILD FAILED!"
    echo "========================================="
    exit 1
fi
