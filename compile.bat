@echo off
echo Compiling TypingGame Java Project...
echo =====================================

if not exist "src" (
    echo Error: src directory not found!
    echo Please run this script from the TypingGame project directory.
    pause
    exit /b 1
)

if not exist "out" (
    echo Creating out directory...
    mkdir out
)

echo Compiling all Java files...
javac -cp "." -d "out" src\*.java src\client\*.java src\server\*.java src\shared\*.java

if %errorlevel% equ 0 (
    echo.
    echo Compilation successful!
    echo Output directory: out\
    echo.
    echo You can now run:
    echo - run.bat          ^(for the game^)
    echo - run-server.bat   ^(for the server^)
) else (
    echo.
    echo Compilation failed!
    echo Please check the error messages above.
)

pause