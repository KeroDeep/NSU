# Конфигурационный менеджер
Write-Host "=== Configuration Manager ===" -ForegroundColor Green

class ConfigurationManager {
    [string]$ConfigBasePath
    [hashtable]$ServerGroups
    [string]$VersionHistoryPath
    
    ConfigurationManager([string]$basePath) {
        $this.ConfigBasePath = $basePath
        $this.VersionHistoryPath = Join-Path $basePath "VersionHistory"
        $this.ServerGroups = @{}
        
        # Создание структуры директорий
        $this.InitializeDirectoryStructure()
    }
    
    [void]InitializeDirectoryStructure() {
        $directories = @(
            $this.ConfigBasePath,
            $this.VersionHistoryPath,
            "$($this.ConfigBasePath)\Templates",
            "$($this.ConfigBasePath)\Applied",
            "$($this.ConfigBasePath)\Backups"
        )
        
        foreach ($dir in $directories) {
            if (-not (Test-Path $dir)) {
                New-Item -Path $dir -ItemType Directory -Force | Out-Null
            }
        }
        
        Write-Host "Configuration directory structure initialized" -ForegroundColor Green
    }
    
    [void]CreateConfigurationTemplate([string]$templateName, [hashtable]$configSettings) {
        $templatePath = "$($this.ConfigBasePath)\Templates\$templateName.json"
        
        $template = @{
            Name = $templateName
            Created = Get-Date
            Settings = $configSettings
            Variables = @()
        }
        
        # Автоматическое определение переменных
        foreach ($setting in $configSettings.GetEnumerator()) {
            if ($setting.Value -is [string] -and $setting.Value -match "{{\w+}}") {
                $matches[0] -match "{{(\w+)}}"
                $variableName = $matches[1]
                if ($variableName -notin $template.Variables) {
                    $template.Variables += $variableName
                }
            }
        }
        
        $template | ConvertTo-Json -Depth 5 | Set-Content $templatePath
        Write-Host "Configuration template created: $templatePath" -ForegroundColor Green
    }
    
    [void]ApplyConfigurationToServers([string]$templateName, [string[]]$servers, [hashtable]$variableValues) {
        $templatePath = "$($this.ConfigBasePath)\Templates\$templateName.json"
        
        if (-not (Test-Path $templatePath)) {
            Write-Host "Template not found: $templateName" -ForegroundColor Red
            return
        }
        
        $template = Get-Content $templatePath | ConvertFrom-Json
        $results = @()
        
        Write-Host "Applying configuration template '$templateName' to $($servers.Count) servers..." -ForegroundColor Yellow
        
        foreach ($server in $servers) {
            Write-Host "`nProcessing server: $server" -ForegroundColor Cyan
            
            try {
                # Проверка доступности сервера
                if (-not (Test-Connection -ComputerName $server -Count 1 -Quiet)) {
                    Write-Host "  ✗ Server is not reachable" -ForegroundColor Red
                    $results += @{Server = $server; Status = "Failed"; Error = "Server unreachable"}
                    continue
                }
                
                # Создание конфигурации с подстановкой переменных
                $appliedConfig = $this.ApplyTemplateVariables($template.Settings, $variableValues)
                
                # Валидация конфигурации
                if (-not $this.ValidateConfiguration($appliedConfig)) {
                    Write-Host "  ✗ Configuration validation failed" -ForegroundColor Red
                    $results += @{Server = $server; Status = "Failed"; Error = "Configuration validation failed"}
                    continue
                }
                
                # Применение конфигурации
                $applyResult = $this.ApplyConfiguration($server, $appliedConfig)
                
                if ($applyResult.Success) {
                    Write-Host "  ✓ Configuration applied successfully" -ForegroundColor Green
                    
                    # Сохранение примененной конфигурации
                    $this.SaveAppliedConfiguration($server, $templateName, $appliedConfig)
                    
                    $results += @{Server = $server; Status = "Success"; AppliedConfig = $appliedConfig}
                } else {
                    Write-Host "  ✗ Failed to apply configuration: $($applyResult.Error)" -ForegroundColor Red
                    $results += @{Server = $server; Status = "Failed"; Error = $applyResult.Error}
                }
            }
            catch {
                Write-Host "  ✗ Error processing server: $($_.Exception.Message)" -ForegroundColor Red
                $results += @{Server = $server; Status = "Failed"; Error = $_.Exception.Message}
            }
        }
        
        # Сохранение результатов
        $this.SaveApplicationResults($templateName, $results)
        $this.DisplayApplicationSummary($results)
    }
    
