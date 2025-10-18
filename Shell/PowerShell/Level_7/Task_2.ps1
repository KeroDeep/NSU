# Валидатор конфигураций
Write-Host "=== Configuration Validator ===" -ForegroundColor Green

class ConfigurationValidator {
    [string]$ValidationRulesPath
    
    ConfigurationValidator() {
        $this.ValidationRulesPath = "ValidationRules.json"
        $this.InitializeDefaultRules()
    }
    
    [void]InitializeDefaultRules() {
        if (-not (Test-Path $this.ValidationRulesPath)) {
            $defaultRules = @{
                PowerShell = @{
                    ScriptSyntax = @{
                        Description = "Validate PowerShell script syntax"
                        Rules = @(
                            @{Pattern = "Get-WmiObject"; Message = "Consider using Get-CimInstance instead of Get-WmiObject"; Severity = "Warning"}
                            @{Pattern = "Invoke-Expression"; Message = "Avoid using Invoke-Expression for security"; Severity = "Error"}
                            @{Pattern = "Write-Host"; Message = "Consider using Write-Output instead of Write-Host"; Severity = "Info"}
                        )
                    }
                    BestPractices = @{
                        Description = "PowerShell best practices"
                        Rules = @(
                            @{Check = "CmdletBinding"; Message = "Add [CmdletBinding()] for advanced functions"; Severity = "Warning"}
                            @{Check = "ParameterValidation"; Message = "Use parameter validation attributes"; Severity = "Warning"}
                            @{Check = "ErrorHandling"; Message = "Implement proper error handling"; Severity = "Warning"}
                        )
                    }
                }
                XML = @{
                    SchemaValidation = @{
                        Description = "XML schema validation"
                        Rules = @(
                            @{Check = "WellFormed"; Message = "XML must be well-formed"; Severity = "Error"}
                            @{Check = "Encoding"; Message = "Specify proper encoding"; Severity = "Warning"}
                        )
                    }
                }
                JSON = @{
                    SyntaxValidation = @{
                        Description = "JSON syntax validation"
                        Rules = @(
                            @{Check = "ValidJSON"; Message = "JSON must be valid"; Severity = "Error"}
                            @{Check = "Encoding"; Message = "Use UTF-8 encoding"; Severity = "Warning"}
                        )
                    }
                }
                Network = @{
                    DNSValidation = @{
                        Description = "DNS name validation"
                        Rules = @(
                            @{Pattern = "^[a-zA-Z0-9]([a-zA-Z0-9\-]{0,61}[a-zA-Z0-9])?(\.[a-zA-Z0-9]([a-zA-Z0-9\-]{0,61}[a-zA-Z0-9])?)*$"; Message = "Invalid DNS name format"; Severity = "Error"}
                        )
                    }
                    IPValidation = @{
                        Description = "IP address validation"
                        Rules = @(
                            @{Pattern = "^(?:[0-9]{1,3}\.){3}[0-9]{1,3}$"; Message = "Invalid IP address format"; Severity = "Error"}
                        )
                    }
                }
                FileSystem = @{
                    PathValidation = @{
                        Description = "File path validation"
                        Rules = @(
                            @{Pattern = "[<>:""|?*]"; Message = "Invalid characters in path"; Severity = "Error"}
                            @{Check = "PathLength"; Message = "Path exceeds maximum length"; Severity = "Warning"}
                        )
                    }
                    PermissionValidation = @{
                        Description = "File permission checks"
                        Rules = @(
                            @{Check = "ReadAccess"; Message = "Verify read permissions"; Severity = "Warning"}
                            @{Check = "WriteAccess"; Message = "Verify write permissions"; Severity = "Warning"}
                        )
                    }
                }
            }
            
            $defaultRules | ConvertTo-Json -Depth 10 | Set-Content $this.ValidationRulesPath
            Write-Host "Default validation rules created" -ForegroundColor Green
        }
    }
    
