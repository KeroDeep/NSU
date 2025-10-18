# Системный инспектор
Write-Host "=== System Inspector ===" -ForegroundColor Green

# Информация о системе
Write-Host "`n--- System Information ---" -ForegroundColor Yellow
Get-ComputerInfo | Select-Object WindowsProductName, WindowsVersion, OSArchitecture, TotalPhysicalMemory | Format-List

# Список служб
Write-Host "`n--- Services Status ---" -ForegroundColor Yellow
Get-Service | Where-Object {$_.Status -eq "Running"} | Select-Object -First 10 Name, Status | Format-Table -AutoSize

# Установленные программы
Write-Host "`n--- Installed Programs ---" -ForegroundColor Yellow
Get-WmiObject -Class Win32_Product | Select-Object -First 10 Name, Version | Format-Table -AutoSize

# Сетевые интерфейсы
Write-Host "`n--- Network Interfaces ---" -ForegroundColor Yellow
Get-NetIPAddress | Where-Object {$_.AddressFamily -eq "IPv4"} | Select-Object InterfaceAlias, IPAddress | Format-Table -AutoSize

Write-Host "`nReport completed!" -ForegroundColor Green
