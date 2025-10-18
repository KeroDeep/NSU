# Умный парсер логов IIS
Write-Host "=== IIS Log Parser ===" -ForegroundColor Green

class IISLogParser {
    [string]$LogPath
    [array]$LogEntries
    [hashtable]$AnalysisResults
    
    IISLogParser([string]$path) {
        $this.LogPath = $path
        $this.LogEntries = @()
        $this.AnalysisResults = @{
            SuspiciousActivity = @()
            AccessPatterns = @()
            BotsAndScanners = @()
            SecurityReport = @()
        }
    }
    
    [void]ParseLogFile() {
        if (-not (Test-Path $this.LogPath)) {
            throw "Log file not found: $($this.LogPath)"
        }
        
        Write-Host "Parsing IIS log file: $($this.LogPath)" -ForegroundColor Yellow
        
        $logContent = Get-Content $this.LogPath
        $this.LogEntries = @()
        
        foreach ($line in $logContent) {
            if ($line.StartsWith("#")) {
                # Пропуск комментариев
                continue
            }
            
            $logEntry = $this.ParseLogLine($line)
            if ($logEntry) {
                $this.LogEntries += $logEntry
            }
        }
        
        Write-Host "Parsed $($this.LogEntries.Count) log entries" -ForegroundColor Green
    }
    
    [PSCustomObject]ParseLogLine([string]$line) {
        $fields = $line -split " "
        
        if ($fields.Count -lt 12) {
            return $null
        }
        
        try {
            return [PSCustomObject]@{
                DateTime = $this.ParseIISDateTime($fields[0], $fields[1])
                ServerIP = $fields[2]
                Method = $fields[3]
                URI = $fields[4]
                QueryString = $fields[5]
                Port = $fields[6]
                Username = $fields[7]
                ClientIP = $fields[8]
                UserAgent = $fields[9] -replace "^""|""$", ""  # Удаление кавычек
                StatusCode = [int]$fields[10]
                SubStatus = [int]$fields[11]
                Win32Status = [int]$fields[12]
                TimeTaken = [int]$fields[13]
            }
        }
        catch {
            Write-Host "Warning: Could not parse line: $line" -ForegroundColor Yellow
            return $null
        }
    }
    
    [datetime]ParseIISDateTime([string]$date, [string]$time) {
        return [datetime]::ParseExact("$date $time", "yyyy-MM-dd HH:mm:ss", $null)
    }
    
    [void]DetectSuspiciousActivity() {
        Write-Host "Detecting suspicious activity..." -ForegroundColor Yellow
        
        $suspiciousPatterns = @()
        
        # Поиск SQL injection попыток
        $sqlInjectionPatterns = @(
            "union.*select", "select.*from", "insert.*into", "drop.*table", 
            "exec.*xp_", "1=1", "';", "--", "/*", "*/"
        )
        
        foreach ($entry in $this.LogEntries) {
            $uri = $entry.URI + $entry.QueryString
            
            # Проверка SQL injection
            foreach ($pattern in $sqlInjectionPatterns) {
                if ($uri -match $pattern -or $entry.QueryString -match $pattern) {
                    $suspiciousPatterns += @{
                        Type = "SQL Injection Attempt"
                        ClientIP = $entry.ClientIP
                        URI = $entry.URI
                        QueryString = $entry.QueryString
                        DateTime = $entry.DateTime
                        UserAgent = $entry.UserAgent
                        Severity = "HIGH"
                    }
                    break
                }
            }
            
            # Проверка на directory traversal
            if ($uri -match "\.\./|\.\.\\") {
                $suspiciousPatterns += @{
                    Type = "Directory Traversal Attempt"
                    ClientIP = $entry.ClientIP
                    URI = $entry.URI
                    DateTime = $entry.DateTime
                    UserAgent = $entry.UserAgent
                    Severity = "HIGH"
                }
            }
            
            # Проверка на чрезмерное количество 404 ошибок с одного IP
            $client404s = $this.LogEntries | 
                         Where-Object { $_.ClientIP -eq $entry.ClientIP -and $_.StatusCode -eq 404 }
            
            if ($client404s.Count -gt 50) {
                $suspiciousPatterns += @{
                    Type = "Excessive 404 Errors"
                    ClientIP = $entry.ClientIP
                    Count = $client404s.Count
                    DateTime = $entry.DateTime
                    Severity = "MEDIUM"
                }
            }
            
            # Проверка на сканирование уязвимостей
            if ($uri -match "/(phpmyadmin|admin|wp-admin|administrator|\.env|\.git)") {
                $suspiciousPatterns += @{
                    Type = "Admin Panel Scanning"
                    ClientIP = $entry.ClientIP
                    URI = $entry.URI
                    DateTime = $entry.DateTime
                    UserAgent = $entry.UserAgent
                    Severity = "MEDIUM"
                }
            }
        }
        
        $this.AnalysisResults.SuspiciousActivity = $suspiciousPatterns | Sort-Object DateTime
    }
    
