# Модуль управления службами

# Создание manifest модуля
$modulePath = "ServiceManager"
$manifestPath = "$modulePath\ServiceManager.psd1"

# Создание директории модуля
if (Test-Path $modulePath) {
    Remove-Item $modulePath -Recurse -Force
}
New-Item -Path $modulePath -ItemType Directory -Force | Out-Null

# Функции модуля
$moduleScript = @'
# Функция получения статуса служб
function Get-ServiceStatus {
    param(
        [string]$ServiceName,
        [switch]$IncludeDependencies
    )
    
    if ($ServiceName) {
        $services = Get-Service -Name $ServiceName -ErrorAction SilentlyContinue
    } else {
        $services = Get-Service
    }
    
    if (-not $services) {
        Write-Warning "No services found matching the criteria"
        return
    }
    
    $serviceInfo = foreach ($service in $services) {
        $info = [PSCustomObject]@{
            Name = $service.Name
            DisplayName = $service.DisplayName
            Status = $service.Status
            StartType = (Get-CimInstance Win32_Service -Filter "Name='$($service.Name)'").StartMode
            CanStop = $service.CanStop
            CanPauseAndContinue = $service.CanPauseAndContinue
        }
        
        if ($IncludeDependencies) {
            $dependencies = (Get-Service -Name $service.Name -RequiredServices).Name -join ", "
            $info | Add-Member -NotePropertyName "Dependencies" -NotePropertyValue $dependencies
        }
        
        $info
    }
    
    return $serviceInfo
}

# Функция управления службами
function Invoke-ServiceAction {
    param(
        [Parameter(Mandatory)]
        [string]$ServiceName,
        
        [Parameter(Mandatory)]
        [ValidateSet("Start", "Stop", "Restart", "Pause", "Resume")]
        [string]$Action,
        
        [int]$Timeout = 30
    )
    
    try {
        $service = Get-Service -Name $ServiceName -ErrorAction Stop
        
        Write-Host "Performing $Action on service: $($service.DisplayName)" -ForegroundColor Yellow
        
        switch ($Action) {
            "Start" { 
                if ($service.Status -ne "Running") {
                    Start-Service -Name $ServiceName
                    Wait-ServiceStatus -ServiceName $ServiceName -DesiredStatus "Running" -Timeout $Timeout
                } else {
                    Write-Host "Service is already running" -ForegroundColor Yellow
                }
            }
            "Stop" { 
                if ($service.Status -ne "Stopped") {
                    Stop-Service -Name $ServiceName -Force
                    Wait-ServiceStatus -ServiceName $ServiceName -DesiredStatus "Stopped" -Timeout $Timeout
                } else {
                    Write-Host "Service is already stopped" -ForegroundColor Yellow
                }
            }
            "Restart" { 
                Restart-Service -Name $ServiceName -Force
                Wait-ServiceStatus -ServiceName $ServiceName -DesiredStatus "Running" -Timeout $Timeout
            }
            "Pause" { 
                if ($service.CanPauseAndContinue) {
                    Suspend-Service -Name $ServiceName
                    Wait-ServiceStatus -ServiceName $ServiceName -DesiredStatus "Paused" -Timeout $Timeout
                } else {
                    Write-Warning "Service does not support pause operation"
                }
            }
            "Resume" { 
                if ($service.CanPauseAndContinue -and $service.Status -eq "Paused") {
                    Resume-Service -Name $ServiceName
                    Wait-ServiceStatus -ServiceName $ServiceName -DesiredStatus "Running" -Timeout $Timeout
                } else {
                    Write-Warning "Service cannot be resumed or is not paused"
                }
            }
        }
        
        Write-Host "$Action completed successfully for $ServiceName" -ForegroundColor Green
    }
    catch {
        Write-Error "Failed to $Action service $ServiceName : $($_.Exception.Message)"
    }
}

# Вспомогательная функция ожидания статуса службы
function Wait-ServiceStatus {
    param(
        [string]$ServiceName,
        [string]$DesiredStatus,
        [int]$Timeout = 30
    )
    
    $startTime = Get-Date
    do {
        $currentStatus = (Get-Service -Name $ServiceName).Status
        if ($currentStatus -eq $DesiredStatus) {
            return $true
        }
        
        Start-Sleep -Seconds 1
        $elapsed = (Get-Date) - $startTime
    } while ($elapsed.TotalSeconds -lt $Timeout)
    
    Write-Warning "Timeout waiting for service $ServiceName to reach status $DesiredStatus"
    return $false
}

