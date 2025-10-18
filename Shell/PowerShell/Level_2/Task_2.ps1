# Генератор отчетов о системе
Write-Host "=== System Report Generator ===" -ForegroundColor Green

# Сбор системной информации
$systemInfo = Get-ComputerInfo | Select-Object `
    @{Name="Computer"; Expression={$_.CsName}},
    @{Name="OS"; Expression={$_.WindowsProductName}},
    @{Name="Version"; Expression={$_.WindowsVersion}},
    @{Name="Architecture"; Expression={$_.OsArchitecture}},
    @{Name="Memory_GB"; Expression={[math]::Round($_.TotalPhysicalMemory/1GB, 2)}},
    @{Name="Uptime_Days"; Expression={[math]::Round($_.OsUptime.TotalDays, 2)}}

$cpuInfo = Get-WmiObject Win32_Processor | Select-Object `
    @{Name="CPU"; Expression={$_.Name}},
    @{Name="Cores"; Expression={$_.NumberOfCores}},
    @{Name="Speed_GHz"; Expression={[math]::Round($_.MaxClockSpeed/1000, 2)}}

$diskInfo = Get-WmiObject Win32_LogicalDisk -Filter "DriveType=3" | Select-Object `
    @{Name="Drive"; Expression={$_.DeviceID}},
    @{Name="Size_GB"; Expression={[math]::Round($_.Size/1GB, 2)}},
    @{Name="Free_GB"; Expression={[math]::Round($_.FreeSpace/1GB, 2)}},
    @{Name="Usage_Percent"; Expression={[math]::Round(100 - ($_.FreeSpace/$_.Size*100), 2)}}

$networkInfo = Get-NetIPAddress -AddressFamily IPv4 | Where-Object {$_.InterfaceAlias -notlike "*Loopback*"} | Select-Object `
    InterfaceAlias, IPAddress

# Функция выбора формата
function Show-ReportMenu {
    Write-Host "`n--- Output Format ---" -ForegroundColor Yellow
    Write-Host "1. Console (colored)"
    Write-Host "2. Text File"
    Write-Host "3. HTML Report"
    Write-Host "4. All Formats"
    
    $choice = Read-Host "`nSelect output format (1-4)"
    return $choice
}

# Функция выбора категорий
function Show-CategoryMenu {
    Write-Host "`n--- Report Categories ---" -ForegroundColor Yellow
    Write-Host "1. System Information"
    Write-Host "2. CPU Information" 
    Write-Host "3. Disk Information"
    Write-Host "4. Network Information"
    Write-Host "5. All Categories"
    
    $choice = Read-Host "`nSelect categories (1-5)"
    return $choice
}

# Основная логика
$formatChoice = Show-ReportMenu
$categoryChoice = Show-CategoryMenu

# Генерация отчета на основе выбора
$reportContent = @()

switch ($categoryChoice) {
    "1" { $reportContent += $systemInfo }
    "2" { $reportContent += $cpuInfo }
    "3" { $reportContent += $diskInfo }
    "4" { $reportContent += $networkInfo }
    "5" { 
        $reportContent += $systemInfo
        $reportContent += $cpuInfo  
        $reportContent += $diskInfo
        $reportContent += $networkInfo
    }
}

# Вывод в выбранном формате
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"

switch ($formatChoice) {
    "1" {
        # Консольный вывод с цветами
        Write-Host "`n=== SYSTEM REPORT ===" -ForegroundColor Green
        foreach ($item in $reportContent) {
            if ($item -is [array]) {
                $item | Format-Table -AutoSize
            } else {
                $item | Format-List
            }
            Write-Host "-" * 50 -ForegroundColor Gray
        }
    }
    "2" {
        # Текстовый файл
        $textFile = "system_report_$timestamp.txt"
        $reportContent | Out-File $textFile
        Write-Host "Report saved to: $textFile" -ForegroundColor Cyan
    }
    "3" {
        # HTML отчет
        $htmlFile = "system_report_$timestamp.html"
        $html = $reportContent | ConvertTo-Html -Title "System Report" -PreContent "<h1>System Report - $(Get-Date)</h1>"
        $html | Out-File $htmlFile
        Write-Host "HTML report saved to: $htmlFile" -ForegroundColor Cyan
    }
    "4" {
        # Все форматы
        $textFile = "system_report_$timestamp.txt"
        $htmlFile = "system_report_$timestamp.html"
        
        $reportContent | Out-File $textFile
        $reportContent | ConvertTo-Html -Title "System Report" -PreContent "<h1>System Report - $(Get-Date)</h1>" | Out-File $htmlFile
        
        Write-Host "Reports saved:" -ForegroundColor Cyan
        Write-Host "Text: $textFile"
        Write-Host "HTML: $htmlFile"
        
        # Также показать в консоли
        Write-Host "`n=== CONSOLE PREVIEW ===" -ForegroundColor Green
        $reportContent | Format-Table -AutoSize
    }
}

Write-Host "`nReport generation completed!" -ForegroundColor Green
