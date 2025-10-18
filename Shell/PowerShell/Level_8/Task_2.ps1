# Фреймворк автоматического тестирования
Write-Host "=== PowerShell Test Framework ===" -ForegroundColor Green

class TestFramework {
    [string]$TestResultsPath
    [System.Collections.ArrayList]$Tests
    [hashtable]$TestResults
    [string]$CoverageReportPath
    
    TestFramework() {
        $this.TestResultsPath = "TestResults_$(Get-Date -Format 'yyyyMMdd_HHmmss').html"
        $this.Tests = [System.Collections.ArrayList]::new()
        $this.TestResults = @{}
        $this.CoverageReportPath = "CoverageReport_$(Get-Date -Format 'yyyyMMdd_HHmmss').html"
    }
    
    [void]AddTest([string]$name, [scriptblock]$testScript, [string]$category = "General") {
        $test = @{
            Id = [guid]::NewGuid().ToString()
            Name = $name
            Category = $category
            Script = $testScript
            Status = "NotRun"
            Duration = 0
            Error = $null
            StackTrace = $null
        }
        
        $this.Tests.Add($test) | Out-Null
        Write-Host "Test added: $name ($category)" -ForegroundColor Cyan
    }
    
    [void]RunUnitTests() {
        Write-Host "Running Unit Tests..." -ForegroundColor Yellow
        Write-Host "====================" -ForegroundColor Yellow
        
        $testCount = 0
        $passCount = 0
        $failCount = 0
        
        foreach ($test in $this.Tests) {
            if ($test.Category -eq "Unit") {
                $testCount++
                Write-Host "Running: $($test.Name)" -NoNewline
                
                $startTime = Get-Date
                try {
                    $null = & $test.Script
                    $test.Status = "Passed"
                    $passCount++
                    Write-Host " - ✓ PASSED" -ForegroundColor Green
                }
                catch {
                    $test.Status = "Failed"
                    $test.Error = $_.Exception.Message
                    $test.StackTrace = $_.ScriptStackTrace
                    $failCount++
                    Write-Host " - ✗ FAILED" -ForegroundColor Red
                    Write-Host "    Error: $($_.Exception.Message)" -ForegroundColor Red
                }
                finally {
                    $test.Duration = ((Get-Date) - $startTime).TotalSeconds
                }
            }
        }
        
        Write-Host "`nUnit Tests Summary: $passCount/$testCount passed" -ForegroundColor $(if ($failCount -eq 0) { "Green" } else { "Red" })
    }
    
    [void]RunIntegrationTests() {
        Write-Host "`nRunning Integration Tests..." -ForegroundColor Yellow
        Write-Host "===========================" -ForegroundColor Yellow
        
        $testCount = 0
        $passCount = 0
        $failCount = 0
        
        foreach ($test in $this.Tests) {
            if ($test.Category -eq "Integration") {
                $testCount++
                Write-Host "Running: $($test.Name)" -NoNewline
                
                $startTime = Get-Date
                try {
                    $null = & $test.Script
                    $test.Status = "Passed"
                    $passCount++
                    Write-Host " - ✓ PASSED" -ForegroundColor Green
                }
                catch {
                    $test.Status = "Failed"
                    $test.Error = $_.Exception.Message
                    $test.StackTrace = $_.ScriptStackTrace
                    $failCount++
                    Write-Host " - ✗ FAILED" -ForegroundColor Red
                    Write-Host "    Error: $($_.Exception.Message)" -ForegroundColor Red
                }
                finally {
                    $test.Duration = ((Get-Date) - $startTime).TotalSeconds
                }
            }
        }
        
        Write-Host "`nIntegration Tests Summary: $passCount/$testCount passed" -ForegroundColor $(if ($failCount -eq 0) { "Green" } else { "Red" })
    }
    
