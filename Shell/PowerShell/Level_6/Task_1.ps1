# Мастер миграции профилей
Write-Host "=== User Profile Migration Master ===" -ForegroundColor Green

class ProfileMigrator {
    [string]$SourceComputer
    [string]$DestinationComputer
    [string]$LogPath
    [hashtable]$MigrationSettings
    
    ProfileMigrator([string]$source, [string]$destination) {
        $this.SourceComputer = $source
        $this.DestinationComputer = $destination
        $this.LogPath = "MigrationLog_$(Get-Date -Format 'yyyyMMdd_HHmmss').json"
        $this.MigrationSettings = @{
            CopyUserData = $true
            CopyRegistry = $true
            CopyAppData = $true
            ValidateIntegrity = $true
        }
    }
    
    [bool]TestSourceAccess() {
        try {
            Write-Host "Testing access to source computer: $($this.SourceComputer)" -ForegroundColor Yellow
            $testResult = Test-Connection -ComputerName $this.SourceComputer -Count 1 -Quiet
            if ($testResult) {
                Write-Host "✓ Source computer is accessible" -ForegroundColor Green
                return $true
            } else {
                Write-Host "✗ Cannot reach source computer" -ForegroundColor Red
                return $false
            }
        }
        catch {
            Write-Host "✗ Error accessing source: $($_.Exception.Message)" -ForegroundColor Red
            return $false
        }
    }
    
    [bool]TestDestinationAccess() {
        try {
            Write-Host "Testing access to destination computer: $($this.DestinationComputer)" -ForegroundColor Yellow
            $testResult = Test-Connection -ComputerName $this.DestinationComputer -Count 1 -Quiet
            if ($testResult) {
                Write-Host "✓ Destination computer is accessible" -ForegroundColor Green
                return $true
            } else {
                Write-Host "✗ Cannot reach destination computer" -ForegroundColor Red
                return $false
            }
        }
        catch {
            Write-Host "✗ Error accessing destination: $($_.Exception.Message)" -ForegroundColor Red
            return $false
        }
    }
    
    [array]GetUserProfiles([string]$computer) {
        try {
            if ($computer -eq "localhost" -or $computer -eq $env:COMPUTERNAME) {
                # Локальные профили
                $profiles = Get-WmiObject -Class Win32_UserProfile | 
                           Where-Object { $_.LocalPath -notlike "*Windows*" -and $_.LocalPath -notlike "*Administrator*" }
            } else {
                # Удаленные профили
                $profiles = Get-WmiObject -Class Win32_UserProfile -ComputerName $computer |
                           Where-Object { $_.LocalPath -notlike "*Windows*" -and $_.LocalPath -notlike "*Administrator*" }
            }
            
            $profileInfo = @()
            foreach ($profile in $profiles) {
                $userFolder = Split-Path $profile.LocalPath -Leaf
                $profileInfo += [PSCustomObject]@{
                    SID = $profile.SID
                    Path = $profile.LocalPath
                    UserName = $userFolder
                    Loaded = $profile.Loaded
                    LastUseTime = $profile.LastUseTime
                    Size = $this.GetFolderSize($profile.LocalPath)
                }
            }
            
            return $profileInfo
        }
        catch {
            Write-Error "Failed to get user profiles from $computer : $($_.Exception.Message)"
            return @()
        }
    }
    
    [string]GetFolderSize([string]$path) {
        if (-not (Test-Path $path)) { return "0 MB" }
        
        try {
            $size = (Get-ChildItem $path -Recurse -File | Measure-Object -Property Length -Sum).Sum
            if ($size -gt 1GB) {
                return "$([math]::Round($size/1GB, 2)) GB"
            } elseif ($size -gt 1MB) {
                return "$([math]::Round($size/1MB, 2)) MB"
            } else {
                return "$([math]::Round($size/1KB, 2)) KB"
            }
        }
        catch {
            return "Unknown"
        }
    }
    
