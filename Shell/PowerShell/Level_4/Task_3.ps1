# Монитор производительности в реальном времени
Write-Host "=== Real-time Performance Monitor ===" -ForegroundColor Green

class PerformanceMonitor {
    [System.Collections.ArrayList]$CpuHistory
    [System.Collections.ArrayList]$MemoryHistory
    [System.Collections.ArrayList]$DiskHistory
    [System.Collections.ArrayList]$NetworkHistory
    [datetime]$StartTime
    [int]$MaxHistory
    
    PerformanceMonitor([int]$maxHistory) {
        $this.CpuHistory = [System.Collections.ArrayList]::new()
        $this.MemoryHistory = [System.Collections.ArrayList]::new()
        $this.DiskHistory = [System.Collections.ArrayList]::new()
        $this.NetworkHistory = [System.Collections.ArrayList]::new()
        $this.StartTime = Get-Date
        $this.MaxHistory = $maxHistory
    }
    
    [void]UpdateMetrics() {
        # CPU Usage
        $cpuCounter = Get-Counter "\Processor(_Total)\% Processor Time" -SampleInterval 1 -MaxSamples 1
        $cpuUsage = [math]::Round($cpuCounter.CounterSamples.CookedValue, 2)
        
        # Memory Usage
        $memoryCounter = Get-Counter "\Memory\% Committed Bytes In Use" -SampleInterval 1 -MaxSamples 1
        $memoryUsage = [math]::Round($memoryCounter.CounterSamples.CookedValue, 2)
        
        # Disk Usage
        $diskCounter = Get-Counter "\PhysicalDisk(_Total)\% Disk Time" -SampleInterval 1 -MaxSamples 1
        $diskUsage = [math]::Round($diskCounter.CounterSamples.CookedValue, 2)
        
        # Network Usage (упрощенный)
        $networkCounter = Get-Counter "\Network Interface(*)\Bytes Total/sec" -SampleInterval 1 -MaxSamples 1
        $networkUsage = [math]::Round(($networkCounter.CounterSamples | Measure-Object CookedValue -Sum).Sum / 1MB, 2)
        
        # Добавление в историю
        $this.AddToHistory($this.CpuHistory, $cpuUsage)
        $this.AddToHistory($this.MemoryHistory, $memoryUsage)
        $this.AddToHistory($this.DiskHistory, $diskUsage)
        $this.AddToHistory($this.NetworkHistory, $networkUsage)
    }
    
    [void]AddToHistory([System.Collections.ArrayList]$list, $value) {
        $list.Add($value) | Out-Null
        if ($list.Count -gt $this.MaxHistory) {
            $list.RemoveAt(0)
        }
    }
    
    [string]GetColor($value, $warning, $critical) {
        if ($value -ge $critical) { return "Red" }
        if ($value -ge $warning) { return "Yellow" }
        return "Green"
    }
    