    [hashtable]ApplyTemplateVariables([hashtable]$settings, [hashtable]$variableValues) {
        $appliedSettings = @{}
        
        foreach ($setting in $settings.GetEnumerator()) {
            $value = $setting.Value
            
            if ($value -is [string]) {
                # Замена переменных в строковых значениях
                foreach ($var in $variableValues.GetEnumerator()) {
                    $value = $value -replace "{{$($var.Key)}}", $var.Value
                }
            }
            elseif ($value -is [hashtable]) {
                # Рекурсивная обработка вложенных хеш-таблиц
                $value = $this.ApplyTemplateVariables($value, $variableValues)
            }
            
            $appliedSettings[$setting.Key] = $value
        }
        
        return $appliedSettings
    }
    
    [bool]ValidateConfiguration([hashtable]$config) {
        # Базовая валидация конфигурации
        $isValid = $true
        
        # Проверка обязательных полей
        $requiredFields = @("ServiceName", "ConfigType")
        foreach ($field in $requiredFields) {
            if (-not $config.ContainsKey($field)) {
                Write-Host "  Missing required field: $field" -ForegroundColor Red
                $isValid = $false
            }
        }
        
        # Проверка значений
        if ($config.ContainsKey("Port") -and $config.Port -is [int]) {
            if ($config.Port -lt 1 -or $config.Port -gt 65535) {
                Write-Host "  Invalid port number: $($config.Port)" -ForegroundColor Red
                $isValid = $false
            }
        }
        
        return $isValid
    }
    
    [hashtable]ApplyConfiguration([string]$server, [hashtable]$config) {
        try {
            # Создание backup текущей конфигурации
            $backupCreated = $this.CreateConfigurationBackup($server, $config)
            
            # Применение конфигурации в зависимости от типа
            switch ($config.ConfigType) {
                "Service" {
                    $this.ConfigureService($server, $config)
                }
                "Registry" {
                    $this.ConfigureRegistry($server, $config)
                }
                "File" {
                    $this.ConfigureFiles($server, $config)
                }
                default {
                    throw "Unknown configuration type: $($config.ConfigType)"
                }
            }
            
            return @{Success = $true; BackupCreated = $backupCreated}
        }
        catch {
            # Откат изменений в случае ошибки
            if ($backupCreated) {
                $this.RollbackConfiguration($server, $config)
            }
            
            return @{Success = $false; Error = $_.Exception.Message}
        }
    }
    
    [bool]CreateConfigurationBackup([string]$server, [hashtable]$config) {
        $backupPath = "$($this.ConfigBasePath)\Backups\$server"
        if (-not (Test-Path $backupPath)) {
            New-Item -Path $backupPath -ItemType Directory -Force | Out-Null
        }
        
        $backupFile = Join-Path $backupPath "$($config.ServiceName)_backup_$(Get-Date -Format 'yyyyMMdd_HHmmss').json"
        
        try {
            # Получение текущей конфигурации (упрощенная версия)
            $currentConfig = @{
                Server = $server
                ServiceName = $config.ServiceName
                BackupTime = Get-Date
                ConfigType = $config.ConfigType
            }
            
            $currentConfig | ConvertTo-Json -Depth 3 | Set-Content $backupFile
            return $true
        }
        catch {
            Write-Host "  Warning: Could not create backup" -ForegroundColor Yellow
            return $false
        }
    }
    
