#!/bin/bash

# Configuration
SOURCE_DIR="src/main"
OUTPUT_DIR="out"
MAIN_CLASS="PopulateDB"

# Create output directory if it doesn't exist
mkdir -p $OUTPUT_DIR

# Verify CSV directory exists
if [ ! -d "csvfiles" ]; then
    echo "Error: csvfiles directory not found in project root"
    exit 1
fi

# Set classpath
CLASSPATH="lib/*:$OUTPUT_DIR"

# Compile
echo "Compiling $MAIN_CLASS..."
javac -cp "$CLASSPATH" -d $OUTPUT_DIR $SOURCE_DIR/${MAIN_CLASS}.java

# Run if compilation succeeded
if [ $? -eq 0 ]; then
    echo "Running $MAIN_CLASS..."
    java -cp "$CLASSPATH" $MAIN_CLASS
else
    echo "Compilation failed."
    exit 1
fi