    [void]DisplayDashboard() {
        $currentCpu = if ($this.CpuHistory.Count -gt 0) { $this.CpuHistory[-1] } else { 0 }
        $currentMemory = if ($this.MemoryHistory.Count -gt 0) { $this.MemoryHistory[-1] } else { 0 }
        $currentDisk = if ($this.DiskHistory.Count -gt 0) { $this.DiskHistory[-1] } else { 0 }
        $currentNetwork = if ($this.NetworkHistory.Count -gt 0) { $this.NetworkHistory[-1] } else { 0 }
        
        $cpuColor = $this.GetColor($currentCpu, 70, 85)
        $memoryColor = $this.GetColor($currentMemory, 75, 90)
        $diskColor = $this.GetColor($currentDisk, 70, 85)
        $networkColor = $this.GetColor($currentNetwork, 50, 80)
        
        Clear-Host
        Write-Host "=== REAL-TIME PERFORMANCE MONITOR ===" -ForegroundColor Green
        Write-Host "Started: $($this.StartTime)" -ForegroundColor Gray
        Write-Host "Current: $(Get-Date)" -ForegroundColor Gray
        Write-Host ""
        
        # Текущие метрики
        Write-Host "CURRENT METRICS:" -ForegroundColor Yellow
        Write-Host "  CPU Usage:    " -NoNewline
        Write-Host "$currentCpu%" -ForegroundColor $cpuColor
        Write-Host "  Memory Usage: " -NoNewline
        Write-Host "$currentMemory%" -ForegroundColor $memoryColor
        Write-Host "  Disk Usage:   " -NoNewline
        Write-Host "$currentDisk%" -ForegroundColor $diskColor
        Write-Host "  Network:      " -NoNewline
        Write-Host "$currentNetwork MB/s" -ForegroundColor $networkColor
        
        Write-Host "`nHISTORY CHARTS:" -ForegroundColor Yellow
        
        # ASCII графики
        $this.DrawChart("CPU", $this.CpuHistory, 70, 85)
        $this.DrawChart("Memory", $this.MemoryHistory, 75, 90)
        $this.DrawChart("Disk", $this.DiskHistory, 70, 85)
        $this.DrawChart("Network", $this.NetworkHistory, 50, 80)
        
        Write-Host "`nLEGEND: " -NoNewline -ForegroundColor Yellow
        Write-Host "█ Normal " -NoNewline -ForegroundColor Green
        Write-Host "█ Warning " -NoNewline -ForegroundColor Yellow
        Write-Host "█ Critical" -ForegroundColor Red
        
        Write-Host "`nPress 'q' to quit, 'a' for alerts, any key to refresh" -ForegroundColor Gray
    }
    
    [void]DrawChart([string]$metric, [System.Collections.ArrayList]$data, [int]$warning, [int]$critical) {
        Write-Host "`n$metric : " -NoNewline -ForegroundColor Gray
        
        if ($data.Count -eq 0) {
            Write-Host "No data" -ForegroundColor Gray
            return
        }
        
        foreach ($value in $data) {
            $color = $this.GetColor($value, $warning, $critical)
            $symbol = if ($value -eq 0) { " " } else { "█" }
            Write-Host $symbol -NoNewline -ForegroundColor $color
        }
        
        Write-Host " $($data[-1])%" -ForegroundColor Gray
    }
    
    [void]CheckAlerts() {
        $currentCpu = if ($this.CpuHistory.Count -gt 0) { $this.CpuHistory[-1] } else { 0 }
        $currentMemory = if ($this.MemoryHistory.Count -gt 0) { $this.MemoryHistory[-1] } else { 0 }
        
        $alerts = @()
        
        if ($currentCpu -ge 85) {
            $alerts += "CRITICAL: CPU usage at $currentCpu%"
        } elseif ($currentCpu -ge 70) {
            $alerts += "WARNING: CPU usage at $currentCpu%"
        }
        
        if ($currentMemory -ge 90) {
            $alerts += "CRITICAL: Memory usage at $currentMemory%"
        } elseif ($currentMemory -ge 75) {
            $alerts += "WARNING: Memory usage at $currentMemory%"
        }
        
        if ($alerts.Count -gt 0) {
            Write-Host "`n🚨 ALERTS 🚨" -ForegroundColor Red -BackgroundColor White
            foreach ($alert in $alerts) {
                Write-Host "  $alert" -ForegroundColor Red
            }
        }
    }
}

# Основная программа
$monitor = [PerformanceMonitor]::new(50)

Write-Host "Starting Real-time Performance Monitor..." -ForegroundColor Green
Write-Host "Collecting initial data..." -ForegroundColor Yellow

# Начальный сбор данных
for ($i = 0; $i -lt 5; $i++) {
    $monitor.UpdateMetrics()
    Start-Sleep -Seconds 1
}

do {
    $monitor.UpdateMetrics()
    $monitor.DisplayDashboard()
    $monitor.CheckAlerts()
    
    # Ожидание ввода
    if ($Host.UI.RawUI.KeyAvailable) {
        $key = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
        if ($key.Character -eq 'q') { break }
        if ($key.Character -eq 'a') { 
            $monitor.CheckAlerts()
            Read-Host "`nPress Enter to continue..."
        }
    } else {
        Start-Sleep -Seconds 1
    }
} while ($true)

Write-Host "`nPerformance monitoring stopped." -ForegroundColor Green
