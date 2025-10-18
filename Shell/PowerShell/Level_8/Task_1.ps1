# Система оркестрации
Write-Host "=== Distributed System Orchestrator ===" -ForegroundColor Green

class SystemOrchestrator {
    [string]$ConfigPath
    [hashtable]$Servers
    [System.Collections.ArrayList]$TaskQueue
    [hashtable]$ExecutionResults
    [string]$DashboardPath
    
    SystemOrchestrator([string]$configPath) {
        $this.ConfigPath = $configPath
        $this.Servers = @{}
        $this.TaskQueue = [System.Collections.ArrayList]::new()
        $this.ExecutionResults = @{}
        $this.DashboardPath = "OrchestrationDashboard_$(Get-Date -Format 'yyyyMMdd_HHmmss').html"
        $this.LoadConfiguration()
    }
    
    [void]LoadConfiguration() {
        if (Test-Path $this.ConfigPath) {
            $config = Get-Content $this.ConfigPath | ConvertFrom-Json
            $this.Servers = @{}
            
            foreach ($server in $config.Servers) {
                $this.Servers[$server.Name] = @{
                    ComputerName = $server.ComputerName
                    Credential = $server.Credential
                    Tags = $server.Tags
                    Status = "Unknown"
                    LastPing = $null
                }
            }
            Write-Host "Configuration loaded: $($this.Servers.Count) servers" -ForegroundColor Green
        } else {
            # Создание конфигурации по умолчанию
            $defaultConfig = @{
                Servers = @(
                    @{Name = "LocalHost"; ComputerName = "localhost"; Tags = @("Primary", "Windows")}
                )
            }
            $defaultConfig | ConvertTo-Json -Depth 3 | Set-Content $this.ConfigPath
            $this.Servers["LocalHost"] = $defaultConfig.Servers[0]
            Write-Host "Default configuration created" -ForegroundColor Yellow
        }
    }
    
    [bool]TestServerConnection([string]$serverName) {
        $server = $this.Servers[$serverName]
        if (-not $server) {
            Write-Host "Server not found: $serverName" -ForegroundColor Red
            return $false
        }
        
        try {
            $testResult = Test-Connection -ComputerName $server.ComputerName -Count 1 -Quiet -ErrorAction Stop
            $server.Status = if ($testResult) { "Online" } else { "Offline" }
            $server.LastPing = Get-Date
            return $testResult
        }
        catch {
            $server.Status = "Error"
            $server.LastPing = Get-Date
            Write-Host "Connection test failed for $serverName : $($_.Exception.Message)" -ForegroundColor Red
            return $false
        }
    }
    
    [hashtable]ExecuteRemoteCommand([string]$serverName, [string]$command, [hashtable]$parameters) {
        $result = @{
            Server = $serverName
            Command = $command
            StartTime = Get-Date
            Success = $false
            Output = $null
            Error = $null
            Duration = $null
        }
        
        $server = $this.Servers[$serverName]
        if (-not $server -or $server.Status -ne "Online") {
            $result.Error = "Server is not available"
            $result.EndTime = Get-Date
            $result.Duration = ($result.EndTime - $result.StartTime).TotalSeconds
            return $result
        }
        
        try {
            $scriptBlock = [scriptblock]::Create($command)
            $invokeParams = @{
                ComputerName = $server.ComputerName
                ScriptBlock = $scriptBlock
                ErrorAction = "Stop"
            }
            
            if ($parameters) {
                $invokeParams.ArgumentList = $parameters.Values
            }
            
            $output = Invoke-Command @invokeParams
            $result.Success = $true
            $result.Output = $output
        }
        catch {
            $result.Success = $false
            $result.Error = $_.Exception.Message
        }
        finally {
            $result.EndTime = Get-Date
            $result.Duration = ($result.EndTime - $result.StartTime).TotalSeconds
        }
        
        return $result
    }
    
    [void]AddTaskToQueue([hashtable]$task) {
        $taskItem = @{
            Id = [guid]::NewGuid().ToString()
            Name = $task.Name
            Command = $task.Command
            Parameters = $task.Parameters
            TargetServers = $task.TargetServers
            Timeout = $task.Timeout ?? 300
            RetryCount = $task.RetryCount ?? 0
            CurrentRetry = 0
            Status = "Pending"
            Created = Get-Date
        }
        
        $this.TaskQueue.Add($taskItem) | Out-Null
        Write-Host "Task added to queue: $($task.Name)" -ForegroundColor Cyan
    }
    
