# Монитор здоровья системы
Write-Host "=== System Health Monitor ===" -ForegroundColor Green

function Get-SystemHealth {
    $healthReport = @()
    
    # Проверка загрузки CPU
    $cpuUsage = (Get-WmiObject Win32_Processor | Measure-Object -Property LoadPercentage -Average).Average
    $cpuStatus = if ($cpuUsage -gt 80) { "CRITICAL" } elseif ($cpuUsage -gt 60) { "WARNING" } else { "OK" }
    
    $healthReport += [PSCustomObject]@{
        Component = "CPU"
        Usage = "$cpuUsage%"
        Status = $cpuStatus
        Recommendation = if ($cpuUsage -gt 80) { "Investigate high CPU processes" } elseif ($cpuUsage -gt 60) { "Monitor CPU usage" } else { "Normal" }
    }
    
    # Проверка памяти
    $memory = Get-WmiObject Win32_OperatingSystem
    $freeMemoryGB = [math]::Round($memory.FreePhysicalMemory / 1MB, 2)
    $totalMemoryGB = [math]::Round($memory.TotalVisibleMemorySize / 1MB, 2)
    $memoryUsagePercent = [math]::Round(($totalMemoryGB - $freeMemoryGB) / $totalMemoryGB * 100, 2)
    $memoryStatus = if ($freeMemoryGB -lt 1) { "CRITICAL" } elseif ($freeMemoryGB -lt 2) { "WARNING" } else { "OK" }
    
    $healthReport += [PSCustomObject]@{
        Component = "Memory"
        Usage = "$memoryUsagePercent% ($freeMemoryGB GB free)"
        Status = $memoryStatus
        Recommendation = if ($freeMemoryGB -lt 1) { "Close applications or add RAM" } elseif ($freeMemoryGB -lt 2) { "Monitor memory usage" } else { "Normal" }
    }
    
    # Проверка диска C:
    $disk = Get-WmiObject Win32_LogicalDisk -Filter "DeviceID='C:'"
    $freeSpaceGB = [math]::Round($disk.FreeSpace / 1GB, 2)
    $diskStatus = if ($freeSpaceGB -lt 5) { "CRITICAL" } elseif ($freeSpaceGB -lt 10) { "WARNING" } else { "OK" }
    
    $healthReport += [PSCustomObject]@{
        Component = "Disk C:"
        Usage = "$freeSpaceGB GB free"
        Status = $diskStatus
        Recommendation = if ($freeSpaceGB -lt 5) { "Clean up disk space immediately" } elseif ($freeSpaceGB -lt 10) { "Consider disk cleanup" } else { "Normal" }
    }
    
    return $healthReport
}

function Send-HealthAlert {
    param($healthReport)
    
    $criticalIssues = $healthReport | Where-Object { $_.Status -eq "CRITICAL" }
    
    if ($criticalIssues) {
        Write-Host "`n🚨 CRITICAL ALERTS 🚨" -ForegroundColor Red -BackgroundColor White
        foreach ($issue in $criticalIssues) {
            Write-Host "CRITICAL: $($issue.Component) - $($issue.Usage)" -ForegroundColor Red
            Write-Host "Action Required: $($issue.Recommendation)" -ForegroundColor Yellow
        }
        
        # Здесь можно добавить отправку email или другого уведомления
        # Send-EmailAlert -Issues $criticalIssues
    }
}

function Show-HealthReport {
    param($healthReport)
    
    Write-Host "`n--- System Health Report ---" -ForegroundColor Yellow
    Write-Host "Generated: $(Get-Date)" -ForegroundColor Gray
    
    foreach ($item in $healthReport) {
        $color = switch ($item.Status) {
            "OK" { "Green" }
            "WARNING" { "Yellow" }
            "CRITICAL" { "Red" }
        }
        
        Write-Host "`n$($item.Component):" -NoNewline
        Write-Host " $($item.Usage)" -ForegroundColor $color -NoNewline
        Write-Host " [$($item.Status)]"
        Write-Host "Recommendation: $($item.Recommendation)" -ForegroundColor Gray
    }
}

# Основной мониторинг
do {
    Clear-Host
    Write-Host "=== Real-time System Health Monitor ===" -ForegroundColor Green
    Write-Host "Press 'q' to quit, any other key to refresh`n" -ForegroundColor Gray
    
    $healthReport = Get-SystemHealth
    Show-HealthReport -healthReport $healthReport
    Send-HealthAlert -healthReport $healthReport
    
    Write-Host "`n" + "="*50 -ForegroundColor Gray
    Write-Host "Press any key to refresh or 'q' to quit..." -ForegroundColor Gray
    
    $key = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
    
} while ($key.Character -ne 'q')

Write-Host "`nMonitoring stopped. Goodbye!" -ForegroundColor Green
