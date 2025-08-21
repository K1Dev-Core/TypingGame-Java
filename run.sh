#!/bin/bash

# Check if project is compiled
if [ ! -d "out" ] || [ ! "$(ls -A out)" ]; then
    echo "Project not compiled. Running compile.sh first..."
    ./compile.sh
fi

echo "Running TypingGame..."
cd out
java -cp ".:../lib/*" App