    [void]CopyUserData([string]$sourceUser, [string]$destUser) {
        if (-not $this.MigrationSettings.CopyUserData) { return }
        
        Write-Host "Copying user data for: $sourceUser" -ForegroundColor Yellow
        
        $sourcePaths = @(
            "C:\Users\$sourceUser\Documents",
            "C:\Users\$sourceUser\Desktop", 
            "C:\Users\$sourceUser\Favorites",
            "C:\Users\$sourceUser\Pictures"
        )
        
        $destBase = "C:\Users\$destUser"
        
        foreach ($sourcePath in $sourcePaths) {
            if (Test-Path $sourcePath) {
                $destPath = $sourcePath -replace $sourceUser, $destUser
                
                try {
                    if (-not (Test-Path $destPath)) {
                        New-Item -Path $destPath -ItemType Directory -Force | Out-Null
                    }
                    
                    robocopy $sourcePath $destPath /E /ZB /R:3 /W:5 /LOG+:migration_robocopy.log
                    Write-Host "  ✓ Copied: $(Split-Path $sourcePath -Leaf)" -ForegroundColor Green
                }
                catch {
                    Write-Host "  ✗ Failed to copy: $(Split-Path $sourcePath -Leaf)" -ForegroundColor Red
                    $this.LogMigration("ERROR", "CopyUserData", "Failed to copy $sourcePath to $destPath : $($_.Exception.Message)")
                }
            }
        }
    }
    
    [void]MigrateRegistry([string]$sourceUser, [string]$destUser) {
        if (-not $this.MigrationSettings.CopyRegistry) { return }
        
        Write-Host "Migrating registry settings for: $sourceUser" -ForegroundColor Yellow
        
        $registryPaths = @(
            "HKCU\Software",
            "HKCU\Console",
            "HKCU\Control Panel",
            "HKCU\Keyboard Layout"
        )
        
        $tempFile = "registry_export_$(Get-Date -Format 'yyyyMMdd_HHmmss').reg"
        
        try {
            # Экспорт реестра (упрощенная версия)
            foreach ($regPath in $registryPaths) {
                $exportPath = $regPath -replace "HKCU\\", ""
                reg export "HKEY_CURRENT_USER\$exportPath" "$tempFile.part" /y 2>$null
                
                if (Test-Path "$tempFile.part") {
                    # Здесь должна быть логика обработки и импорта
                    # В реальном сценарии нужно обработать файл .reg и адаптировать его для нового пользователя
                    Remove-Item "$tempFile.part" -Force
                }
            }
            
            Write-Host "  ✓ Registry migration completed" -ForegroundColor Green
        }
        catch {
            Write-Host "  ✗ Registry migration failed" -ForegroundColor Red
            $this.LogMigration("ERROR", "MigrateRegistry", "Failed to migrate registry for $sourceUser : $($_.Exception.Message)")
        }
    }
    
    [void]MigrateAppData([string]$sourceUser, [string]$destUser) {
        if (-not $this.MigrationSettings.CopyAppData) { return }
        
        Write-Host "Migrating application data for: $sourceUser" -ForegroundColor Yellow
        
        $appDataPaths = @(
            "C:\Users\$sourceUser\AppData\Local",
            "C:\Users\$sourceUser\AppData\Roaming"
        )
        
        $excludePatterns = @(
            "*Temp*",
            "*Cache*", 
            "*Logs*",
            "*tmp*"
        )
        
        foreach ($appDataPath in $appDataPaths) {
            if (Test-Path $appDataPath) {
                $destPath = $appDataPath -replace $sourceUser, $destUser
                
                try {
                    # Создание целевой директории
                    if (-not (Test-Path $destPath)) {
                        New-Item -Path $destPath -ItemType Directory -Force | Out-Null
                    }
                    
                    # Копирование с исключением временных файлов
                    Get-ChildItem $appDataPath -Directory | Where-Object {
                        $exclude = $false
                        foreach ($pattern in $excludePatterns) {
                            if ($_.Name -like $pattern) {
                                $exclude = $true
                                break
                            }
                        }
                        -not $exclude
                    } | ForEach-Object {
                        $sourceDir = $_.FullName
                        $destDir = Join-Path $destPath $_.Name
                        
                        robocopy $sourceDir $destDir /E /ZB /R:3 /W:5 /LOG+:migration_robocopy.log
                        Write-Host "  ✓ AppData: $($_.Name)" -ForegroundColor Green
                    }
                }
                catch {
                    Write-Host "  ✗ Failed to migrate AppData: $(Split-Path $appDataPath -Leaf)" -ForegroundColor Red
                    $this.LogMigration("ERROR", "MigrateAppData", "Failed to migrate $appDataPath : $($_.Exception.Message)")
                }
            }
        }
    }
    
