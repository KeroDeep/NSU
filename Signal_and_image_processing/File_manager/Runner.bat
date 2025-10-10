@echo off
javac FileManager.java

if errorlevel 1 (
    echo Compilation failed!
    pause
    exit /b 1
)

java --enable-native-access=ALL-UNNAMED FileManager 2>nul
exit /b 0