    [void]AnalyzeAccessPatterns() {
        Write-Host "Analyzing access patterns..." -ForegroundColor Yellow
        
        $patterns = @()
        
        # Анализ по часам
        $hourlyStats = $this.LogEntries | Group-Object { $_.DateTime.Hour }
        
        foreach ($hour in $hourlyStats) {
            $patterns += @{
                Type = "Hourly Traffic"
                Hour = $hour.Name
                RequestCount = $hour.Count
                Peak = ($hour.Count -eq ($hourlyStats | Measure-Object Count -Maximum).Maximum)
            }
        }
        
        # Анализ по страницам
        $pageStats = $this.LogEntries | Group-Object URI | Sort-Object Count -Descending | Select-Object -First 10
        
        foreach ($page in $pageStats) {
            $patterns += @{
                Type = "Popular Page"
                Page = $page.Name
                RequestCount = $page.Count
                AverageTime = ($page.Group | Measure-Object TimeTaken -Average).Average
            }
        }
        
        # Анализ по методам HTTP
        $methodStats = $this.LogEntries | Group-Object Method
        
        foreach ($method in $methodStats) {
            $patterns += @{
                Type = "HTTP Method"
                Method = $method.Name
                Count = $method.Count
                Percentage = [math]::Round(($method.Count / $this.LogEntries.Count) * 100, 2)
            }
        }
        
        $this.AnalysisResults.AccessPatterns = $patterns
    }
    
    [void]DetectBotsAndScanners() {
        Write-Host "Detecting bots and scanners..." -ForegroundColor Yellow
        
        $botPatterns = @(
            "bot", "crawler", "spider", "scanner", "nmap", "nikto", 
            "sqlmap", "metasploit", "burp", "zap", "acunetix"
        )
        
        $botResults = @()
        
        foreach ($entry in $this.LogEntries) {
            $userAgent = $entry.UserAgent.ToLower()
            
            foreach ($pattern in $botPatterns) {
                if ($userAgent -match $pattern) {
                    $botResults += @{
                        ClientIP = $entry.ClientIP
                        UserAgent = $entry.UserAgent
                        Type = "Bot/Scanner"
                        DateTime = $entry.DateTime
                        URI = $entry.URI
                        Confidence = if ($pattern -match "nmap|nikto|sqlmap") { "HIGH" } else { "MEDIUM" }
                    }
                    break
                }
            }
            
            # Обнаружение по поведению (много запросов за короткое время)
            $clientRequests = $this.LogEntries | 
                            Where-Object { $_.ClientIP -eq $entry.ClientIP } |
                            Group-Object { $_.DateTime.ToString("yyyy-MM-dd HH") }
            
            foreach ($hour in $clientRequests) {
                if ($hour.Count -gt 1000) {  # Более 1000 запросов в час
                    $botResults += @{
                        ClientIP = $entry.ClientIP
                        UserAgent = $entry.UserAgent
                        Type = "Potential DDoS/Bot"
                        DateTime = $entry.DateTime
                        RequestCount = $hour.Count
                        Confidence = "HIGH"
                    }
                }
            }
        }
        
        $this.AnalysisResults.BotsAndScanners = $botResults | Sort-Object DateTime
    }
    
