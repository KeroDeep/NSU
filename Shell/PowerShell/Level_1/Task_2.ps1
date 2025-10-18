# Работа с объектами
Write-Host "=== Process Analysis ===" -ForegroundColor Green

# Процессы отсортированные по CPU
Write-Host "`n--- Top Processes by CPU ---" -ForegroundColor Yellow
$processes = Get-Process | Sort-Object CPU -Descending | Select-Object -First 15 Name, CPU, PM, WS

# Отфильтровать процессы с памятью > 100MB
$filteredProcesses = $processes | Where-Object {$_.PM -gt 100MB}

# Вывод в табличном формате
$filteredProcesses | Format-Table -AutoSize

# Экспорт в CSV
$csvFile = "processes_report.csv"
$filteredProcesses | Export-Csv -Path $csvFile -NoTypeInformation
Write-Host "`nExported to CSV: $csvFile" -ForegroundColor Cyan

# Создание HTML отчета
$htmlFile = "processes_report.html"
$htmlHeader = @"
<style>
    table { border-collapse: collapse; width: 100%; }
    th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
    th { background-color: #f2f2f2; }
    tr:nth-child(even) { background-color: #f9f9f9; }
    .high-memory { background-color: #ffcccc; }
</style>
<h2>Process Report - $(Get-Date)</h2>
"@

$filteredProcesses | ConvertTo-Html -PreContent $htmlHeader | Out-File $htmlFile
Write-Host "HTML report created: $htmlFile" -ForegroundColor Cyan

Write-Host "`nAnalysis completed!" -ForegroundColor Green
