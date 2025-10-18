# Анализатор безопасности
Write-Host "=== Security Analyzer ===" -ForegroundColor Green

class SecurityAnalyzer {
    [string]$ReportPath
    [hashtable]$AnalysisResults
    
    SecurityAnalyzer() {
        $this.ReportPath = "SecurityAudit_$(Get-Date -Format 'yyyyMMdd_HHmmss').html"
        $this.AnalysisResults = @{
            PasswordPolicy = @()
            FilePermissions = @()
            FirewallSettings = @()
            SecurityLogs = @()
            Compliance = @()
        }
    }
    
    [void]AnalyzePasswordPolicy() {
        Write-Host "Analyzing password policies..." -ForegroundColor Yellow
        
        try {
            $passwordPolicy = Get-ADDefaultDomainPasswordPolicy -ErrorAction Stop
            
            $this.AnalysisResults.PasswordPolicy = @(
                @{Check = "Minimum Password Length"; Value = $passwordPolicy.MinPasswordLength; Recommended = 8; Status = if ($passwordPolicy.MinPasswordLength -ge 8) { "PASS" } else { "FAIL" }},
                @{Check = "Password Complexity"; Value = $passwordPolicy.ComplexityEnabled; Recommended = $true; Status = if ($passwordPolicy.ComplexityEnabled) { "PASS" } else { "FAIL" }},
                @{Check = "Password History"; Value = $passwordPolicy.PasswordHistoryCount; Recommended = 24; Status = if ($passwordPolicy.PasswordHistoryCount -ge 12) { "PASS" } else { "FAIL" }},
                @{Check = "Maximum Password Age"; Value = $passwordPolicy.MaxPasswordAge.Days; Recommended = 90; Status = if ($passwordPolicy.MaxPasswordAge.Days -le 90) { "PASS" } else { "FAIL" }},
                @{Check = "Minimum Password Age"; Value = $passwordPolicy.MinPasswordAge.Days; Recommended = 1; Status = if ($passwordPolicy.MinPasswordAge.Days -ge 1) { "PASS" } else { "FAIL" }},
                @{Check = "Account Lockout Threshold"; Value = $passwordPolicy.LockoutThreshold; Recommended = 5; Status = if ($passwordPolicy.LockoutThreshold -le 5 -and $passwordPolicy.LockoutThreshold -gt 0) { "PASS" } else { "FAIL" }},
                @{Check = "Account Lockout Duration"; Value = $passwordPolicy.LockoutDuration.Minutes; Recommended = 30; Status = if ($passwordPolicy.LockoutDuration.Minutes -ge 30) { "PASS" } else { "FAIL" }}
            )
            
            Write-Host "✓ Password policy analysis completed" -ForegroundColor Green
        }
        catch {
            Write-Host "✗ Could not analyze password policy: $($_.Exception.Message)" -ForegroundColor Red
        }
    }
    
    [void]AnalyzeFilePermissions() {
        Write-Host "Analyzing critical file permissions..." -ForegroundColor Yellow
        
        $criticalPaths = @(
            "C:\Windows\System32",
            "C:\Windows\SysWOW64", 
            "C:\Program Files",
            "C:\ProgramData",
            "C:\Users"
        )
        
        $permissionIssues = @()
        
        foreach ($path in $criticalPaths) {
            if (Test-Path $path) {
                try {
                    $acl = Get-Acl $path
                    $accessRules = $acl.Access
                    
                    # Проверка на слишком открытые разрешения
                    $everyoneAccess = $accessRules | Where-Object { 
                        $_.IdentityReference -eq "Everyone" -and $_.FileSystemRights -match "FullControl|Write"
                    }
                    
                    if ($everyoneAccess) {
                        $permissionIssues += @{
                            Path = $path
                            Issue = "Everyone has excessive permissions: $($everyoneAccess.FileSystemRights)"
                            Severity = "HIGH"
                        }
                    }
                    
                    # Проверка на анонимный доступ
                    $anonymousAccess = $accessRules | Where-Object { 
                        $_.IdentityReference -like "*ANONYMOUS*" -or $_.IdentityReference -like "*Guest*"
                    }
                    
                    if ($anonymousAccess) {
                        $permissionIssues += @{
                            Path = $path
                            Issue = "Anonymous/Guest access detected"
                            Severity = "HIGH"
                        }
                    }
                }
                catch {
                    Write-Host "  Could not analyze permissions for: $path" -ForegroundColor Yellow
                }
            }
        }
        
        $this.AnalysisResults.FilePermissions = $permissionIssues
        Write-Host "✓ File permission analysis completed" -ForegroundColor Green
    }
    