    [void]ProcessTaskQueue() {
        Write-Host "Processing task queue..." -ForegroundColor Yellow
        
        $completedTasks = @()
        
        foreach ($task in $this.TaskQueue.ToArray()) {
            if ($task.Status -eq "Pending" -or $task.Status -eq "Retrying") {
                Write-Host "Executing task: $($task.Name)" -ForegroundColor Green
                
                $taskResults = @()
                $taskSuccess = $true
                
                foreach ($serverName in $task.TargetServers) {
                    Write-Host "  On server: $serverName" -ForegroundColor Gray
                    
                    $result = $this.ExecuteRemoteCommand($serverName, $task.Command, $task.Parameters)
                    $taskResults += $result
                    
                    if (-not $result.Success) {
                        $taskSuccess = $false
                        
                        # Логика повторных попыток
                        if ($task.CurrentRetry -lt $task.RetryCount) {
                            $task.CurrentRetry++
                            $task.Status = "Retrying"
                            Write-Host "  Retry $($task.CurrentRetry)/$($task.RetryCount) scheduled" -ForegroundColor Yellow
                        } else {
                            $task.Status = "Failed"
                        }
                    }
                }
                
                if ($taskSuccess) {
                    $task.Status = "Completed"
                    $task.Completed = Get-Date
                }
                
                # Сохранение результатов
                $this.ExecutionResults[$task.Id] = @{
                    Task = $task
                    Results = $taskResults
                }
                
                if ($task.Status -in @("Completed", "Failed")) {
                    $completedTasks += $task
                }
            }
        }
        
        # Удаление завершенных задач из очереди
        foreach ($completedTask in $completedTasks) {
            $this.TaskQueue.Remove($completedTask)
        }
        
        Write-Host "Queue processing completed. Active tasks: $($this.TaskQueue.Count)" -ForegroundColor Green
    }
    
    [void]ExecuteParallelTasks([hashtable[]]$tasks) {
        Write-Host "Executing $($tasks.Count) tasks in parallel..." -ForegroundColor Yellow
        
        $jobs = @()
        $taskMap = @{}
        
        # Запуск задач параллельно
        foreach ($task in $tasks) {
            $job = Start-Job -ScriptBlock {
                param($serverName, $command, $parameters)
                
                try {
                    $invokeParams = @{
                        ComputerName = $serverName
                        ScriptBlock = [scriptblock]::Create($command)
                        ErrorAction = "Stop"
                    }
                    
                    if ($parameters) {
                        $invokeParams.ArgumentList = $parameters.Values
                    }
                    
                    $output = Invoke-Command @invokeParams
                    return @{ Success = $true; Output = $output }
                }
                catch {
                    return @{ Success = $false; Error = $_.Exception.Message }
                }
            } -ArgumentList $task.TargetServer, $task.Command, $task.Parameters
            
            $jobs += $job
            $taskMap[$job.Id] = $task
        }
        
        # Ожидание завершения и сбор результатов
        $results = @()
        $completed = 0
        
        do {
            $completedJobs = $jobs | Where-Object { $_.State -eq "Completed" -or $_.State -eq "Failed" }
            $completed = $completedJobs.Count
            
            Write-Progress -Activity "Executing Parallel Tasks" -Status "Completed: $completed of $($jobs.Count)" -PercentComplete (($completed / $jobs.Count) * 100)
            Start-Sleep -Seconds 1
        } while ($completed -lt $jobs.Count)
        
        Write-Progress -Activity "Executing Parallel Tasks" -Completed
        
        # Обработка результатов
        foreach ($job in $jobs) {
            $task = $taskMap[$job.Id]
            $jobResult = Receive-Job -Job $job
            
            $results += @{
                Task = $task
                Result = $jobResult
                Server = $task.TargetServer
            }
            
            Remove-Job -Job $job
        }
        
        Write-Host "Parallel execution completed. Results: $($results.Count)" -ForegroundColor Green
        return $results
    }
    
