@echo off
echo TypingGame Server GUI Launcher
echo ===============================

if not exist "out" (
    echo Error: out directory not found!
    echo Please run compile.bat first to compile the project.
    pause
    exit /b 1
)

if not exist "out\server\ServerGUI.class" (
    echo Error: Server GUI classes not found in out directory!
    echo Please run compile.bat first to compile the project.
    pause
    exit /b 1
)

echo Starting TypingGame Server GUI...
cd out
java -cp "." server.ServerGUI
cd ..

pause