    [void]AnalyzeFirewallSettings() {
        Write-Host "Analyzing Windows Firewall settings..." -ForegroundColor Yellow
        
        $firewallIssues = @()
        
        try {
            $firewallProfiles = Get-NetFirewallProfile
            
            foreach ($profile in $firewallProfiles) {
                if (-not $profile.Enabled) {
                    $firewallIssues += @{
                        Profile = $profile.Name
                        Issue = "Firewall is disabled"
                        Severity = "HIGH"
                    }
                }
                
                if ($profile.DefaultInboundAction -eq "Allow") {
                    $firewallIssues += @{
                        Profile = $profile.Name
                        Issue = "Default inbound action is set to ALLOW"
                        Severity = "MEDIUM"
                    }
                }
            }
            
            # Проверка открытых портов
            $openPorts = Get-NetFirewallRule | Where-Object { 
                $_.Enabled -eq $true -and $_.Direction -eq "Inbound" -and $_.Action -eq "Allow"
            }
            
            $dangerousPorts = @(21, 23, 135, 139, 445, 3389)
            foreach ($rule in $openPorts) {
                if ($rule.LocalPort -in $dangerousPorts) {
                    $firewallIssues += @{
                        Profile = "All"
                        Issue = "Dangerous port $($rule.LocalPort) is open ($($rule.DisplayName))"
                        Severity = "HIGH"
                    }
                }
            }
            
            $this.AnalysisResults.FirewallSettings = $firewallIssues
            Write-Host "✓ Firewall analysis completed" -ForegroundColor Green
        }
        catch {
            Write-Host "✗ Could not analyze firewall settings: $($_.Exception.Message)" -ForegroundColor Red
        }
    }
    
    [void]AnalyzeSecurityLogs() {
        Write-Host "Analyzing security event logs..." -ForegroundColor Yellow
        
        $securityEvents = @()
        
        try {
            # Поиск критических событий безопасности за последние 7 дней
            $startTime = (Get-Date).AddDays(-7)
            
            $criticalEvents = Get-WinEvent -LogName "Security" -MaxEvents 1000 -ErrorAction SilentlyContinue | 
                            Where-Object { $_.TimeCreated -ge $startTime -and $_.LevelDisplayName -eq "Error" }
            
            $eventCounts = $criticalEvents | Group-Object Id | Sort-Object Count -Descending | Select-Object -First 10
            
            foreach ($eventGroup in $eventCounts) {
                $securityEvents += @{
                    EventID = $eventGroup.Name
                    Count = $eventGroup.Count
                    Description = $this.GetEventDescription($eventGroup.Name)
                    Severity = "HIGH"
                }
            }
            
            # Проверка на атаки brute force
            $failedLogons = Get-WinEvent -FilterHashtable @{LogName='Security'; ID=4625; StartTime=$startTime} -ErrorAction SilentlyContinue
            if ($failedLogons.Count -gt 100) {
                $securityEvents += @{
                    EventID = "BRUTE_FORCE"
                    Count = $failedLogons.Count
                    Description = "Possible brute force attack detected - $($failedLogons.Count) failed logons"
                    Severity = "CRITICAL"
                }
            }
            
            $this.AnalysisResults.SecurityLogs = $securityEvents
            Write-Host "✓ Security log analysis completed" -ForegroundColor Green
        }
        catch {
            Write-Host "✗ Could not analyze security logs: $($_.Exception.Message)" -ForegroundColor Red
        }
    }
    
    [string]GetEventDescription([int]$eventId) {
        $eventDescriptions = @{
            4625 = "Failed logon attempt"
            4648 = "Logon with explicit credentials"
            4672 = "Special privileges assigned to new logon"
            4720 = "User account created"
            4732 = "Member added to security-enabled global group"
            1102 = "Audit log was cleared"
        }
        
        return $eventDescriptions[$eventId] ?? "Unknown security event"
    }
    
