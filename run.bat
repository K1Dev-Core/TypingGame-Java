@echo off
echo Running TypingGame...
echo =====================

if not exist "out" (
    echo Error: out directory not found!
    echo Please run compile.bat first to compile the project.
    pause
    exit /b 1
)

if not exist "out\App.class" (
    echo Error: App.class not found in out directory!
    echo Please run compile.bat first to compile the project.
    pause
    exit /b 1
)

echo Starting TypingGame client...
cd out
java -cp "." App
cd ..

pause