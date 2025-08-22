@echo off
echo TypingGame Server Launcher
echo ==========================

if not exist "out" (
    echo Error: out directory not found!
    echo Please run compile.bat first to compile the project.
    pause
    exit /b 1
)

if not exist "out\server\TypingGameServer.class" (
    echo Error: Server classes not found in out directory!
    echo Please run compile.bat first to compile the project.
    pause
    exit /b 1
)

echo Starting TypingGame Server...
cd out
java -cp "." server.TypingGameServer
cd ..

pause