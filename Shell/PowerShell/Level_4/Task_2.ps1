# Инвентаризация сети
Write-Host "=== Network Inventory Scanner ===" -ForegroundColor Green

function Test-NetworkRange {
    param(
        [string]$network = "192.168.1",
        [int]$start = 1,
        [int]$end = 254
    )
    
    Write-Host "Scanning network range: $network.$start - $network.$end" -ForegroundColor Yellow
    
    $activeHosts = @()
    $jobs = @()
    
    # Асинхронное сканирование
    for ($i = $start; $i -le $end; $i++) {
        $ip = "$network.$i"
        $job = Start-Job -ScriptBlock {
            param($ip)
            if (Test-Connection -ComputerName $ip -Count 1 -Quiet -TimeoutSeconds 1) {
                return $ip
            }
        } -ArgumentList $ip
        $jobs += $job
    }
    
    # Ожидание завершения всех jobs
    Write-Host "Scanning in progress..." -NoNewline
    
    do {
        Write-Host "." -NoNewline
        Start-Sleep -Seconds 1
        $completed = ($jobs | Where-Object { $_.State -ne "Running" }).Count
    } while ($completed -lt $jobs.Count)
    
    Write-Host " Done!" -ForegroundColor Green
    
    # Сбор результатов
    foreach ($job in $jobs) {
        $result = Receive-Job -Job $job
        if ($result) {
            $activeHosts += $result
        }
        Remove-Job -Job $job
    }
    
    return $activeHosts
}

function Get-PortInformation {
    param([string]$hostIP)
    
    $commonPorts = @(21, 22, 23, 25, 53, 80, 110, 135, 139, 143, 443, 445, 993, 995, 3389)
    $openPorts = @()
    
    Write-Host "Scanning ports on $hostIP..." -ForegroundColor Gray
    
    foreach ($port in $commonPorts) {
        $tcpClient = New-Object System.Net.Sockets.TcpClient
        $result = $tcpClient.BeginConnect($hostIP, $port, $null, $null)
        $success = $result.AsyncWaitHandle.WaitOne(100, $false)
        
        if ($success) {
            $openPorts += $port
            $tcpClient.EndConnect($result)
        }
        
        $tcpClient.Close()
    }
    
    return $openPorts
}

function Get-HostDetails {
    param([string]$hostIP)
    
    $details = @{
        IP = $hostIP
        Hostname = $null
        MAC = $null
        Ports = @()
    }
    
    try {
        # Получение имени хоста
        $dnsResult = [System.Net.Dns]::GetHostEntry($hostIP)
        $details.Hostname = $dnsResult.HostName
        
        # Получение MAC адреса через ARP
        $arpOutput = arp -a $hostIP 2>$null
        if ($arpOutput -match "([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})") {
            $details.MAC = $matches[0]
        }
        
        # Сканирование портов
        $details.Ports = Get-PortInformation -hostIP $hostIP
    }
    catch {
        # Если не удалось получить детали, оставляем базовую информацию
    }
    
    return $details
}

function Create-NetworkMap {
    param($hostsDetails)
    
    Write-Host "`n=== Network Map ===" -ForegroundColor Yellow
    
    foreach ($host in $hostsDetails) {
        Write-Host "`nIP: $($host.IP)" -ForegroundColor Cyan
        Write-Host "  Hostname: $($host.Hostname)" -ForegroundColor Gray
        Write-Host "  MAC: $($host.MAC)" -ForegroundColor Gray
        
        if ($host.Ports.Count -gt 0) {
            Write-Host "  Open Ports: $($host.Ports -join ', ')" -ForegroundColor Green
        } else {
            Write-Host "  Open Ports: None detected" -ForegroundColor Gray
        }
    }
}

function Export-ToExcel {
    param($hostsDetails, [string]$fileName = "NetworkInventory.xlsx")
    
    try {
        # Создание данных для экспорта
        $exportData = $hostsDetails | ForEach-Object {
            [PSCustomObject]@{
                IP = $_.IP
                Hostname = $_.Hostname
                MAC = $_.MAC
                OpenPorts = ($_.Ports -join ", ")
                ScanDate = Get-Date
            }
        }
        
        # Экспорт в CSV (альтернатива если Excel не доступен)
        $csvFile = $fileName -replace "\.xlsx$", ".csv"
        $exportData | Export-Csv -Path $csvFile -NoTypeInformation
        Write-Host "Exported to CSV: $csvFile" -ForegroundColor Cyan
        
        # Попытка экспорта в Excel если установлен ImportExcel модуль
        if (Get-Module -ListAvailable -Name ImportExcel) {
            Import-Module ImportExcel
            $exportData | Export-Excel -Path $fileName -WorksheetName "Network Inventory" -AutoSize -BoldTopRow -FreezeTopRow
            Write-Host "Exported to Excel: $fileName" -ForegroundColor Cyan
        } else {
            Write-Host "Install ImportExcel module for Excel export: Install-Module ImportExcel" -ForegroundColor Yellow
        }
    }
    catch {
        Write-Host "Export failed: $($_.Exception.Message)" -ForegroundColor Red
    }
}

# Основная программа
Write-Host "Network Inventory Scanner" -ForegroundColor Green
Write-Host "========================" -ForegroundColor Green

# Получение текущей сети
$localIP = (Get-NetIPAddress -AddressFamily IPv4 | Where-Object {$_.InterfaceAlias -notlike "*Loopback*"}).IPAddress[0]
$networkPrefix = $localIP -replace "\.\d+$", ""

Write-Host "Detected local network: $networkPrefix.0/24" -ForegroundColor Yellow

$scanChoice = Read-Host "`nScan this network? (y/n)"
if ($scanChoice -eq 'y' -or $scanChoice -eq 'Y') {
    $network = $networkPrefix
} else {
    $network = Read-Host "Enter network prefix (e.g., 192.168.1)"
}

$startRange = Read-Host "Start IP (default: 1)"
$endRange = Read-Host "End IP (default: 254)"

if (-not $startRange) { $startRange = 1 }
if (-not $endRange) { $endRange = 254 }

Write-Host "`nStarting network scan..." -ForegroundColor Yellow

# Сканирование сети
$activeHosts = Test-NetworkRange -network $network -start ([int]$startRange) -end ([int]$endRange)

if ($activeHosts.Count -gt 0) {
    Write-Host "`nFound $($activeHosts.Count) active hosts" -ForegroundColor Green
    
    # Получение детальной информации о хостах
    $hostsDetails = @()
    $counter = 0
    
    foreach ($host in $activeHosts) {
        $counter++
        Write-Progress -Activity "Scanning Hosts" -Status "Processing $host" -PercentComplete (($counter / $activeHosts.Count) * 100)
        $hostDetails = Get-HostDetails -hostIP $host
        $hostsDetails += $hostDetails
    }
    
    Write-Progress -Activity "Scanning Hosts" -Completed
    
    # Создание карты сети
    Create-NetworkMap -hostsDetails $hostsDetails
    
    # Экспорт результатов
    $exportChoice = Read-Host "`nExport results to Excel? (y/n)"
    if ($exportChoice -eq 'y' -or $exportChoice -eq 'Y') {
        $fileName = "NetworkInventory_$(Get-Date -Format 'yyyyMMdd_HHmmss').xlsx"
        Export-ToExcel -hostsDetails $hostsDetails -fileName $fileName
    }
    
    Write-Host "`nNetwork inventory completed!" -ForegroundColor Green
    
} else {
    Write-Host "No active hosts found in the specified range" -ForegroundColor Red
}
