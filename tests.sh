#!/bin/bash

# Script to compile and run JUnit tests

# Set the base directory
BASE_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Set classpath - includes all JARs in lib directory and the out directory
CLASSPATH="$BASE_DIR/out:$BASE_DIR/lib/*"

# Compile source files
echo "Compiling source files..."
javac -d "$BASE_DIR/out" -cp "$CLASSPATH" "$BASE_DIR/src/main/"*.java
javac -d "$BASE_DIR/out" -cp "$CLASSPATH:$BASE_DIR/out" "$BASE_DIR/src/test/"*.java

# Check if compilation was successful
if [ $? -ne 0 ]; then
    echo "Compilation failed"
    exit 1
fi

echo "Compilation successful"

# Run all JUnit tests
echo "Running JUnit tests..."
java -cp "$CLASSPATH" org.junit.runner.JUnitCore \
    InitialiseDBTest \
    PopulateDBTest \
    QueryDBTest

# Check if tests ran successfully
if [ $? -ne 0 ]; then
    echo "Some tests failed"
    exit 1
fi

echo "All tests passed successfully"
exit 0