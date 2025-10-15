@echo off
copy ..\..\File_manager\FileManager.class . >nul
javac Main.java

if errorlevel 1 (
    echo Compilation failed!
    del FileManager.class
    exit /b 1
)

java --enable-native-access=ALL-UNNAMED Main 2>nul
del FileManager.class
exit /b 0