    [void]GenerateSecurityReport() {
        Write-Host "Generating security report..." -ForegroundColor Yellow
        
        $report = @{
            Summary = @{
                TotalRequests = $this.LogEntries.Count
                UniqueIPs = ($this.LogEntries | Group-Object ClientIP).Count
                SuspiciousActivities = $this.AnalysisResults.SuspiciousActivity.Count
                BotsDetected = ($this.AnalysisResults.BotsAndScanners | Group-Object ClientIP).Count
                AnalysisDate = Get-Date
            }
            TopThreats = @()
            Recommendations = @()
        }
        
        # Анализ угроз
        $highThreats = $this.AnalysisResults.SuspiciousActivity | Where-Object { $_.Severity -eq "HIGH" }
        if ($highThreats.Count -gt 0) {
            $report.TopThreats += "Critical: $($highThreats.Count) high severity threats detected"
        }
        
        $botThreats = $this.AnalysisResults.BotsAndScanners | Where-Object { $_.Confidence -eq "HIGH" }
        if ($botThreats.Count -gt 0) {
            $report.TopThreats += "Critical: $($botThreats.Count) confirmed bots/scanners detected"
        }
        
        # Рекомендации
        if ($highThreats.Count -gt 0) {
            $report.Recommendations += "Block IP addresses with high threat scores"
            $report.Recommendations += "Implement WAF rules for SQL injection protection"
        }
        
        if ($botThreats.Count -gt 0) {
            $report.Recommendations += "Implement rate limiting for suspicious IPs"
            $report.Recommendations += "Consider using bot detection services"
        }
        
        $suspiciousIPs = $this.AnalysisResults.SuspiciousActivity | Group-Object ClientIP | Sort-Object Count -Descending
        if ($suspiciousIPs.Count -gt 0) {
            $report.Recommendations += "Review and potentially block: $($suspiciousIPs[0..4].Name -join ', ')"
        }
        
        $this.AnalysisResults.SecurityReport = $report
    }
    