# Функция настройки типа запуска
function Set-ServiceStartupType {
    param(
        [Parameter(Mandatory)]
        [string]$ServiceName,
        
        [Parameter(Mandatory)]
        [ValidateSet("Automatic", "Manual", "Disabled")]
        [string]$StartupType
    )
    
    try {
        # Проверка прав администратора
        if (-NOT ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole] "Administrator")) {
            throw "Administrator rights required to change service startup type"
        }
        
        $service = Get-WmiObject -Class Win32_Service -Filter "Name='$ServiceName'" -ErrorAction Stop
        
        if ($service.StartMode -eq $StartupType) {
            Write-Host "Service $ServiceName is already set to $StartupType" -ForegroundColor Yellow
            return
        }
        
        $result = $service.ChangeStartMode($StartupType)
        
        if ($result.ReturnValue -eq 0) {
            Write-Host "Startup type for $ServiceName changed to $StartupType" -ForegroundColor Green
        } else {
            throw "Failed to change startup type. Return code: $($result.ReturnValue)"
        }
    }
    catch {
        Write-Error "Failed to set startup type for $ServiceName : $($_.Exception.Message)"
    }
}

# Функция мониторинга зависимостей
function Get-ServiceDependencies {
    param(
        [Parameter(Mandatory)]
        [string]$ServiceName
    )
    
    try {
        $service = Get-Service -Name $ServiceName -ErrorAction Stop
        
        Write-Host "=== Dependencies for: $($service.DisplayName) ===" -ForegroundColor Yellow
        
        # Required services (зависимости)
        $requiredServices = Get-Service -Name $ServiceName -RequiredServices
        if ($requiredServices) {
            Write-Host "`nRequired Services:" -ForegroundColor Green
            $requiredServices | Format-Table Name, DisplayName, Status -AutoSize
        } else {
            Write-Host "`nNo required services" -ForegroundColor Gray
        }
        
        # Dependent services (службы, которые зависят от этой)
        $dependentServices = Get-Service -Name $ServiceName -DependentServices
        if ($dependentServices) {
            Write-Host "`nDependent Services:" -ForegroundColor Cyan
            $dependentServices | Format-Table Name, DisplayName, Status -AutoSize
        } else {
            Write-Host "`nNo dependent services" -ForegroundColor Gray
        }
        
        return @{
            Required = $requiredServices
            Dependent = $dependentServices
        }
    }
    catch {
        Write-Error "Service $ServiceName not found: $($_.Exception.Message)"
    }
}

# Функция массового управления службами
function Invoke-BulkServiceAction {
    param(
        [string[]]$ServiceNames,
        [string]$Action,
        [switch]$WhatIf
    )
    
    $results = @()
    
    foreach ($serviceName in $ServiceNames) {
        if ($WhatIf) {
            Write-Host "WHATIF: Would perform $Action on $serviceName" -ForegroundColor Yellow
            continue
        }
        
        try {
            Invoke-ServiceAction -ServiceName $serviceName -Action $Action
            $status = "Success"
        }
        catch {
            $status = "Failed: $($_.Exception.Message)"
        }
        
        $results += [PSCustomObject]@{
            ServiceName = $serviceName
            Action = $Action
            Status = $status
            Timestamp = Get-Date
        }
    }
    
    return $results
}

Export-ModuleMember -Function Get-ServiceStatus, Invoke-ServiceAction, Set-ServiceStartupType, Get-ServiceDependencies, Invoke-BulkServiceAction
'@

# Создание файла модуля
Set-Content -Path "$modulePath\ServiceManager.psm1" -Value $moduleScript

# Создание manifest
$manifestParams = @{
    Path = $manifestPath
    RootModule = "ServiceManager.psm1"
    ModuleVersion = "1.0.0"
    GUID = [guid]::NewGuid().ToString()
    Author = "PowerShell Course"
    CompanyName = "Training"
    Copyright = "(c) 2024. All rights reserved."
    Description = "Advanced Windows Service Management Module"
    PowerShellVersion = "5.1"
    FunctionsToExport = @('Get-ServiceStatus', 'Invoke-ServiceAction', 'Set-ServiceStartupType', 'Get-ServiceDependencies', 'Invoke-BulkServiceAction')
    CmdletsToExport = @()
    VariablesToExport = '*'
    AliasesToExport = @()
}

New-ModuleManifest @manifestParams

Write-Host "Service Manager Module created successfully!" -ForegroundColor Green
Write-Host "Module path: $modulePath" -ForegroundColor Cyan
Write-Host "`nTo use the module:" -ForegroundColor Yellow
Write-Host "1. Import-Module .\$modulePath\ServiceManager.psd1" -ForegroundColor White
Write-Host "2. Get-Command -Module ServiceManager" -ForegroundColor White
Write-Host "3. Get-ServiceStatus -ServiceName 'Spooler' -IncludeDependencies" -ForegroundColor White

# Демонстрация использования модуля
Write-Host "`n=== Module Demonstration ===" -ForegroundColor Green

try {
    Import-Module $manifestPath -Force
    Write-Host "✓ Module imported successfully" -ForegroundColor Green
    
    # Демонстрация функций
    Write-Host "`n--- Available Services ---" -ForegroundColor Yellow
    Get-ServiceStatus | Select-Object -First 5 | Format-Table -AutoSize
    
    Write-Host "`n--- Service Dependencies Example ---" -ForegroundColor Yellow
    Get-ServiceDependencies -ServiceName "Spooler"
    
}
catch {
    Write-Host "Error demonstrating module: $($_.Exception.Message)" -ForegroundColor Red
}