    [array]ValidatePowerShellScript([string]$scriptPath) {
        $results = @()
        
        if (-not (Test-Path $scriptPath)) {
            return @(@{Type = "Error"; Message = "Script file not found: $scriptPath"; Line = 0})
        }
        
        $scriptContent = Get-Content $scriptPath -Raw
        $lines = Get-Content $scriptPath
        
        Write-Host "Validating PowerShell script: $scriptPath" -ForegroundColor Yellow
        
        # Проверка синтаксиса
        try {
            $tokens = @()
            $errors = @()
            $null = [System.Management.Automation.PSParser]::Tokenize($scriptContent, [ref]$tokens, [ref]$errors)
            
            if ($errors.Count -gt 0) {
                foreach ($error in $errors) {
                    $results += @{
                        Type = "Error"
                        Message = $error.Message
                        Line = $error.Token.StartLine
                        Column = $error.Token.StartColumn
                        Severity = "Error"
                    }
                }
            }
        }
        catch {
            $results += @{
                Type = "Error"
                Message = "Script parsing failed: $($_.Exception.Message)"
                Line = 0
                Severity = "Error"
            }
        }
        
        # Применение правил валидации
        $rules = (Get-Content $this.ValidationRulesPath | ConvertFrom-Json).PowerShell
        
        foreach ($ruleCategory in $rules.PSObject.Properties) {
            $category = $ruleCategory.Name
            $ruleSet = $ruleCategory.Value
            
            Write-Host "  Applying $($ruleSet.Description)..." -ForegroundColor Gray
            
            foreach ($rule in $ruleSet.Rules) {
                if ($rule.Pattern) {
                    # Проверка по шаблону
                    $lineNumber = 1
                    foreach ($line in $lines) {
                        if ($line -match $rule.Pattern) {
                            $results += @{
                                Type = $category
                                Message = $rule.Message
                                Line = $lineNumber
                                Pattern = $rule.Pattern
                                Severity = $rule.Severity
                            }
                        }
                        $lineNumber++
                    }
                }
                elseif ($rule.Check) {
                    # Специальные проверки
                    switch ($rule.Check) {
                        "CmdletBinding" {
                            if ($scriptContent -notmatch "\[CmdletBinding\(\)\]" -and $scriptContent -match "function \w+-") {
                                $results += @{
                                    Type = $category
                                    Message = $rule.Message
                                    Line = 0
                                    Severity = $rule.Severity
                                }
                            }
                        }
                        "ErrorHandling" {
                            $hasErrorHandling = $scriptContent -match "try\s*\{|catch\s*\{|trap\s*\{"
                            if (-not $hasErrorHandling -and $scriptContent -match "function \w+-") {
                                $results += @{
                                    Type = $category
                                    Message = $rule.Message
                                    Line = 0
                                    Severity = $rule.Severity
                                }
                            }
                        }
                    }
                }
            }
        }
        
        return $results
    }
    
    [array]ValidateXML([string]$xmlPath) {
        $results = @()
        
        if (-not (Test-Path $xmlPath)) {
            return @(@{Type = "Error"; Message = "XML file not found: $xmlPath"})
        }
        
        Write-Host "Validating XML file: $xmlPath" -ForegroundColor Yellow
        
        try {
            $xmlContent = Get-Content $xmlPath -Raw
            $xmlDoc = [xml]$xmlContent
            
            # Проверка well-formed XML
            $rules = (Get-Content $this.ValidationRulesPath | ConvertFrom-Json).XML
            
            foreach ($ruleCategory in $rules.PSObject.Properties) {
                $category = $ruleCategory.Name
                $ruleSet = $ruleCategory.Value
                
                foreach ($rule in $ruleSet.Rules) {
                    if ($rule.Check -eq "Encoding" -and $xmlContent -notmatch 'encoding="utf-8"') {
                        $results += @{
                            Type = $category
                            Message = $rule.Message
                            Severity = $rule.Severity
                        }
                    }
                }
            }
            
            Write-Host "  ✓ XML is well-formed" -ForegroundColor Green
        }
        catch {
            $results += @{
                Type = "Error"
                Message = "XML validation failed: $($_.Exception.Message)"
                Severity = "Error"
            }
        }
        
        return $results
    }
    