    [void]RunAllTests() {
        Write-Host "Running All Tests..." -ForegroundColor Yellow
        Write-Host "===================" -ForegroundColor Yellow
        
        $totalTests = $this.Tests.Count
        $currentTest = 0
        
        foreach ($test in $this.Tests) {
            $currentTest++
            Write-Progress -Activity "Running Tests" -Status "Executing: $($test.Name)" -PercentComplete (($currentTest / $totalTests) * 100)
            
            Write-Host "Running: $($test.Name) [$($test.Category)]" -NoNewline
            
            $startTime = Get-Date
            try {
                $null = & $test.Script
                $test.Status = "Passed"
                Write-Host " - ✓ PASSED" -ForegroundColor Green
            }
            catch {
                $test.Status = "Failed"
                $test.Error = $_.Exception.Message
                $test.StackTrace = $_.ScriptStackTrace
                Write-Host " - ✗ FAILED" -ForegroundColor Red
            }
            finally {
                $test.Duration = ((Get-Date) - $startTime).TotalSeconds
            }
        }
        
        Write-Progress -Activity "Running Tests" -Completed
        
        $this.GenerateTestReport()
        $this.CalculateCoverage()
    }
    
    [void]GenerateTestReport() {
        Write-Host "`nGenerating Test Report..." -ForegroundColor Yellow
        
        $passedTests = ($this.Tests | Where-Object { $_.Status -eq "Passed" }).Count
        $failedTests = ($this.Tests | Where-Object { $_.Status -eq "Failed" }).Count
        $totalTests = $this.Tests.Count
        
        $report = @{
            Summary = @{
                TotalTests = $totalTests
                Passed = $passedTests
                Failed = $failedTests
                SuccessRate = if ($totalTests -gt 0) { [math]::Round(($passedTests / $totalTests) * 100, 2) } else { 0 }
                ExecutionTime = Get-Date
            }
            Tests = $this.Tests
        }
        
        $htmlReport = @"
<!DOCTYPE html>
<html>
<head>
    <title>PowerShell Test Report</title>
    <style>
        body { font-family: 'Segoe UI', Arial, sans-serif; margin: 20px; background: #f5f5f5; }
        .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; border-radius: 10px; margin-bottom: 20px; }
        .summary { display: grid; grid-template-columns: repeat(4, 1fr); gap: 15px; margin: 20px 0; }
        .summary-card { background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); text-align: center; }
        .summary-number { font-size: 2em; font-weight: bold; margin: 10px 0; }
        .test-results { background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        table { width: 100%; border-collapse: collapse; margin: 10px 0; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background: #34495e; color: white; }
        .test-passed { background: #d4edda; }
        .test-failed { background: #f8d7da; }
        .status-passed { color: #27ae60; font-weight: bold; }
        .status-failed { color: #e74c3c; font-weight: bold; }
    </style>
</head>
<body>
    <div class="header">
        <h1>🧪 PowerShell Test Report</h1>
        <p>Generated: $(Get-Date)</p>
    </div>
    
    <div class="summary">
        <div class="summary-card">
            <h3>Total Tests</h3>
            <div class="summary-number">$totalTests</div>
        </div>
        <div class="summary-card">
            <h3>Passed</h3>
            <div class="summary-number" style="color: #27ae60;">$passedTests</div>
        </div>
        <div class="summary-card">
            <h3>Failed</h3>
            <div class="summary-number" style="color: #e74c3c;">$failedTests</div>
        </div>
        <div class="summary-card">
            <h3>Success Rate</h3>
            <div class="summary-number">$([math]::Round(($passedTests / $totalTests) * 100, 2))%</div>
        </div>
    </div>
    
    <div class="test-results">
        <h2>Test Results</h2>
        <table>
            <tr><th>Test Name</th><th>Category</th><th>Status</th><th>Duration (s)</th><th>Error</th></tr>
"@

        foreach ($test in $this.Tests) {
            $rowClass = if ($test.Status -eq "Passed") { "test-passed" } else { "test-failed" }
            $statusClass = "status-" + $test.Status.ToLower()
            
            $htmlReport += "<tr class='$rowClass'>"
            $htmlReport += "<td>$($test.Name)</td>"
            $htmlReport += "<td>$($test.Category)</td>"
            $htmlReport += "<td class='$statusClass'>$($test.Status)</td>"
            $htmlReport += "<td>$([math]::Round($test.Duration, 3))</td>"
            $htmlReport += "<td>$(if ($test.Error) { $test.Error } else { '-' })</td>"
            $htmlReport += "</tr>"
        }

        $htmlReport += @"
        </table>
    </div>
</body>
</html>
"@

        $htmlReport | Out-File -FilePath $this.TestResultsPath -Encoding UTF8
        Write-Host "Test report generated: $($this.TestResultsPath)" -ForegroundColor Cyan
    }
    
    [void]CalculateCoverage() {
        Write-Host "Calculating code coverage..." -ForegroundColor Yellow
        
        # Простой анализ покрытия кода
        $coverageData = @{
            TotalFunctions = 0
            TestedFunctions = 0
            CoveragePercentage = 0
            Functions = @()
        }
        
        # Поиск функций в текущей сессии
        $allFunctions = Get-ChildItem Function:\ | Where-Object { $_.Name -notlike "*:*" -and $_.Name -notlike "^*" }
        $coverageData.TotalFunctions = $allFunctions.Count
        
        foreach ($function in $allFunctions) {
            $functionInfo = @{
                Name = $function.Name
                Tested = $false
                TestReferences = @()
            }
            
            # Проверка, тестируется ли функция в наших тестах
            foreach ($test in $this.Tests) {
                if ($test.Script.ToString() -match $function.Name) {
                    $functionInfo.Tested = $true
                    $functionInfo.TestReferences += $test.Name
                }
            }
            
            if ($functionInfo.Tested) {
                $coverageData.TestedFunctions++
            }
            
            $coverageData.Functions += $functionInfo
        }
        
        $coverageData.CoveragePercentage = if ($coverageData.TotalFunctions -gt 0) {
            [math]::Round(($coverageData.TestedFunctions / $coverageData.TotalFunctions) * 100, 2)
        } else {
            0
        }
        
        $this.GenerateCoverageReport($coverageData)
    }
    
    [void]GenerateCoverageReport([hashtable]$coverageData) {
        $htmlCoverage = @"
<!DOCTYPE html>
<html>
<head>
    <title>Code Coverage Report</title>
    <style>
        body { font-family: 'Segoe UI', Arial, sans-serif; margin: 20px; background: #f5f5f5; }
        .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; border-radius: 10px; margin-bottom: 20px; }
        .coverage-summary { background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); margin: 20px 0; }
        .coverage-bar { background: #ecf0f1; height: 20px; border-radius: 10px; margin: 10px 0; overflow: hidden; }
        .coverage-fill { background: linear-gradient(90deg, #27ae60, #2ecc71); height: 100%; border-radius: 10px; }
        .function-list { background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        .function-tested { color: #27ae60; }
        .function-untested { color: #e74c3c; }
    </style>
</head>
<body>
    <div class="header">
        <h1>📊 Code Coverage Report</h1>
        <p>Generated: $(Get-Date)</p>
    </div>
    
    <div class="coverage-summary">
        <h2>Coverage Summary</h2>
        <p><strong>Total Functions:</strong> $($coverageData.TotalFunctions)</p>
        <p><strong>Tested Functions:</strong> $($coverageData.TestedFunctions)</p>
        <p><strong>Coverage:</strong> $($coverageData.CoveragePercentage)%</p>
        
        <div class="coverage-bar">
            <div class="coverage-fill" style="width: $($coverageData.CoveragePercentage)%"></div>
        </div>
    </div>
    
    <div class="function-list">
        <h2>Function Coverage Details</h2>
        <table style="width: 100%; border-collapse: collapse;">
            <tr><th>Function Name</th><th>Status</th><th>Test References</th></tr>
"@

        foreach ($function in $coverageData.Functions) {
            $statusClass = if ($function.Tested) { "function-tested" } else { "function-untested" }
            $statusText = if ($function.Tested) { "✓ Tested" } else { "✗ Not Tested" }
            $testRefs = if ($function.TestReferences.Count -gt 0) { $function.TestReferences -join ", " } else { "None" }
            
            $htmlCoverage += "<tr>"
            $htmlCoverage += "<td>$($function.Name)</td>"
            $htmlCoverage += "<td class='$statusClass'>$statusText</td>"
            $htmlCoverage += "<td>$testRefs</td>"
            $htmlCoverage += "</tr>"
        }

        $htmlCoverage += @"
        </table>
    </div>
</body>
</html>
"@

        $htmlCoverage | Out-File -FilePath $this.CoverageReportPath -Encoding UTF8
        Write-Host "Coverage report generated: $($this.CoverageReportPath)" -ForegroundColor Cyan
    }
    
    [void]SetupCIPipeline() {
        Write-Host "Setting up CI Pipeline..." -ForegroundColor Yellow
        
        $pipelineScript = @"
# Continuous Integration Pipeline Script
`$ErrorActionPreference = 'Stop'

Write-Host 'Starting CI Pipeline...' -ForegroundColor Green

# Шаг 1: Запуск всех тестов
try {
    `$testFramework = [TestFramework]::new()
    
    # Добавление тестов (должно быть настроено в проекте)
    # `$testFramework.AddTest('Test1', { ... }, 'Unit')
    # `$testFramework.AddTest('Test2', { ... }, 'Integration')
    
    `$testFramework.RunAllTests()
    
    # Проверка успешности тестов
    `$failedTests = (`$testFramework.Tests | Where-Object { `$_.Status -eq 'Failed' }).Count
    if (`$failedTests -gt 0) {
        throw "`$failedTests tests failed. Pipeline aborted."
    }
    
    Write-Host 'All tests passed!' -ForegroundColor Green
}
catch {
    Write-Host "CI Pipeline failed: `$(`$_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Шаг 2: Генерация артефактов
Write-Host 'Generating build artifacts...' -ForegroundColor Yellow

# Шаг 3: Деплой (если все тесты пройдены)
Write-Host 'Deploying application...' -ForegroundColor Green

Write-Host 'CI Pipeline completed successfully!' -ForegroundColor Green
"@

        $pipelinePath = "CIPipeline.ps1"
        $pipelineScript | Set-Content -Path $pipelinePath
        Write-Host "CI Pipeline script created: $pipelinePath" -ForegroundColor Green
    }
    
    [void]MockExternalDependencies() {
        Write-Host "Creating mock dependencies..." -ForegroundColor Yellow
        
        # Моки для внешних зависимостей
        $mockScript = @"
# Mock Database Connection
function Mock-GetDatabaseData {
    param([string]`$query)
    
    return @(
        @{ Id = 1; Name = 'Test User 1'; Email = 'test1@example.com' },
        @{ Id = 2; Name = 'Test User 2'; Email = 'test2@example.com' },
        @{ Id = 3; Name = 'Test User 3'; Email = 'test3@example.com' }
    )
}

# Mock Web Service Call
function Mock-InvokeWebService {
    param([string]`$url, [hashtable]`$body)
    
    return @{
        StatusCode = 200
        Content = 'Mocked response'
        Success = `$true
    }
}

# Mock File System Operations
function Mock-ReadFile {
    param([string]`$path)
    
    return 'Mocked file content'
}

function Mock-WriteFile {
    param([string]`$path, [string]`$content)
    
    Write-Host "Mock: Writing to `$path" -ForegroundColor Gray
    return `$true
}

# Mock Email Sending
function Mock-SendEmail {
    param([string]`$to, [string]`$subject, [string]`$body)
    
    Write-Host "Mock: Sending email to `$to" -ForegroundColor Gray
    return @{ Success = `$true; MessageId = 'mock-123' }
}

Write-Host 'Mock dependencies created successfully!' -ForegroundColor Green
"@

        $mockPath = "MockDependencies.ps1"
        $mockScript | Set-Content -Path $mockPath
        Write-Host "Mock dependencies script created: $mockPath" -ForegroundColor Green
    }
}

# Демонстрация работы фреймворка тестирования
function Show-TestDemo {
    $testFramework = [TestFramework]::new()
    
    # Добавление unit тестов
    $testFramework.AddTest("Test String Operations", {
        $result = "Hello".Length
        if ($result -ne 5) { throw "Expected length 5, got $result" }
        
        $upper = "hello".ToUpper()
        if ($upper -ne "HELLO") { throw "Expected HELLO, got $upper" }
    }, "Unit")
    
    $testFramework.AddTest("Test Math Operations", {
        $sum = 2 + 2
        if ($sum -ne 4) { throw "Expected 4, got $sum" }
        
        $product = 3 * 3
        if ($product -ne 9) { throw "Expected 9, got $product" }
    }, "Unit")
    
    # Добавление integration тестов
    $testFramework.AddTest("Test File System Integration", {
        $testFile = "test_integration.txt"
        "Test content" | Set-Content -Path $testFile
        $content = Get-Content -Path $testFile -Raw
        if ($content.Trim() -ne "Test content") { throw "File content mismatch" }
        Remove-Item -Path $testFile -ErrorAction SilentlyContinue
    }, "Integration")
    
    # Запуск всех тестов
    $testFramework.RunAllTests()
    
    # Настройка CI пайплайна
    $testFramework.SetupCIPipeline()
    
    # Создание mock зависимостей
    $testFramework.MockExternalDependencies()
    
    Write-Host "Test framework demo completed!" -ForegroundColor Green
}

# Основное меню
function Show-MainMenu {
    Write-Host "`n=== PowerShell Test Framework ===" -ForegroundColor Green
    Write-Host "1. Add Test"
    Write-Host "2. Run Unit Tests"
    Write-Host "3. Run Integration Tests"
    Write-Host "4. Run All Tests"
    Write-Host "5. Setup CI Pipeline"
    Write-Host "6. Create Mock Dependencies"
    Write-Host "7. Run Demo"
    Write-Host "8. Exit"
    
    return Read-Host "`nSelect option (1-8)"
}

# Инициализация фреймворка тестирования
$testFramework = [TestFramework]::new()

Write-Host "PowerShell Test Framework initialized" -ForegroundColor Green

do {
    $choice = Show-MainMenu
    
    try {
        switch ($choice) {
            "1" {
                Write-Host "Adding new test..." -ForegroundColor Yellow
                $testName = Read-Host "Test name"
                $testCategory = Read-Host "Test category (Unit/Integration)"
                $testScript = Read-Host "Test script (PowerShell code)"
                
                $scriptBlock = [scriptblock]::Create($testScript)
                $testFramework.AddTest($testName, $scriptBlock, $testCategory)
            }
            "2" {
                $testFramework.RunUnitTests()
            }
            "3" {
                $testFramework.RunIntegrationTests()
            }
            "4" {
                $testFramework.RunAllTests()
            }
            "5" {
                $testFramework.SetupCIPipeline()
            }
            "6" {
                $testFramework.MockExternalDependencies()
            }
            "7" {
                Show-TestDemo
            }
            "8" {
                Write-Host "Goodbye!" -ForegroundColor Green
            }
            default {
                Write-Host "Invalid option" -ForegroundColor Red
            }
        }
    }
    catch {
        Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    }
    
    if ($choice -ne "8") {
        Read-Host "`nPress Enter to continue..."
    }
} while ($choice -ne "8")