    [void]ConfigureService([string]$server, [hashtable]$config) {
        if ($server -eq "localhost" -or $server -eq $env:COMPUTERNAME) {
            $service = Get-Service -Name $config.ServiceName -ErrorAction Stop
            
            if ($config.ContainsKey("StartupType")) {
                Set-Service -Name $config.ServiceName -StartupType $config.StartupType
            }
            
            if ($config.ContainsKey("Status") -and $config.Status -ne $service.Status) {
                if ($config.Status -eq "Running") {
                    Start-Service -Name $config.ServiceName
                } else {
                    Stop-Service -Name $config.ServiceName -Force
                }
            }
        } else {
            # Удаленная настройка службы
            Invoke-Command -ComputerName $server -ScriptBlock {
                param($config)
                $service = Get-Service -Name $config.ServiceName -ErrorAction Stop
                
                if ($config.ContainsKey("StartupType")) {
                    Set-Service -Name $config.ServiceName -StartupType $config.StartupType
                }
                
                if ($config.ContainsKey("Status") -and $config.Status -ne $service.Status) {
                    if ($config.Status -eq "Running") {
                        Start-Service -Name $config.ServiceName
                    } else {
                        Stop-Service -Name $config.ServiceName -Force
                    }
                }
            } -ArgumentList $config
        }
    }
    
    [void]ConfigureRegistry([string]$server, [hashtable]$config) {
        if ($config.ContainsKey("RegistryPaths")) {
            foreach ($regPath in $config.RegistryPaths) {
                if ($server -eq "localhost" -or $server -eq $env:COMPUTERNAME) {
                    foreach ($value in $regPath.Values) {
                        if (Test-Path $regPath.Path) {
                            Set-ItemProperty -Path $regPath.Path -Name $value.Name -Value $value.Value
                        } else {
                            New-Item -Path $regPath.Path -Force | Out-Null
                            New-ItemProperty -Path $regPath.Path -Name $value.Name -Value $value.Value -PropertyType $value.Type
                        }
                    }
                } else {
                    # Удаленная настройка реестра
                    Invoke-Command -ComputerName $server -ScriptBlock {
                        param($regPath)
                        foreach ($value in $regPath.Values) {
                            if (Test-Path $regPath.Path) {
                                Set-ItemProperty -Path $regPath.Path -Name $value.Name -Value $value.Value
                            } else {
                                New-Item -Path $regPath.Path -Force | Out-Null
                                New-ItemProperty -Path $regPath.Path -Name $value.Name -Value $value.Value -PropertyType $value.Type
                            }
                        }
                    } -ArgumentList $regPath
                }
            }
        }
    }
    
    [void]ConfigureFiles([string]$server, [hashtable]$config) {
        if ($config.ContainsKey("Files")) {
            foreach ($fileConfig in $config.Files) {
                if ($server -eq "localhost" -or $server -eq $env:COMPUTERNAME) {
                    if (-not (Test-Path (Split-Path $fileConfig.Path -Parent))) {
                        New-Item -Path (Split-Path $fileConfig.Path -Parent) -ItemType Directory -Force | Out-Null
                    }
                    
                    Set-Content -Path $fileConfig.Path -Value $fileConfig.Content
                } else {
                    # Удаленная настройка файлов
                    Invoke-Command -ComputerName $server -ScriptBlock {
                        param($fileConfig)
                        if (-not (Test-Path (Split-Path $fileConfig.Path -Parent))) {
                            New-Item -Path (Split-Path $fileConfig.Path -Parent) -ItemType Directory -Force | Out-Null
                        }
                        Set-Content -Path $fileConfig.Path -Value $fileConfig.Content
                    } -ArgumentList $fileConfig
                }
            }
        }
    }
    