    [void]ExportSecurityReport([string]$outputPath) {
        $reportData = @{
            AnalysisResults = $this.AnalysisResults
            LogStatistics = @{
                TimeRange = @{
                    Start = ($this.LogEntries | Sort-Object DateTime | Select-Object -First 1).DateTime
                    End = ($this.LogEntries | Sort-Object DateTime | Select-Object -Last 1).DateTime
                }
                StatusCodes = $this.LogEntries | Group-Object StatusCode | Sort-Object Count -Descending
                TopClients = $this.LogEntries | Group-Object ClientIP | Sort-Object Count -Descending | Select-Object -First 10
            }
        }
        
        # HTML отчет
        $htmlReport = @"
<!DOCTYPE html>
<html>
<head>
    <title>IIS Security Analysis Report</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .header { background: #2c3e50; color: white; padding: 20px; border-radius: 5px; }
        .section { margin: 20px 0; padding: 15px; border: 1px solid #ddd; border-radius: 5px; }
        .critical { border-left: 5px solid #e74c3c; background: #fadbd8; }
        .warning { border-left: 5px solid #f39c12; background: #fef9e7; }
        .info { border-left: 5px solid #3498db; background: #ebf5fb; }
        table { width: 100%; border-collapse: collapse; margin: 10px 0; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background: #34495e; color: white; }
    </style>
</head>
<body>
    <div class="header">
        <h1>🛡️ IIS Security Analysis Report</h1>
        <p>Generated: $(Get-Date)</p>
    </div>
    
    <div class="section info">
        <h2>📊 Summary</h2>
        <p><strong>Total Requests:</strong> $($this.AnalysisResults.SecurityReport.Summary.TotalRequests)</p>
        <p><strong>Unique IPs:</strong> $($this.AnalysisResults.SecurityReport.Summary.UniqueIPs)</p>
        <p><strong>Suspicious Activities:</strong> $($this.AnalysisResults.SecurityReport.Summary.SuspiciousActivities)</p>
        <p><strong>Bots Detected:</strong> $($this.AnalysisResults.SecurityReport.Summary.BotsDetected)</p>
    </div>
"@

        # Секция угроз
        if ($this.AnalysisResults.SuspiciousActivity.Count -gt 0) {
            $htmlReport += @"
            <div class="section critical">
                <h2>🚨 Suspicious Activity</h2>
                <table>
                    <tr><th>Type</th><th>Client IP</th><th>URI</th><th>Time</th><th>Severity</th></tr>
"@
            foreach ($activity in $this.AnalysisResults.SuspiciousActivity | Select-Object -First 20) {
                $htmlReport += "<tr><td>$($activity.Type)</td><td>$($activity.ClientIP)</td><td>$($activity.URI)</td><td>$($activity.DateTime)</td><td>$($activity.Severity)</td></tr>"
            }
            $htmlReport += "</table></div>"
        }

        # Секция ботов
        if ($this.AnalysisResults.BotsAndScanners.Count -gt 0) {
            $htmlReport += @"
            <div class="section warning">
                <h2>🤖 Bots & Scanners</h2>
                <table>
                    <tr><th>Client IP</th><th>User Agent</th><th>Type</th><th>Confidence</th></tr>
"@
            foreach ($bot in $this.AnalysisResults.BotsAndScanners | Select-Object -First 15) {
                $htmlReport += "<tr><td>$($bot.ClientIP)</td><td title='$($bot.UserAgent)'>$($bot.UserAgent.Substring(0, [math]::Min(50, $bot.UserAgent.Length)))...</td><td>$($bot.Type)</td><td>$($bot.Confidence)</td></tr>"
            }
            $htmlReport += "</table></div>"
        }

        # Рекомендации
        $htmlReport += @"
            <div class="section info">
                <h2>💡 Recommendations</h2>
                <ul>
"@
        foreach ($recommendation in $this.AnalysisResults.SecurityReport.Recommendations) {
            $htmlReport += "<li>$recommendation</li>"
        }
        $htmlReport += @"
                </ul>
            </div>
        </body>
        </html>
"@

        $htmlReport | Out-File -FilePath $outputPath -Encoding UTF8
        Write-Host "Security report exported: $outputPath" -ForegroundColor Cyan
    }
    
    [void]RunFullAnalysis() {
        Write-Host "Starting comprehensive IIS log analysis..." -ForegroundColor Green
        
        $this.ParseLogFile()
        $this.DetectSuspiciousActivity()
        $this.AnalyzeAccessPatterns()
        $this.DetectBotsAndScanners()
        $this.GenerateSecurityReport()
        
        $reportFile = "IIS_Security_Report_$(Get-Date -Format 'yyyyMMdd_HHmmss').html"
        $this.ExportSecurityReport($reportFile)
        
        Write-Host "`nAnalysis completed!" -ForegroundColor Green
        Write-Host "Suspicious activities found: $($this.AnalysisResults.SuspiciousActivity.Count)" -ForegroundColor $(if ($this.AnalysisResults.SuspiciousActivity.Count -gt 0) { "Red" } else { "Green" })
        Write-Host "Bots/Scanners detected: $($this.AnalysisResults.BotsAndScanners.Count)" -ForegroundColor $(if ($this.AnalysisResults.BotsAndScanners.Count -gt 0) { "Red" } else { "Green" })
    }
}

# Основная программа
Write-Host "IIS Log Parser and Security Analyzer" -ForegroundColor Green
Write-Host "=====================================" -ForegroundColor Green

$logPath = Read-Host "Enter path to IIS log file"

if (Test-Path $logPath) {
    $parser = [IISLogParser]::new($logPath)
    
    try {
        $parser.RunFullAnalysis()
    }
    catch {
        Write-Host "Error during analysis: $($_.Exception.Message)" -ForegroundColor Red
    }
} else {
    Write-Host "Log file not found: $logPath" -ForegroundColor Red
    Write-Host "`nTypical IIS log locations:" -ForegroundColor Yellow
    Write-Host "• C:\inetpub\logs\LogFiles\W3SVC1\" -ForegroundColor White
    Write-Host "• C:\Windows\System32\LogFiles\W3SVC1\" -ForegroundColor White
}
