@echo off
javac -encoding UTF-8 ..\File_manager\FileManager.java Main.java

if errorlevel 1 (
    echo Compilation failed!
    pause
    exit /b 1
)

java -cp ".;..\File_manager" Main
exit /b 0
