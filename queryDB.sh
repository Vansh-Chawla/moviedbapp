#!/bin/bash

# Configuration
SOURCE_DIR="src/main"
OUTPUT_DIR="out"
MAIN_CLASS="QueryDB"

# Check for arguments
if [ $# -lt 1 ]; then
    echo "Usage: $0 <query_number> [additional_parameters]"
    exit 1
fi

# Set classpath
CLASSPATH="lib/*:$OUTPUT_DIR"

# Run the query
java -cp "$CLASSPATH" $MAIN_CLASS "$@"