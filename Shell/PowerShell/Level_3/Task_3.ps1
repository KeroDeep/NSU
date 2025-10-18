# Умный установщик пакетов
Write-Host "=== Smart Package Installer ===" -ForegroundColor Green

function Test-Administrator {
    $currentPrincipal = New-Object Security.Principal.WindowsPrincipal([Security.Principal.WindowsIdentity]::GetCurrent())
    return $currentPrincipal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
}

function Get-SystemArchitecture {
    return $env:PROCESSOR_ARCHITECTURE
}

function Test-SoftwareInstalled {
    param([string]$softwareName)
    
    # Проверка через реестр
    $registryPaths = @(
        "HKLM:\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\*",
        "HKLM:\SOFTWARE\WOW6432Node\Microsoft\Windows\CurrentVersion\Uninstall\*"
    )
    
    foreach ($path in $registryPaths) {
        $installed = Get-ItemProperty $path -ErrorAction SilentlyContinue | 
                    Where-Object { $_.DisplayName -like "*$softwareName*" }
        if ($installed) { return $true }
    }
    
    # Проверка через Get-Command
    if (Get-Command $softwareName -ErrorAction SilentlyContinue) {
        return $true
    }
    
    return $false
}

function Install-Chocolatey {
    Write-Host "Installing Chocolatey package manager..." -ForegroundColor Yellow
    
    if (-not (Test-Administrator)) {
        Write-Host "Administrator rights required to install Chocolatey" -ForegroundColor Red
        return $false
    }
    
    try {
        Set-ExecutionPolicy Bypass -Scope Process -Force
        [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072
        Invoke-Expression ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))
        Write-Host "Chocolatey installed successfully" -ForegroundColor Green
        return $true
    }
    catch {
        Write-Host "Failed to install Chocolatey: $($_.Exception.Message)" -ForegroundColor Red
        return $false
    }
}

function Install-Package {
    param(
        [string]$packageName,
        [string]$installerType = "chocolatey"
    )
    
    Write-Host "`nInstalling: $packageName" -ForegroundColor Yellow
    
    # Логирование начала установки
    $logEntry = @{
        Timestamp = Get-Date
        Package = $packageName
        Action = "Install"
        Status = "Started"
    }
    Add-Content -Path "package_install.log" -Value "$(ConvertTo-Json $logEntry)"
    
    try {
        switch ($installerType) {
            "chocolatey" {
                if (-not (Get-Command choco -ErrorAction SilentlyContinue)) {
                    Write-Host "Chocolatey not found. Installing..." -ForegroundColor Yellow
                    if (-not (Install-Chocolatey)) {
                        throw "Chocolatey installation failed"
                    }
                }
                
                $architecture = Get-SystemArchitecture
                Write-Host "System architecture: $architecture" -ForegroundColor Gray
                
                # Установка через Chocolatey
                $process = Start-Process choco -ArgumentList "install", $packageName, "-y", "--force" -Wait -PassThru -NoNewWindow
                
                if ($process.ExitCode -eq 0) {
                    Write-Host "✓ $packageName installed successfully" -ForegroundColor Green
                    $logEntry.Status = "Success"
                } else {
                    throw "Installation failed with exit code: $($process.ExitCode)"
                }
            }
            "winget" {
                if (Get-Command winget -ErrorAction SilentlyContinue) {
                    winget install --id $packageName --silent --accept-package-agreements --accept-source-agreements
                    Write-Host "✓ $packageName installed successfully" -ForegroundColor Green
                    $logEntry.Status = "Success"
                } else {
                    throw "Winget not available"
                }
            }
            "msi" {
                # Для MSI установщиков
                Start-Process msiexec -ArgumentList "/i", "$packageName.msi", "/quiet", "/norestart" -Wait
                Write-Host "✓ $packageName installed successfully" -ForegroundColor Green
                $logEntry.Status = "Success"
            }
        }
    }
    catch {
        Write-Host "✗ Failed to install $packageName : $($_.Exception.Message)" -ForegroundColor Red
        $logEntry.Status = "Failed"
        $logEntry.Error = $_.Exception.Message
    }
    
    # Логирование результата
    Add-Content -Path "package_install.log" -Value "$(ConvertTo-Json $logEntry)"
}