    [void]AggregateResults() {
        Write-Host "Aggregating execution results..." -ForegroundColor Yellow
        
        $summary = @{
            TotalTasks = $this.ExecutionResults.Count
            SuccessfulTasks = ($this.ExecutionResults.Values | Where-Object { 
                $_.Task.Status -eq "Completed" 
            }).Count
            FailedTasks = ($this.ExecutionResults.Values | Where-Object { 
                $_.Task.Status -eq "Failed" 
            }).Count
            TotalServers = $this.Servers.Count
            OnlineServers = ($this.Servers.Values | Where-Object { $_.Status -eq "Online" }).Count
            AggregationTime = Get-Date
        }
        
        # Анализ производительности
        $durations = $this.ExecutionResults.Values | ForEach-Object { 
            $_.Results | ForEach-Object { $_.Duration } 
        } | Where-Object { $_ -ne $null }
        
        if ($durations.Count -gt 0) {
            $summary.Performance = @{
                AverageDuration = ($durations | Measure-Object -Average).Average
                MinDuration = ($durations | Measure-Object -Minimum).Minimum
                MaxDuration = ($durations | Measure-Object -Maximum).Maximum
                TotalDuration = ($durations | Measure-Object -Sum).Sum
            }
        }
        
        $this.ExecutionResults["Summary"] = @{
            Type = "Summary"
            Data = $summary
        }
        
        Write-Host "Results aggregation completed" -ForegroundColor Green
    }
    