    [array]ValidateJSON([string]$jsonPath) {
        $results = @()
        
        if (-not (Test-Path $jsonPath)) {
            return @(@{Type = "Error"; Message = "JSON file not found: $jsonPath"})
        }
        
        Write-Host "Validating JSON file: $jsonPath" -ForegroundColor Yellow
        
        try {
            $jsonContent = Get-Content $jsonPath -Raw
            $null = $jsonContent | ConvertFrom-Json
            
            $rules = (Get-Content $this.ValidationRulesPath | ConvertFrom-Json).JSON
            
            foreach ($ruleCategory in $rules.PSObject.Properties) {
                $category = $ruleCategory.Name
                $ruleSet = $ruleCategory.Value
                
                foreach ($rule in $ruleSet.Rules) {
                    if ($rule.Check -eq "Encoding") {
                        # Простая проверка кодировки
                        $encoding = [System.Text.Encoding]::UTF8
                        $fileBytes = [System.IO.File]::ReadAllBytes($jsonPath)
                        $fileContent = $encoding.GetString($fileBytes)
                        
                        if ($fileContent -ne $jsonContent) {
                            $results += @{
                                Type = $category
                                Message = $rule.Message
                                Severity = $rule.Severity
                            }
                        }
                    }
                }
            }
            
            Write-Host "  ✓ JSON is valid" -ForegroundColor Green
        }
        catch {
            $results += @{
                Type = "Error"
                Message = "JSON validation failed: $($_.Exception.Message)"
                Severity = "Error"
            }
        }
        
        return $results
    }
    
    [array]ValidateDNS([string]$dnsName) {
        $results = @()
        
        Write-Host "Validating DNS name: $dnsName" -ForegroundColor Yellow
        
        $rules = (Get-Content $this.ValidationRulesPath | ConvertFrom-Json).Network.DNSValidation
        
        foreach ($rule in $rules.Rules) {
            if ($rule.Pattern -and $dnsName -notmatch $rule.Pattern) {
                $results += @{
                    Type = "DNSValidation"
                    Message = $rule.Message
                    Value = $dnsName
                    Severity = $rule.Severity
                }
            }
        }
        
        # Дополнительная проверка резолвинга
        try {
            $ipAddress = [System.Net.Dns]::GetHostAddresses($dnsName)
            if ($ipAddress) {
                Write-Host "  ✓ DNS name resolves successfully" -ForegroundColor Green
            }
        }
        catch {
            $results += @{
                Type = "DNSResolution"
                Message = "DNS name does not resolve: $($_.Exception.Message)"
                Value = $dnsName
                Severity = "Warning"
            }
        }
        
        return $results
    }
    
    [array]ValidateIPAddress([string]$ipAddress) {
        $results = @()
        
        Write-Host "Validating IP address: $ipAddress" -ForegroundColor Yellow
        
        $rules = (Get-Content $this.ValidationRulesPath | ConvertFrom-Json).Network.IPValidation
        
        foreach ($rule in $rules.Rules) {
            if ($rule.Pattern -and $ipAddress -notmatch $rule.Pattern) {
                $results += @{
                    Type = "IPValidation"
                    Message = $rule.Message
                    Value = $ipAddress
                    Severity = $rule.Severity
                }
                return $results
            }
        }
        
        # Проверка корректности октетов
        $octets = $ipAddress -split "\."
        foreach ($octet in $octets) {
            if ([int]$octet -gt 255) {
                $results += @{
                    Type = "IPValidation"
                    Message = "Invalid IP address: octet value too large ($octet)"
                    Value = $ipAddress
                    Severity = "Error"
                }
                break
            }
        }
        
        if ($results.Count -eq 0) {
            Write-Host "  ✓ IP address is valid" -ForegroundColor Green
        }
        
        return $results
    }
    