    [void]GenerateComplianceReport() {
        Write-Host "Generating compliance report..." -ForegroundColor Yellow
        
        $complianceScore = 100
        $complianceIssues = @()
        
        # Оценка политики паролей
        $passwordFailures = $this.AnalysisResults.PasswordPolicy | Where-Object { $_.Status -eq "FAIL" }
        if ($passwordFailures) {
            $complianceScore -= 20
            $complianceIssues += "Password policy does not meet security standards ($($passwordFailures.Count) issues)"
        }
        
        # Оценка разрешений файлов
        if ($this.AnalysisResults.FilePermissions.Count -gt 0) {
            $complianceScore -= 25
            $highSeverity = ($this.AnalysisResults.FilePermissions | Where-Object { $_.Severity -eq "HIGH" }).Count
            $complianceIssues += "File permission issues detected ($highSeverity high severity)"
        }
        
        # Оценка брандмауэра
        $firewallFailures = $this.AnalysisResults.FirewallSettings | Where-Object { $_.Severity -eq "HIGH" }
        if ($firewallFailures) {
            $complianceScore -= 30
            $complianceIssues += "Critical firewall issues detected"
        }
        
        # Оценка логов безопасности
        $criticalEvents = $this.AnalysisResults.SecurityLogs | Where-Object { $_.Severity -eq "CRITICAL" }
        if ($criticalEvents) {
            $complianceScore -= 25
            $complianceIssues += "Critical security events detected"
        }
        
        $this.AnalysisResults.Compliance = @{
            Score = [math]::Max($complianceScore, 0)
            Grade = if ($complianceScore -ge 90) { "A" } elseif ($complianceScore -ge 80) { "B" } elseif ($complianceScore -ge 70) { "C" } elseif ($complianceScore -ge 60) { "D" } else { "F" }
            Issues = $complianceIssues
            Timestamp = Get-Date
        }
    }
    