    [void]RollbackConfiguration([string]$server, [hashtable]$config) {
        Write-Host "  Rolling back configuration for: $server" -ForegroundColor Yellow
        
        $backupPath = "$($this.ConfigBasePath)\Backups\$server"
        $backupFiles = Get-ChildItem $backupPath -Filter "*$($config.ServiceName)*" | Sort-Object LastWriteTime -Descending
        
        if ($backupFiles.Count -gt 0) {
            $latestBackup = $backupFiles[0].FullName
            $backupConfig = Get-Content $latestBackup | ConvertFrom-Json
            
            # Восстановление из backup
            $this.ApplyConfiguration($server, $backupConfig)
            Write-Host "  ✓ Configuration rolled back successfully" -ForegroundColor Green
        } else {
            Write-Host "  ✗ No backup found for rollback" -ForegroundColor Red
        }
    }
    
    [void]SaveAppliedConfiguration([string]$server, [string]$templateName, [hashtable]$config) {
        $appliedPath = "$($this.ConfigBasePath)\Applied\$server"
        if (-not (Test-Path $appliedPath)) {
            New-Item -Path $appliedPath -ItemType Directory -Force | Out-Null
        }
        
        $appliedConfig = @{
            Server = $server
            Template = $templateName
            AppliedDate = Get-Date
            Configuration = $config
        }
        
        $appliedFile = Join-Path $appliedPath "$($templateName)_applied_$(Get-Date -Format 'yyyyMMdd_HHmmss').json"
        $appliedConfig | ConvertTo-Json -Depth 5 | Set-Content $appliedFile
    }
    
    [void]SaveApplicationResults([string]$templateName, [array]$results) {
        $resultsFile = "$($this.ConfigBasePath)\$($templateName)_results_$(Get-Date -Format 'yyyyMMdd_HHmmss').json"
        $results | ConvertTo-Json -Depth 3 | Set-Content $resultsFile
    }
    
    [void]DisplayApplicationSummary([array]$results) {
        $successCount = ($results | Where-Object { $_.Status -eq "Success" }).Count
        $failedCount = ($results | Where-Object { $_.Status -eq "Failed" }).Count
        
        Write-Host "`n=== Application Summary ===" -ForegroundColor Yellow
        Write-Host "Successful: $successCount" -ForegroundColor Green
        Write-Host "Failed: $failedCount" -ForegroundColor Red
        
        if ($failedCount -gt 0) {
            Write-Host "`nFailed servers:" -ForegroundColor Red
            $results | Where-Object { $_.Status -eq "Failed" } | ForEach-Object {
                Write-Host "  $($_.Server): $($_.Error)" -ForegroundColor Red
            }
        }
    }
    
    [void]ManageConfigurationVersions([string]$templateName) {
        $versionPath = "$($this.VersionHistoryPath)\$templateName"
        if (-not (Test-Path $versionPath)) {
            New-Item -Path $versionPath -ItemType Directory -Force | Out-Null
        }
        
        $templatePath = "$($this.ConfigBasePath)\Templates\$templateName.json"
        if (Test-Path $templatePath) {
            $versionFile = Join-Path $versionPath "$($templateName)_v$(Get-Date -Format 'yyyyMMdd_HHmmss').json"
            Copy-Item $templatePath $versionFile
            Write-Host "Version saved: $versionFile" -ForegroundColor Green
        }
    }
    
    [void]ShowConfigurationHistory([string]$templateName) {
        $versionPath = "$($this.VersionHistoryPath)\$templateName"
        if (Test-Path $versionPath) {
            $versions = Get-ChildItem $versionPath -Filter "*.json" | Sort-Object LastWriteTime -Descending
            
            Write-Host "`n=== Configuration History: $templateName ===" -ForegroundColor Yellow
            foreach ($version in $versions) {
                Write-Host "  $($version.Name) - $($version.LastWriteTime)" -ForegroundColor Gray
            }
        } else {
            Write-Host "No version history found for: $templateName" -ForegroundColor Yellow
        }
    }
}