    [array]ValidateFilePath([string]$filePath) {
        $results = @()
        
        Write-Host "Validating file path: $filePath" -ForegroundColor Yellow
        
        $rules = (Get-Content $this.ValidationRulesPath | ConvertFrom-Json).FileSystem.PathValidation
        
        foreach ($rule in $rules.Rules) {
            if ($rule.Pattern -and $filePath -match $rule.Pattern) {
                $results += @{
                    Type = "PathValidation"
                    Message = $rule.Message
                    Value = $filePath
                    Severity = $rule.Severity
                }
            }
            elseif ($rule.Check -eq "PathLength" -and $filePath.Length -gt 260) {
                $results += @{
                    Type = "PathValidation"
                    Message = $rule.Message
                    Value = $filePath
                    Severity = $rule.Severity
                }
            }
        }
        
        # Проверка существования пути
        if (-not (Test-Path (Split-Path $filePath -Parent))) {
            $results += @{
                Type = "PathExistence"
                Message = "Parent directory does not exist"
                Value = $filePath
                Severity = "Warning"
            }
        }
        
        if ($results.Count -eq 0) {
            Write-Host "  ✓ File path is valid" -ForegroundColor Green
        }
        
        return $results
    }
    
    [void]GenerateValidationReport([array]$allResults, [string]$outputPath) {
        $report = @{
            Summary = @{
                TotalChecks = $allResults.Count
                Errors = ($allResults | Where-Object { $_.Severity -eq "Error" }).Count
                Warnings = ($allResults | Where-Object { $_.Severity -eq "Warning" }).Count
                Info = ($allResults | Where-Object { $_.Severity -eq "Info" }).Count
                Generated = Get-Date
            }
            Results = $allResults
        }
        
        # Группировка результатов по типу
        $groupedResults = $allResults | Group-Object Type
        
        $htmlReport = @"
<!DOCTYPE html>
<html>
<head>
    <title>Configuration Validation Report</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .header { background: #2c3e50; color: white; padding: 20px; border-radius: 5px; }
        .summary { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; margin: 20px 0; }
        .summary-item { text-align: center; padding: 15px; border-radius: 5px; color: white; }
        .errors { background: #e74c3c; }
        .warnings { background: #f39c12; }
        .info { background: #3498db; }
        .total { background: #27ae60; }
        .result-item { margin: 10px 0; padding: 10px; border-left: 4px solid; }
        .error { border-left-color: #e74c3c; background: #fadbd8; }
        .warning { border-left-color: #f39c12; background: #fef9e7; }
        .info { border-left-color: #3498db; background: #ebf5fb; }
    </style>
</head>
<body>
    <div class="header">
        <h1>🔍 Configuration Validation Report</h1>
        <p>Generated: $(Get-Date)</p>
    </div>
    
    <div class="summary">
        <div class="summary-item total">
            <h3>Total</h3>
            <p>$($report.Summary.TotalChecks)</p>
        </div>
        <div class="summary-item errors">
            <h3>Errors</h3>
            <p>$($report.Summary.Errors)</p>
        </div>
        <div class="summary-item warnings">
            <h3>Warnings</h3>
            <p>$($report.Summary.Warnings)</p>
        </div>
        <div class="summary-item info">
            <h3>Info</h3>
            <p>$($report.Summary.Info)</p>
        </div>
    </div>
"@

        foreach ($group in $groupedResults) {
            $htmlReport += "<h2>$($group.Name)</h2>"
            
            foreach ($result in $group.Group) {
                $severityClass = $result.Severity.ToLower()
                $lineInfo = if ($result.Line -and $result.Line -gt 0) { " (Line: $($result.Line))" } else { "" }
                
                $htmlReport += @"
                <div class="result-item $severityClass">
                    <strong>$($result.Severity):</strong> $($result.Message)$lineInfo
                    $(if ($result.Value) { "<br><small>Value: $($result.Value)</small>" } else { "" })
                </div>
"@
            }
        }

        $htmlReport += @"
</body>
</html>
"@

        $htmlReport | Out-File -FilePath $outputPath -Encoding UTF8
        Write-Host "Validation report generated: $outputPath" -ForegroundColor Cyan
    }
}

# Основная программа
Write-Host "Configuration Validator" -ForegroundColor Green
Write-Host "======================" -ForegroundColor Green

$validator = [ConfigurationValidator]::new()

function Show-MainMenu {
    Write-Host "`n=== Validation Options ===" -ForegroundColor Yellow
    Write-Host "1. Validate PowerShell Script"
    Write-Host "2. Validate XML File"
    Write-Host "3. Validate JSON File"
    Write-Host "4. Validate DNS Name"
    Write-Host "5. Validate IP Address"
    Write-Host "6. Validate File Path"
    Write-Host "7. Run All Validations (Demo)"
    Write-Host "8. Exit"
    
    return Read-Host "`nSelect option (1-8)"
}

$allResults = @()

do {
    $choice = Show-MainMenu
    
    switch ($choice) {
        "1" {
            $scriptPath = Read-Host "Enter PowerShell script path"
            $results = $validator.ValidatePowerShellScript($scriptPath)
            $allResults += $results
            
            if ($results.Count -eq 0) {
                Write-Host "✓ No issues found in PowerShell script" -ForegroundColor Green
            }
        }
        "2" {
            $xmlPath = Read-Host "Enter XML file path"
            $results = $validator.ValidateXML($xmlPath)
            $allResults += $results
        }
        "3" {
            $jsonPath = Read-Host "Enter JSON file path"
            $results = $validator.ValidateJSON($jsonPath)
            $allResults += $results
        }
        "4" {
            $dnsName = Read-Host "Enter DNS name"
            $results = $validator.ValidateDNS($dnsName)
            $allResults += $results
        }
        "5" {
            $ipAddress = Read-Host "Enter IP address"
            $results = $validator.ValidateIPAddress($ipAddress)
            $allResults += $results
        }
        "6" {
            $filePath = Read-Host "Enter file path"
            $results = $validator.ValidateFilePath($filePath)
            $allResults += $results
        }
        "7" {
            Write-Host "Running demo validations..." -ForegroundColor Yellow
            
            # Демонстрационные проверки
            $demoResults = @()
            
            # Создание тестового PowerShell скрипта
            $testScript = @'
function Test-Function {
    Get-WmiObject Win32_ComputerSystem
    Invoke-Expression "Get-Process"
    Write-Host "Hello World"
}
'@
            $testScriptPath = "test_script.ps1"
            $testScript | Set-Content $testScriptPath
            
            $demoResults += $validator.ValidatePowerShellScript($testScriptPath)
            $demoResults += $validator.ValidateDNS("example.com")
            $demoResults += $validator.ValidateIPAddress("192.168.1.1")
            $demoResults += $validator.ValidateFilePath("C:\Test\File.txt")
            
            $allResults += $demoResults
            
            # Очистка
            if (Test-Path $testScriptPath) { Remove-Item $testScriptPath }
            
            Write-Host "Demo validations completed" -ForegroundColor Green
        }
        "8" {
            Write-Host "Goodbye!" -ForegroundColor Green
        }
        default {
            Write-Host "Invalid option" -ForegroundColor Red
        }
    }
    
    if ($choice -ne "8") {
        # Показать текущие результаты
        if ($allResults.Count -gt 0) {
            $errors = ($allResults | Where-Object { $_.Severity -eq "Error" }).Count
            $warnings = ($allResults | Where-Object { $_.Severity -eq "Warning" }).Count
            
            Write-Host "`nCurrent validation results:" -ForegroundColor Yellow
            Write-Host "  Errors: $errors" -ForegroundColor $(if ($errors -gt 0) { "Red" } else { "Green" })
            Write-Host "  Warnings: $warnings" -ForegroundColor $(if ($warnings -gt 0) { "Yellow" } else { "Green" })
        }
        
        Read-Host "`nPress Enter to continue..."
    }
} while ($choice -ne "8")

# Генерация финального отчета
if ($allResults.Count -gt 0) {
    $reportChoice = Read-Host "`nGenerate validation report? (y/n)"
    if ($reportChoice -eq 'y') {
        $reportFile = "ValidationReport_$(Get-Date -Format 'yyyyMMdd_HHmmss').html"
        $validator.GenerateValidationReport($allResults, $reportFile)
    }
}