function Show-SoftwareCatalog {
    $softwareCatalog = @(
        @{Name = "Google Chrome"; ID = "Google.Chrome"; Type = "chocolatey"},
        @{Name = "Visual Studio Code"; ID = "vscode"; Type = "chocolatey"},
        @{Name = "Git"; ID = "git"; Type = "chocolatey"},
        @{Name = "Node.js"; ID = "nodejs"; Type = "chocolatey"},
        @{Name = "Python 3"; ID = "python"; Type = "chocolatey"},
        @{Name = "7-Zip"; ID = "7zip"; Type = "chocolatey"},
        @{Name = "VLC Media Player"; ID = "vlc"; Type = "chocolatey"},
        @{Name = "Notepad++"; ID = "notepadplusplus"; Type = "chocolatey"}
    )
    
    Write-Host "`n=== Available Software ===" -ForegroundColor Yellow
    
    for ($i = 0; $i -lt $softwareCatalog.Count; $i++) {
        $software = $softwareCatalog[$i]
        $status = if (Test-SoftwareInstalled $software.Name) { "✓ Installed" } else { "Not installed" }
        $color = if ($status -eq "✓ Installed") { "Green" } else { "Gray" }
        
        Write-Host "$($i+1). $($software.Name)" -NoNewline
        Write-Host " [$status]" -ForegroundColor $color
    }
    
    return $softwareCatalog
}

# Основная программа
Write-Host "System Information:" -ForegroundColor Yellow
Write-Host "Architecture: $(Get-SystemArchitecture)" -ForegroundColor Gray
Write-Host "Admin Rights: $(if (Test-Administrator) { 'Yes' } else { 'No' })" -ForegroundColor Gray

do {
    $softwareCatalog = Show-SoftwareCatalog
    
    Write-Host "`n=== Installation Options ===" -ForegroundColor Green
    Write-Host "1. Install specific software"
    Write-Host "2. Install all missing software" 
    Write-Host "3. Check installation log"
    Write-Host "4. Exit"
    
    $choice = Read-Host "`nSelect option (1-4)"
    
    switch ($choice) {
        "1" {
            $softwareNumber = Read-Host "Enter software number to install"
            $selectedSoftware = $softwareCatalog[[int]$softwareNumber - 1]
            
            if ($selectedSoftware) {
                if (Test-SoftwareInstalled $selectedSoftware.Name) {
                    Write-Host "$($selectedSoftware.Name) is already installed" -ForegroundColor Yellow
                } else {
                    Install-Package -packageName $selectedSoftware.ID -installerType $selectedSoftware.Type
                }
            }
        }
        "2" {
            Write-Host "Installing all missing software..." -ForegroundColor Yellow
            foreach ($software in $softwareCatalog) {
                if (-not (Test-SoftwareInstalled $software.Name)) {
                    Install-Package -packageName $software.ID -installerType $software.Type
                    Start-Sleep -Seconds 2 # Пауза между установками
                }
            }
        }
        "3" {
            if (Test-Path "package_install.log") {
                Write-Host "`n=== Installation Log ===" -ForegroundColor Yellow
                Get-Content "package_install.log" | ForEach-Object {
                    $logEntry = $_ | ConvertFrom-Json
                    $color = if ($logEntry.Status -eq "Success") { "Green" } else { "Red" }
                    Write-Host "$($logEntry.Timestamp): $($logEntry.Package) - $($logEntry.Status)" -ForegroundColor $color
                }
            } else {
                Write-Host "No installation log found" -ForegroundColor Yellow
            }
        }
        "4" {
            Write-Host "Goodbye!" -ForegroundColor Green
        }
        default {
            Write-Host "Invalid option" -ForegroundColor Red
        }
    }
    
    if ($choice -ne "4") {
        Read-Host "`nPress Enter to continue..."
    }
} while ($choice -ne "4")