    [void]GenerateHTMLReport() {
        Write-Host "Generating HTML security report..." -ForegroundColor Yellow
        
        $html = @"
<!DOCTYPE html>
<html>
<head>
    <title>Security Audit Report</title>
    <style>
        body { font-family: 'Segoe UI', Arial, sans-serif; margin: 20px; background: #f5f5f5; }
        .container { max-width: 1200px; margin: 0 auto; }
        .header { background: #2c3e50; color: white; padding: 20px; border-radius: 5px; }
        .section { background: white; margin: 20px 0; padding: 20px; border-radius: 5px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        .compliance-score { text-align: center; padding: 30px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; border-radius: 10px; }
        .score-number { font-size: 48px; font-weight: bold; }
        .score-grade { font-size: 24px; margin-top: 10px; }
        .status-pass { color: #27ae60; font-weight: bold; }
        .status-fail { color: #e74c3c; font-weight: bold; }
        .severity-high { background: #e74c3c; color: white; padding: 2px 8px; border-radius: 3px; }
        .severity-medium { background: #f39c12; color: white; padding: 2px 8px; border-radius: 3px; }
        .severity-critical { background: #c0392b; color: white; padding: 2px 8px; border-radius: 3px; }
        table { width: 100%; border-collapse: collapse; margin: 10px 0; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background: #34495e; color: white; }
        tr:nth-child(even) { background: #f8f9fa; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>🛡️ Security Audit Report</h1>
            <p>Generated on: $(Get-Date)</p>
        </div>
        
        <div class="compliance-score">
            <div class="score-number">$($this.AnalysisResults.Compliance.Score)/100</div>
            <div class="score-grade">Grade: $($this.AnalysisResults.Compliance.Grade)</div>
        </div>
"@

        # Отчет о политике паролей
        $html += @"
        <div class="section">
            <h2>🔐 Password Policy Analysis</h2>
            <table>
                <tr><th>Check</th><th>Current Value</th><th>Recommended</th><th>Status</th></tr>
"@
        foreach ($check in $this.AnalysisResults.PasswordPolicy) {
            $statusClass = if ($check.Status -eq "PASS") { "status-pass" } else { "status-fail" }
            $html += "<tr><td>$($check.Check)</td><td>$($check.Value)</td><td>$($check.Recommended)</td><td class='$statusClass'>$($check.Status)</td></tr>"
        }
        $html += "</table></div>"

        # Отчет о разрешениях файлов
        $html += @"
        <div class="section">
            <h2>📁 File Permission Analysis</h2>
"@
        if ($this.AnalysisResults.FilePermissions.Count -gt 0) {
            $html += "<table><tr><th>Path</th><th>Issue</th><th>Severity</th></tr>"
            foreach ($issue in $this.AnalysisResults.FilePermissions) {
                $html += "<tr><td>$($issue.Path)</td><td>$($issue.Issue)</td><td><span class='severity-$($issue.Severity.ToLower())'>$($issue.Severity)</span></td></tr>"
            }
            $html += "</table>"
        } else {
            $html += "<p>No critical file permission issues found.</p>"
        }
        $html += "</div>"

        # Отчет о брандмауэре
        $html += @"
        <div class="section">
            <h2>🔥 Firewall Analysis</h2>
"@
        if ($this.AnalysisResults.FirewallSettings.Count -gt 0) {
            $html += "<table><tr><th>Profile</th><th>Issue</th><th>Severity</th></tr>"
            foreach ($issue in $this.AnalysisResults.FirewallSettings) {
                $html += "<tr><td>$($issue.Profile)</td><td>$($issue.Issue)</td><td><span class='severity-$($issue.Severity.ToLower())'>$($issue.Severity)</span></td></tr>"
            }
            $html += "</table>"
        } else {
            $html += "<p>No critical firewall issues found.</p>"
        }
        $html += "</div>"

        # Отчет о логах безопасности
        $html += @"
        <div class="section">
            <h2>📊 Security Log Analysis</h2>
"@
        if ($this.AnalysisResults.SecurityLogs.Count -gt 0) {
            $html += "<table><tr><th>Event ID</th><th>Count</th><th>Description</th><th>Severity</th></tr>"
            foreach ($event in $this.AnalysisResults.SecurityLogs) {
                $html += "<tr><td>$($event.EventID)</td><td>$($event.Count)</td><td>$($event.Description)</td><td><span class='severity-$($event.Severity.ToLower())'>$($event.Severity)</span></td></tr>"
            }
            $html += "</table>"
        } else {
            $html += "<p>No critical security events detected.</p>"
        }
        $html += "</div>"

        # Проблемы соответствия
        $html += @"
        <div class="section">
            <h2>📋 Compliance Issues</h2>
            <ul>
"@
        foreach ($issue in $this.AnalysisResults.Compliance.Issues) {
            $html += "<li>$issue</li>"
        }
        $html += @"
            </ul>
        </div>
    </div>
</body>
</html>
"@

        $html | Out-File -FilePath $this.ReportPath -Encoding UTF8
        Write-Host "Security report generated: $($this.ReportPath)" -ForegroundColor Cyan
    }
    
    [void]RunFullAnalysis() {
        Write-Host "Starting comprehensive security analysis..." -ForegroundColor Green
        Write-Host "This may take several minutes...`n" -ForegroundColor Yellow
        
        $this.AnalyzePasswordPolicy()
        $this.AnalyzeFilePermissions() 
        $this.AnalyzeFirewallSettings()
        $this.AnalyzeSecurityLogs()
        $this.GenerateComplianceReport()
        $this.GenerateHTMLReport()
        
        Write-Host "`nSecurity analysis completed!" -ForegroundColor Green
        Write-Host "Compliance Score: $($this.AnalysisResults.Compliance.Score)/100 ($($this.AnalysisResults.Compliance.Grade))" -ForegroundColor Cyan
    }
}

# Основная программа
Write-Host "Security Analyzer" -ForegroundColor Green
Write-Host "================" -ForegroundColor Green

$analyzer = [SecurityAnalyzer]::new()

Write-Host "This tool will analyze:" -ForegroundColor Yellow
Write-Host "• Password policies" -ForegroundColor White
Write-Host "• File and folder permissions" -ForegroundColor White
Write-Host "• Windows Firewall settings" -ForegroundColor White
Write-Host "• Security event logs" -ForegroundColor White
Write-Host "• Overall security compliance`n" -ForegroundColor White

$confirm = Read-Host "Start security analysis? (y/n)"
if ($confirm -eq 'y' -or $confirm -eq 'Y') {
    $analyzer.RunFullAnalysis()
} else {
    Write-Host "Analysis cancelled" -ForegroundColor Yellow
}
