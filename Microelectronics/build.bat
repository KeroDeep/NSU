@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ========================================
echo    Building MDP analysis application
echo ========================================

echo Step 1: Cleaning previous builds...
if exist "build" (
    rmdir /s /q "build" 2>nul
)
if exist "dist" (
    rmdir /s /q "dist" 2>nul
)
if exist "__pycache__" (
    rmdir /s /q "__pycache__" 2>nul
)
if exist "main.exe" (
    del "main.exe" 2>nul
)

echo Step 2: Creating virtual environment...
python -m venv venv

echo Step 3: Activating virtual environment...
call venv\Scripts\activate.bat

echo Step 4: Installing dependencies...
pip install numpy matplotlib pillow pyinstaller --quiet

echo Step 5: Building using build.spec file...
pyinstaller build.spec

echo Step 6: Moving EXE to current directory...
if exist "dist\main.exe" (
    move "dist\main.exe" "main.exe"
    echo EXE file moved successfully!
) else (
    echo EXE file not found!
    goto :error
)

echo Step 7: Cleaning up build artifacts...
if exist "build" (
    rmdir /s /q "build" 2>nul
)
if exist "dist" (
    rmdir /s /q "dist" 2>nul
)
if exist "__pycache__" (
    rmdir /s /q "__pycache__" 2>nul
)
if exist "venv" (
    rmdir /s /q "venv" 2>nul
)

echo ========================================
echo      Build completed successfully!
echo ========================================

goto :end

:error
echo ========================================
echo             Build failed!
echo ========================================

echo Cleaning up after error...
if exist "build" (
    rmdir /s /q "build" 2>nul
)
if exist "dist" (
    rmdir /s /q "dist" 2>nul
)
if exist "__pycache__" (
    rmdir /s /q "__pycache__" 2>nul
)
if exist "venv" (
    rmdir /s /q "venv" 2>nul
)
exit /b 1

:end