    [void]GenerateDashboard() {
        Write-Host "Generating orchestration dashboard..." -ForegroundColor Yellow
        
        $dashboardData = @{
            Servers = $this.Servers
            ActiveTasks = $this.TaskQueue
            ExecutionHistory = $this.ExecutionResults
            Generated = Get-Date
        }
        
        $htmlDashboard = @"
<!DOCTYPE html>
<html>
<head>
    <title>System Orchestration Dashboard</title>
    <style>
        body { font-family: 'Segoe UI', Arial, sans-serif; margin: 20px; background: #f5f5f5; }
        .dashboard { max-width: 1400px; margin: 0 auto; }
        .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; border-radius: 10px; margin-bottom: 20px; }
        .stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; margin-bottom: 20px; }
        .stat-card { background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); text-align: center; }
        .stat-number { font-size: 2em; font-weight: bold; margin: 10px 0; }
        .servers-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 15px; }
        .server-card { background: white; padding: 15px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        .server-online { border-left: 4px solid #27ae60; }
        .server-offline { border-left: 4px solid #e74c3c; }
        .task-queue { background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); margin: 20px 0; }
        table { width: 100%; border-collapse: collapse; margin: 10px 0; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background: #34495e; color: white; }
        .status-completed { color: #27ae60; font-weight: bold; }
        .status-failed { color: #e74c3c; font-weight: bold; }
        .status-pending { color: #f39c12; font-weight: bold; }
    </style>
</head>
<body>
    <div class="dashboard">
        <div class="header">
            <h1>🚀 System Orchestration Dashboard</h1>
            <p>Real-time monitoring and execution tracking</p>
            <p>Generated: $(Get-Date)</p>
        </div>
        
        <div class="stats-grid">
            <div class="stat-card">
                <h3>Total Servers</h3>
                <div class="stat-number">$($this.Servers.Count)</div>
            </div>
            <div class="stat-card">
                <h3>Online Servers</h3>
                <div class="stat-number" style="color: #27ae60;">$(($this.Servers.Values | Where-Object { $_.Status -eq 'Online' }).Count)</div>
            </div>
            <div class="stat-card">
                <h3>Active Tasks</h3>
                <div class="stat-number">$($this.TaskQueue.Count)</div>
            </div>
            <div class="stat-card">
                <h3>Completed Tasks</h3>
                <div class="stat-number" style="color: #27ae60;">$(($this.ExecutionResults.Values | Where-Object { $_.Task.Status -eq 'Completed' }).Count)</div>
            </div>
        </div>
        
        <div class="servers-grid">
            <h2>🖥️ Server Status</h2>
"@

        foreach ($server in $this.Servers.GetEnumerator()) {
            $statusClass = if ($server.Value.Status -eq "Online") { "server-online" } else { "server-offline" }
            $htmlDashboard += @"
            <div class="server-card $statusClass">
                <h3>$($server.Key)</h3>
                <p><strong>Computer:</strong> $($server.Value.ComputerName)</p>
                <p><strong>Status:</strong> <span style="color: $(if ($server.Value.Status -eq 'Online') { '#27ae60' } else { '#e74c3c' })">$($server.Value.Status)</span></p>
                <p><strong>Last Ping:</strong> $(if ($server.Value.LastPing) { $server.Value.LastPing.ToString('yyyy-MM-dd HH:mm:ss') } else { 'Never' })</p>
                <p><strong>Tags:</strong> $($server.Value.Tags -join ', ')</p>
            </div>
"@
        }

        $htmlDashboard += @"
        </div>
        
        <div class="task-queue">
            <h2>📋 Task Queue</h2>
"@

        if ($this.TaskQueue.Count -gt 0) {
            $htmlDashboard += @"
            <table>
                <tr><th>Task Name</th><th>Status</th><th>Target Servers</th><th>Created</th><th>Retry Count</th></tr>
"@
            foreach ($task in $this.TaskQueue) {
                $statusClass = "status-" + $task.Status.ToLower()
                $htmlDashboard += "<tr>"
                $htmlDashboard += "<td>$($task.Name)</td>"
                $htmlDashboard += "<td class='$statusClass'>$($task.Status)</td>"
                $htmlDashboard += "<td>$($task.TargetServers -join ', ')</td>"
                $htmlDashboard += "<td>$($task.Created.ToString('yyyy-MM-dd HH:mm:ss'))</td>"
                $htmlDashboard += "<td>$($task.CurrentRetry)/$($task.RetryCount)</td>"
                $htmlDashboard += "</tr>"
            }
            $htmlDashboard += "</table>"
        } else {
            $htmlDashboard += "<p>No active tasks in queue.</p>"
        }

        $htmlDashboard += @"
        </div>
        
        <div class="task-queue">
            <h2>📊 Execution History</h2>
"@

        $completedTasks = $this.ExecutionResults.Values | Where-Object { $_.Type -ne "Summary" }
        if ($completedTasks.Count -gt 0) {
            $htmlDashboard += @"
            <table>
                <tr><th>Task Name</th><th>Status</th><th>Servers</th><th>Duration</th><th>Completed</th></tr>
"@
            foreach ($taskResult in $completedTasks) {
                $task = $taskResult.Task
                $statusClass = "status-" + $task.Status.ToLower()
                $successCount = ($taskResult.Results | Where-Object { $_.Success }).Count
                $totalCount = $taskResult.Results.Count
                
                $htmlDashboard += "<tr>"
                $htmlDashboard += "<td>$($task.Name)</td>"
                $htmlDashboard += "<td class='$statusClass'>$($task.Status) ($successCount/$totalCount)</td>"
                $htmlDashboard += "<td>$($task.TargetServers -join ', ')</td>"
                $htmlDashboard += "<td>$([math]::Round(($taskResult.Results | Measure-Object Duration -Sum).Sum, 2))s</td>"
                $htmlDashboard += "<td>$(if ($task.Completed) { $task.Completed.ToString('yyyy-MM-dd HH:mm:ss') } else { 'N/A' })</td>"
                $htmlDashboard += "</tr>"
            }
            $htmlDashboard += "</table>"
        } else {
            $htmlDashboard += "<p>No execution history available.</p>"
        }

        $htmlDashboard += @"
        </div>
    </div>
</body>
</html>
"@

        $htmlDashboard | Out-File -FilePath $this.DashboardPath -Encoding UTF8
        Write-Host "Dashboard generated: $($this.DashboardPath)" -ForegroundColor Cyan
    }
}

# Демонстрация работы оркестратора
function Show-Demo {
    $orchestrator = [SystemOrchestrator]::new("orchestrator_config.json")
    
    # Тестирование соединений
    Write-Host "Testing server connections..." -ForegroundColor Yellow
    foreach ($serverName in $orchestrator.Servers.Keys) {
        $orchestrator.TestServerConnection($serverName)
    }
    
    # Добавление демонстрационных задач
    $demoTasks = @(
        @{
            Name = "Get System Info"
            Command = 'Get-ComputerInfo | Select-Object WindowsProductName, TotalPhysicalMemory, OsArchitecture'
            TargetServers = @("LocalHost")
            RetryCount = 2
        },
        @{
            Name = "Get Running Services"
            Command = 'Get-Service | Where-Object Status -eq "Running" | Select-Object -First 5 Name, Status'
            TargetServers = @("LocalHost")
            RetryCount = 1
        },
        @{
            Name = "Check Disk Space"
            Command = 'Get-WmiObject Win32_LogicalDisk -Filter "DriveType=3" | Select-Object DeviceID, Size, FreeSpace'
            TargetServers = @("LocalHost")
            RetryCount = 3
        }
    )
    
    foreach ($task in $demoTasks) {
        $orchestrator.AddTaskToQueue($task)
    }
    
    # Обработка очереди задач
    $orchestrator.ProcessTaskQueue()
    
    # Агрегация результатов
    $orchestrator.AggregateResults()
    
    # Генерация дашборда
    $orchestrator.GenerateDashboard()
    
    Write-Host "Demo completed! Check the generated dashboard." -ForegroundColor Green
}

# Основное меню
function Show-MainMenu {
    Write-Host "`n=== System Orchestrator ===" -ForegroundColor Green
    Write-Host "1. Test Server Connections"
    Write-Host "2. Add Task to Queue"
    Write-Host "3. Process Task Queue"
    Write-Host "4. Execute Parallel Tasks"
    Write-Host "5. Show Execution Results"
    Write-Host "6. Generate Dashboard"
    Write-Host "7. Run Demo"
    Write-Host "8. Exit"
    
    return Read-Host "`nSelect option (1-8)"
}

# Инициализация оркестратора
$orchestrator = [SystemOrchestrator]::new("orchestrator_config.json")

Write-Host "System Orchestrator initialized" -ForegroundColor Green
Write-Host "Configured servers: $($orchestrator.Servers.Count)" -ForegroundColor Cyan

do {
    $choice = Show-MainMenu
    
    try {
        switch ($choice) {
            "1" {
                Write-Host "Testing server connections..." -ForegroundColor Yellow
                foreach ($serverName in $orchestrator.Servers.Keys) {
                    $status = $orchestrator.TestServerConnection($serverName)
                    Write-Host "  $serverName : $(if ($status) { 'Online' } else { 'Offline' })" -ForegroundColor $(if ($status) { 'Green' } else { 'Red' })
                }
            }
            "2" {
                Write-Host "Adding new task to queue..." -ForegroundColor Yellow
                $taskName = Read-Host "Task name"
                $command = Read-Host "PowerShell command"
                
                Write-Host "Available servers: $($orchestrator.Servers.Keys -join ', ')"
                $serversInput = Read-Host "Target servers (comma-separated)"
                $targetServers = $serversInput -split ',' | ForEach-Object { $_.Trim() }
                
                $retryCount = Read-Host "Retry count (default: 0)"
                if (-not $retryCount) { $retryCount = 0 }
                
                $task = @{
                    Name = $taskName
                    Command = $command
                    TargetServers = $targetServers
                    RetryCount = [int]$retryCount
                }
                
                $orchestrator.AddTaskToQueue($task)
            }
            "3" {
                Write-Host "Processing task queue..." -ForegroundColor Yellow
                $orchestrator.ProcessTaskQueue()
            }
            "4" {
                Write-Host "Preparing parallel execution..." -ForegroundColor Yellow
                $parallelTasks = @()
                
                do {
                    $taskName = Read-Host "Task name (or 'done' to finish)"
                    if ($taskName -ne 'done') {
                        $command = Read-Host "PowerShell command"
                        $server = Read-Host "Target server"
                        
                        $parallelTasks += @{
                            Name = $taskName
                            Command = $command
                            TargetServer = $server
                        }
                    }
                } while ($taskName -ne 'done')
                
                if ($parallelTasks.Count -gt 0) {
                    $results = $orchestrator.ExecuteParallelTasks($parallelTasks)
                    Write-Host "Parallel execution completed. Results: $($results.Count)" -ForegroundColor Green
                }
            }
            "5" {
                Write-Host "Execution Results:" -ForegroundColor Yellow
                if ($orchestrator.ExecutionResults.Count -gt 0) {
                    foreach ($result in $orchestrator.ExecutionResults.GetEnumerator()) {
                        if ($result.Key -ne "Summary") {
                            $task = $result.Value.Task
                            Write-Host "  $($task.Name): $($task.Status)" -ForegroundColor $(if ($task.Status -eq 'Completed') { 'Green' } else { 'Red' })
                        }
                    }
                } else {
                    Write-Host "  No execution results available" -ForegroundColor Gray
                }
            }
            "6" {
                $orchestrator.GenerateDashboard()
            }
            "7" {
                Show-Demo
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