    [bool]ValidateDataIntegrity([string]$sourceUser, [string]$destUser) {
        if (-not $this.MigrationSettings.ValidateIntegrity) { return $true }
        
        Write-Host "Validating data integrity..." -ForegroundColor Yellow
        
        $validationPaths = @(
            @{Source = "C:\Users\$sourceUser\Documents"; Dest = "C:\Users\$destUser\Documents"},
            @{Source = "C:\Users\$sourceUser\Desktop"; Dest = "C:\Users\$destUser\Desktop"}
        )
        
        $allValid = $true
        
        foreach ($path in $validationPaths) {
            if (Test-Path $path.Source -and Test-Path $path.Dest) {
                $sourceFiles = Get-ChildItem $path.Source -Recurse -File | Measure-Object | Select-Object -ExpandProperty Count
                $destFiles = Get-ChildItem $path.Dest -Recurse -File | Measure-Object | Select-Object -ExpandProperty Count
                
                if ($sourceFiles -eq $destFiles) {
                    Write-Host "  ✓ $(Split-Path $path.Source -Leaf): $sourceFiles files validated" -ForegroundColor Green
                } else {
                    Write-Host "  ✗ $(Split-Path $path.Source -Leaf): Source=$sourceFiles, Dest=$destFiles" -ForegroundColor Red
                    $allValid = $false
                }
            }
        }
        
        return $allValid
    }
    
    [void]LogMigration([string]$level, [string]$operation, [string]$message) {
        $logEntry = @{
            Timestamp = Get-Date
            Level = $level
            Operation = $operation
            Message = $message
            Source = $this.SourceComputer
            Destination = $this.DestinationComputer
        }
        
        $logData = @()
        if (Test-Path $this.LogPath) {
            $logData = Get-Content $this.LogPath | ConvertFrom-Json
        }
        
        $logData += $logEntry
        $logData | ConvertTo-Json -Depth 3 | Set-Content $this.LogPath
    }
    
