# Анализатор событий Windows
Write-Host "=== Windows Event Log Analyzer ===" -ForegroundColor Green

function Get-EventLogSummary {
    param([int]$hours = 24)
    
    $startTime = (Get-Date).AddHours(-$hours)
    
    Write-Host "Collecting events from last $hours hours..." -ForegroundColor Yellow
    
    # Получение событий из основных логов
    $eventLogs = @("Application", "System", "Security")
    $allEvents = @()
    
    foreach ($log in $eventLogs) {
        try {
            $events = Get-WinEvent -LogName $log -MaxEvents 1000 -ErrorAction SilentlyContinue | 
                     Where-Object { $_.TimeCreated -ge $startTime }
            $allEvents += $events
        }
        catch {
            Write-Host "Could not access $log log: $($_.Exception.Message)" -ForegroundColor Red
        }
    }
    
    return $allEvents
}

function Group-EventsByLevel {
    param($events)
    
    $grouped = $events | Group-Object LevelDisplayName
    
    Write-Host "`n=== Events by Level ===" -ForegroundColor Yellow
    
    foreach ($group in $grouped) {
        $color = switch ($group.Name) {
            "Error" { "Red" }
            "Warning" { "Yellow" }
            "Information" { "Green" }
            default { "Gray" }
        }
        
        Write-Host "$($group.Name): $($group.Count) events" -ForegroundColor $color
    }
    
    return $grouped
}

function Find-TopErrorSources {
    param($events)
    
    $errorEvents = $events | Where-Object { $_.LevelDisplayName -eq "Error" }
    
    if (-not $errorEvents) {
        Write-Host "No error events found" -ForegroundColor Green
        return
    }
    
    $errorSources = $errorEvents | Group-Object ProviderName | Sort-Object Count -Descending | Select-Object -First 10
    
    Write-Host "`n=== Top Error Sources ===" -ForegroundColor Yellow
    
    foreach ($source in $errorSources) {
        Write-Host "$($source.Name): $($source.Count) errors" -ForegroundColor Red
    }
    
    return $errorSources
}

function Create-HourlyTimeline {
    param($events)
    
    $hourlyStats = $events | Group-Object { $_.TimeCreated.Hour }
    
    Write-Host "`n=== Events by Hour ===" -ForegroundColor Yellow
    
    # Создание простой ASCII диаграммы
    $maxCount = ($hourlyStats | Measure-Object Count -Maximum).Maximum
    
    foreach ($hour in 0..23) {
        $hourData = $hourlyStats | Where-Object Name -eq $hour.ToString()
        $count = if ($hourData) { $hourData.Count } else { 0 }
        $bar = "█" * [math]::Round(($count / $maxCount) * 20)
        
        Write-Host "$($hour.ToString("00")):00 - " -NoNewline
        Write-Host "$bar $count events" -ForegroundColor $(if ($count -gt 0) { "Cyan" } else { "Gray" })
    }
}

function Send-CriticalAlert {
    param($events)
    
    $criticalErrors = $events | Where-Object { 
        $_.LevelDisplayName -eq "Error" -and 
        $_.TimeCreated -gt (Get-Date).AddHours(-1)
    }
    
    if ($criticalErrors) {
        Write-Host "`n🚨 CRITICAL ALERT: $($criticalErrors.Count) errors in last hour! 🚨" -ForegroundColor Red -BackgroundColor White
        
        $topCritical = $criticalErrors | Group-Object ProviderName | Sort-Object Count -Descending | Select-Object -First 3
        
        foreach ($error in $topCritical) {
            Write-Host "  - $($error.Name): $($error.Count) errors" -ForegroundColor Red
        }
        
        # Здесь можно добавить отправку email или другого уведомления
        # Send-EmailNotification -Errors $criticalErrors
    }
}

function Generate-EventReport {
    param($events, [string]$reportPath = "EventAnalysisReport.html")
    
    $reportData = @"
<!DOCTYPE html>
<html>
<head>
    <title>Windows Event Log Report</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        h1 { color: #333; }
        .section { margin: 20px 0; padding: 15px; border: 1px solid #ddd; }
        .error { color: #d9534f; }
        .warning { color: #f0ad4e; }
        .info { color: #5bc0de; }
        table { width: 100%; border-collapse: collapse; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background-color: #f2f2f2; }
    </style>
</head>
<body>
    <h1>Windows Event Log Analysis Report</h1>
    <p>Generated: $(Get-Date)</p>
    
    <div class="section">
        <h2>Event Summary</h2>
        <p>Total Events: $($events.Count)</p>
"@

    $levelGroups = $events | Group-Object LevelDisplayName
    foreach ($group in $levelGroups) {
        $reportData += "<p class='$($group.Name.ToLower())'>$($group.Name): $($group.Count) events</p>"
    }

    $reportData += @"
    </div>
    
    <div class="section">
        <h2>Recent Critical Errors</h2>
        <table>
            <tr><th>Time</th><th>Source</th><th>Message</th></tr>
"@

    $recentErrors = $events | Where-Object { $_.LevelDisplayName -eq "Error" } | Sort-Object TimeCreated -Descending | Select-Object -First 10
    foreach ($error in $recentErrors) {
        $shortMessage = $error.Message -replace "`n", " " -replace "`r", " "
        if ($shortMessage.Length -gt 100) { $shortMessage = $shortMessage.Substring(0, 100) + "..." }
        
        $reportData += "<tr><td>$($error.TimeCreated)</td><td>$($error.ProviderName)</td><td>$shortMessage</td></tr>"
    }

    $reportData += @"
        </table>
    </div>
</body>
</html>
"@

    $reportData | Out-File -FilePath $reportPath -Encoding UTF8
    Write-Host "HTML report generated: $reportPath" -ForegroundColor Cyan
}

# Основной анализ
Write-Host "Starting Windows Event Log Analysis..." -ForegroundColor Green

$events = Get-EventLogSummary -hours 24

if ($events) {
    Write-Host "Collected $($events.Count) events" -ForegroundColor Green
    
    Group-EventsByLevel -events $events
    Find-TopErrorSources -events $events
    Create-HourlyTimeline -events $events
    Send-CriticalAlert -events $events
    
    # Генерация отчета
    $reportFile = "EventReport_$(Get-Date -Format 'yyyyMMdd_HHmmss').html"
    Generate-EventReport -events $events -reportPath $reportFile
    
    Write-Host "`nAnalysis completed!" -ForegroundColor Green
} else {
    Write-Host "No events found in the specified time range" -ForegroundColor Yellow
}
