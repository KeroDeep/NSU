@echo off
javac Main.java

if errorlevel 1 (
    echo Compilation failed!
    pause
    exit /b 1
)

java --enable-native-access=ALL-UNNAMED Main 2>nul
exit /b 0