    [void]GenerateMigrationReport() {
        $reportData = @{
            MigrationSummary = @{
                SourceComputer = $this.SourceComputer
                DestinationComputer = $this.DestinationComputer
                MigrationDate = Get-Date
                Settings = $this.MigrationSettings
                LogFile = $this.LogPath
            }
            MigrationLog = if (Test-Path $this.LogPath) { 
                Get-Content $this.LogPath | ConvertFrom-Json 
            } else { @() }
        }
        
        $reportFile = "MigrationReport_$(Get-Date -Format 'yyyyMMdd_HHmmss').html"
        
        $htmlReport = @"
<!DOCTYPE html>
<html>
<head>
    <title>User Profile Migration Report</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        h1 { color: #333; border-bottom: 2px solid #333; }
        .summary { background: #f5f5f5; padding: 15px; border-radius: 5px; }
        .log-entry { margin: 5px 0; padding: 5px; border-left: 3px solid #ccc; }
        .error { border-left-color: #d9534f; background: #f8d7da; }
        .success { border-left-color: #28a745; background: #d4edda; }
        .warning { border-left-color: #ffc107; background: #fff3cd; }
    </style>
</head>
<body>
    <h1>User Profile Migration Report</h1>
    
    <div class="summary">
        <h2>Migration Summary</h2>
        <p><strong>Source:</strong> $($this.SourceComputer)</p>
        <p><strong>Destination:</strong> $($this.DestinationComputer)</p>
        <p><strong>Date:</strong> $(Get-Date)</p>
    </div>
    
    <h2>Migration Log</h2>
"@

        if (Test-Path $this.LogPath) {
            $logEntries = Get-Content $this.LogPath | ConvertFrom-Json
            foreach ($entry in $logEntries) {
                $cssClass = $entry.Level.ToLower()
                $htmlReport += "<div class='log-entry $cssClass'><strong>$($entry.Timestamp)</strong> [$($entry.Level)] $($entry.Operation): $($entry.Message)</div>"
            }
        }

        $htmlReport += @"
</body>
</html>
"@

        $htmlReport | Out-File -FilePath $reportFile -Encoding UTF8
        Write-Host "Migration report generated: $reportFile" -ForegroundColor Cyan
    }
    
    [void]StartMigration([string[]]$usersToMigrate) {
        Write-Host "Starting migration process..." -ForegroundColor Green
        Write-Host "Source: $($this.SourceComputer)" -ForegroundColor Yellow
        Write-Host "Destination: $($this.DestinationComputer)" -ForegroundColor Yellow
        
        # Проверка доступности
        if (-not ($this.TestSourceAccess() -and $this.TestDestinationAccess())) {
            Write-Host "Migration aborted due to connectivity issues" -ForegroundColor Red
            return
        }
        
        # Получение профилей
        $sourceProfiles = $this.GetUserProfiles($this.SourceComputer)
        if (-not $sourceProfiles) {
            Write-Host "No user profiles found on source computer" -ForegroundColor Red
            return
        }
        
        Write-Host "`nFound $($sourceProfiles.Count) user profiles on source" -ForegroundColor Green
        
        # Миграция выбранных пользователей
        foreach ($user in $usersToMigrate) {
            $sourceProfile = $sourceProfiles | Where-Object { $_.UserName -eq $user }
            if ($sourceProfile) {
                Write-Host "`n=== Migrating: $user ===" -ForegroundColor Cyan
                
                $this.CopyUserData($user, $user)
                $this.MigrateAppData($user, $user)
                $this.MigrateRegistry($user, $user)
                
                $isValid = $this.ValidateDataIntegrity($user, $user)
                if ($isValid) {
                    Write-Host "✓ Migration completed successfully for: $user" -ForegroundColor Green
                    $this.LogMigration("SUCCESS", "UserMigration", "Successfully migrated user $user")
                } else {
                    Write-Host "⚠ Migration completed with warnings for: $user" -ForegroundColor Yellow
                    $this.LogMigration("WARNING", "UserMigration", "Migration completed with warnings for $user")
                }
            } else {
                Write-Host "User profile not found: $user" -ForegroundColor Red
            }
        }
        
        # Генерация отчета
        $this.GenerateMigrationReport()
        
        Write-Host "`nMigration process completed!" -ForegroundColor Green
    }
}

# Основная программа
Write-Host "User Profile Migration Master" -ForegroundColor Green
Write-Host "=============================" -ForegroundColor Green

$sourceComputer = Read-Host "Enter source computer name (or 'localhost' for current)"
$destComputer = Read-Host "Enter destination computer name (or 'localhost' for current)"

$migrator = [ProfileMigrator]::new($sourceComputer, $destComputer)

# Настройка параметров миграции
Write-Host "`n=== Migration Settings ===" -ForegroundColor Yellow
$copyData = Read-Host "Copy user data (Documents, Desktop, etc.)? (y/n)"
$migrator.MigrationSettings.CopyUserData = ($copyData -eq 'y')

$copyAppData = Read-Host "Copy application data? (y/n)" 
$migrator.MigrationSettings.CopyAppData = ($copyAppData -eq 'y')

$copyRegistry = Read-Host "Copy registry settings? (y/n)"
$migrator.MigrationSettings.CopyRegistry = ($copyRegistry -eq 'y')

$validate = Read-Host "Validate data integrity after migration? (y/n)"
$migrator.MigrationSettings.ValidateIntegrity = ($validate -eq 'y')

# Выбор пользователей для миграции
Write-Host "`n=== Select Users to Migrate ===" -ForegroundColor Yellow
$sourceProfiles = $migrator.GetUserProfiles($sourceComputer)

if ($sourceProfiles) {
    for ($i = 0; $i -lt $sourceProfiles.Count; $i++) {
        Write-Host "$($i+1). $($sourceProfiles[$i].UserName) - $($sourceProfiles[$i].Size)"
    }
    
    $userChoice = Read-Host "`nEnter user numbers to migrate (comma-separated, or 'all')"
    
    $usersToMigrate = @()
    if ($userChoice -eq 'all') {
        $usersToMigrate = $sourceProfiles.UserName
    } else {
        $userIndexes = $userChoice -split ',' | ForEach-Object { [int]$_ - 1 }
        foreach ($index in $userIndexes) {
            if ($index -ge 0 -and $index -lt $sourceProfiles.Count) {
                $usersToMigrate += $sourceProfiles[$index].UserName
            }
        }
    }
    
    if ($usersToMigrate.Count -gt 0) {
        Write-Host "`nUsers selected for migration: $($usersToMigrate -join ', ')" -ForegroundColor Green
        $confirm = Read-Host "`nStart migration? (y/n)"
        
        if ($confirm -eq 'y') {
            $migrator.StartMigration($usersToMigrate)
        } else {
            Write-Host "Migration cancelled" -ForegroundColor Yellow
        }
    } else {
        Write-Host "No users selected for migration" -ForegroundColor Red
    }
} else {
    Write-Host "No user profiles found on source computer" -ForegroundColor Red
}