# Демонстрация работы конфигурационного менеджера
function Show-Demo {
    $configManager = [ConfigurationManager]::new("C:\ConfigManager")
    
    # Создание тестового шаблона конфигурации
    $testConfig = @{
        ConfigType = "Service"
        ServiceName = "Spooler"
        StartupType = "Automatic"
        Status = "Running"
        Description = "Print Spooler Service"
    }
    
    $configManager.CreateConfigurationTemplate("PrintService", $testConfig)
    
    # Создание шаблона с переменными
    $webConfig = @{
        ConfigType = "Service"
        ServiceName = "{{ServiceName}}"
        StartupType = "{{StartupType}}"
        Status = "{{Status}}"
        Port = 8080
    }
    
    $configManager.CreateConfigurationTemplate("WebService", $webConfig)
    
    Write-Host "Demo templates created successfully!" -ForegroundColor Green
}

# Основное меню
function Show-MainMenu {
    Write-Host "`n=== Configuration Manager ===" -ForegroundColor Green
    Write-Host "1. Create configuration template"
    Write-Host "2. Apply configuration to servers"
    Write-Host "3. Manage configuration versions"
    Write-Host "4. Show configuration history"
    Write-Host "5. Run demo"
    Write-Host "6. Exit"
    
    return Read-Host "`nSelect option (1-6)"
}

# Инициализация менеджера
$configBase = "C:\ConfigManager"
$configManager = [ConfigurationManager]::new($configBase)

Write-Host "Configuration Manager initialized" -ForegroundColor Green
Write-Host "Configuration base: $configBase" -ForegroundColor Cyan

do {
    $choice = Show-MainMenu
    
    switch ($choice) {
        "1" {
            Write-Host "`n--- Create Configuration Template ---" -ForegroundColor Yellow
            $templateName = Read-Host "Template name"
            
            # Упрощенное создание шаблона
            $configSettings = @{
                ConfigType = Read-Host "Configuration type (Service/Registry/File)"
                ServiceName = Read-Host "Service name (if applicable)"
                Description = Read-Host "Description"
            }
            
            $configManager.CreateConfigurationTemplate($templateName, $configSettings)
        }
        "2" {
            Write-Host "`n--- Apply Configuration ---" -ForegroundColor Yellow
            $templateName = Read-Host "Template name"
            $serversInput = Read-Host "Server names (comma-separated)"
            $servers = $serversInput -split ',' | ForEach-Object { $_.Trim() }
            
            # Переменные для подстановки
            $variables = @{}
            $addVariables = Read-Host "Add template variables? (y/n)"
            if ($addVariables -eq 'y') {
                do {
                    $varName = Read-Host "Variable name (or 'done' to finish)"
                    if ($varName -ne 'done') {
                        $varValue = Read-Host "Variable value"
                        $variables[$varName] = $varValue
                    }
                } while ($varName -ne 'done')
            }
            
            $configManager.ApplyConfigurationToServers($templateName, $servers, $variables)
        }
        "3" {
            Write-Host "`n--- Manage Configuration Versions ---" -ForegroundColor Yellow
            $templateName = Read-Host "Template name"
            $configManager.ManageConfigurationVersions($templateName)
        }
        "4" {
            Write-Host "`n--- Configuration History ---" -ForegroundColor Yellow
            $templateName = Read-Host "Template name"
            $configManager.ShowConfigurationHistory($templateName)
        }
        "5" {
            Show-Demo
        }
        "6" {
            Write-Host "Goodbye!" -ForegroundColor Green
        }
        default {
            Write-Host "Invalid option" -ForegroundColor Red
        }
    }
    
    if ($choice -ne "6") {
        Read-Host "`nPress Enter to continue..."
    }
} while ($choice -ne "6")
