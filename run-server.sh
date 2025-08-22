#!/bin/bash

echo "TypingGame Server Launcher"
echo "=========================="

cd "$(dirname "$0")"

if [ ! -d "src" ]; then
    echo "Error: src directory not found!"
    echo "Please run this script from the TypingGame project directory."
    exit 1
fi

echo "Compiling server files..."
javac -cp "." -d . src/server/*.java src/shared/*.java

if [ $? -eq 0 ]; then
    echo "Compilation successful!"
    echo "Starting Server GUI..."
    java -cp "." server.ServerLauncherGUI
else
    echo "Compilation failed!"
    echo "Note: Some compilation warnings about UIManager.getSystemLookAndFeel() are normal."
    echo "Trying to run anyway..."
    java -cp "." server.ServerLauncherGUI
fi