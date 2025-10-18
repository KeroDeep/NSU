# Мастер настройки Windows
Write-Host "=== Windows Configuration Master ===" -ForegroundColor Green

function Show-MainMenu {
    Write-Host "`n--- Configuration Options ---" -ForegroundColor Yellow
    Write-Host "1. Change Computer Name"
    Write-Host "2. Configure Network Settings" 
    Write-Host "3. Manage Services"
    Write-Host "4. Create System Restore Point"
    Write-Host "5. Show Current Configuration"
    Write-Host "6. Exit"
    
    $choice = Read-Host "`nSelect option (1-6)"
    return $choice
}

function Change-ComputerName {
    Write-Host "`n--- Change Computer Name ---" -ForegroundColor Yellow
    $currentName = $env:COMPUTERNAME
    Write-Host "Current computer name: $currentName"
    
    $newName = Read-Host "Enter new computer name"
    if (-not $newName) {
        Write-Host "Operation cancelled" -ForegroundColor Yellow
        return
    }
    
    try {
        # Требуются права администратора
        if (-NOT ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole] "Administrator")) {
            Write-Host "Administrator rights required for this operation" -ForegroundColor Red
            return
        }
        
        Rename-Computer -NewName $newName -Force
        Write-Host "Computer name will be changed to: $newName" -ForegroundColor Green
        Write-Host "Restart required for changes to take effect" -ForegroundColor Yellow
    }
    catch {
        Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    }
}

function Configure-Network {
    Write-Host "`n--- Network Configuration ---" -ForegroundColor Yellow
    $adapters = Get-NetAdapter | Where-Object {$_.Status -eq "Up"}
    
    if (-not $adapters) {
        Write-Host "No active network adapters found" -ForegroundColor Red
        return
    }
    
    Write-Host "Available network adapters:"
    $i = 1
    $adapterList = @()
    foreach ($adapter in $adapters) {
        Write-Host "$i. $($adapter.Name) - $($adapter.InterfaceDescription)"
        $adapterList += $adapter
        $i++
    }
    
    $choice = Read-Host "`nSelect adapter (1-$($adapterList.Count))"
    $selectedAdapter = $adapterList[$choice-1]
    
    if ($selectedAdapter) {
        Write-Host "`nSelected: $($selectedAdapter.Name)"
        Write-Host "1. Show current IP configuration"
        Write-Host "2. Set static IP address"
        Write-Host "3. Set DNS servers"
        
        $action = Read-Host "`nSelect action (1-3)"
        
        switch ($action) {
            "1" {
                Get-NetIPAddress -InterfaceAlias $selectedAdapter.Name | Format-Table
            }
            "2" {
                $ip = Read-Host "Enter IP address (e.g., 192.168.1.100)"
                $gateway = Read-Host "Enter gateway (e.g., 192.168.1.1)"
                $prefix = Read-Host "Enter prefix length (e.g., 24)"
                
                try {
                    New-NetIPAddress -InterfaceAlias $selectedAdapter.Name -IPAddress $ip -PrefixLength $prefix -DefaultGateway $gateway -ErrorAction Stop
                    Write-Host "Static IP configured successfully" -ForegroundColor Green
                }
                catch {
                    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
                }
            }
            "3" {
                $dns1 = Read-Host "Enter primary DNS (e.g., 8.8.8.8)"
                $dns2 = Read-Host "Enter secondary DNS (e.g., 8.8.4.4)"
                
                try {
                    Set-DnsClientServerAddress -InterfaceAlias $selectedAdapter.Name -ServerAddresses $dns1, $dns2
                    Write-Host "DNS servers configured successfully" -ForegroundColor Green
                }
                catch {
                    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
                }
            }
        }
    }
}

function Manage-Services {
    Write-Host "`n--- Service Management ---" -ForegroundColor Yellow
    Write-Host "1. List running services"
    Write-Host "2. Start a service"
    Write-Host "3. Stop a service"
    Write-Host "4. Restart a service"
    
    $action = Read-Host "`nSelect action (1-4)"
    
    switch ($action) {
        "1" {
            Get-Service | Where-Object {$_.Status -eq "Running"} | Select-Object -First 20 Name, DisplayName, Status | Format-Table -AutoSize
        }
        "2" {
            $serviceName = Read-Host "Enter service name to start"
            try {
                Start-Service -Name $serviceName
                Write-Host "Service $serviceName started" -ForegroundColor Green
            }
            catch {
                Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
            }
        }
        "3" {
            $serviceName = Read-Host "Enter service name to stop"
            try {
                Stop-Service -Name $serviceName
                Write-Host "Service $serviceName stopped" -ForegroundColor Green
            }
            catch {
                Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
            }
        }
        "4" {
            $serviceName = Read-Host "Enter service name to restart"
            try {
                Restart-Service -Name $serviceName
                Write-Host "Service $serviceName restarted" -ForegroundColor Green
            }
            catch {
                Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
            }
        }
    }
}

function Create-RestorePoint {
    Write-Host "`n--- System Restore Point ---" -ForegroundColor Yellow
    
    try {
        # Проверка прав администратора
        if (-NOT ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole] "Administrator")) {
            Write-Host "Administrator rights required for this operation" -ForegroundColor Red
            return
        }
        
        $description = Read-Host "Enter restore point description"
        if (-not $description) {
            $description = "PowerShell Configuration Master - $(Get-Date)"
        }
        
        Checkpoint-Computer -Description $description
        Write-Host "System restore point created: $description" -ForegroundColor Green
    }
    catch {
        Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    }
}

function Show-CurrentConfig {
    Write-Host "`n--- Current Configuration ---" -ForegroundColor Yellow
    Write-Host "Computer Name: $env:COMPUTERNAME"
    Write-Host "User: $env:USERNAME"
    Write-Host "Domain: $env:USERDOMAIN"
    Write-Host "OS: $(Get-WmiObject Win32_OperatingSystem).Caption"
    Write-Host "Architecture: $env:PROCESSOR_ARCHITECTURE"
}

# Основной цикл
do {
    $choice = Show-MainMenu
    
    switch ($choice) {
        "1" { Change-ComputerName }
        "2" { Configure-Network }
        "3" { Manage-Services }
        "4" { Create-RestorePoint }
        "5" { Show-CurrentConfig }
        "6" { 
            Write-Host "Goodbye!" -ForegroundColor Green
            break 
        }
        default { Write-Host "Invalid option" -ForegroundColor Red }
    }
    
    if ($choice -ne "6") {
        Read-Host "`nPress Enter to continue..."
    }
} while ($choice -ne "6")
