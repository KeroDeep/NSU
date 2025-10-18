# Система контроля версий файлов
Write-Host "=== File Version Control System ===" -ForegroundColor Green

class FileVersion {
    [string]$FilePath
    [string]$Hash
    [datetime]$Timestamp
    [long]$Size
    [string]$Content
    
    FileVersion([string]$path) {
        $this.FilePath = $path
        $this.Update()
    }
    
    [void]Update() {
        if (Test-Path $this.FilePath) {
            $this.Timestamp = Get-Date
            $this.Size = (Get-Item $this.FilePath).Length
            $this.Content = Get-Content $this.FilePath -Raw
            $this.Hash = (Get-FileHash $this.FilePath -Algorithm MD5).Hash
        }
    }
}

class VersionControlSystem {
    [hashtable]$FileVersions
    [string]$BackupDir
    
    VersionControlSystem([string]$backupPath) {
        $this.FileVersions = @{}
        $this.BackupDir = $backupPath
        if (-not (Test-Path $backupPath)) {
            New-Item -Path $backupPath -ItemType Directory -Force | Out-Null
        }
    }
    
    [void]TrackFile([string]$filePath) {
        if (Test-Path $filePath) {
            $this.FileVersions[$filePath] = @()
            $this.CreateSnapshot($filePath)
            Write-Host "Started tracking: $filePath" -ForegroundColor Green
        } else {
            Write-Host "File not found: $filePath" -ForegroundColor Red
        }
    }
    
    [void]CreateSnapshot([string]$filePath) {
        if ($this.FileVersions.ContainsKey($filePath)) {
            $version = [FileVersion]::new($filePath)
            $this.FileVersions[$filePath] += $version
            
            # Создание backup копии
            $backupFile = Join-Path $this.BackupDir "$(Split-Path $filePath -Leaf).backup.$(Get-Date -Format 'yyyyMMdd_HHmmss')"
            Copy-Item $filePath $backupFile -Force
            
            Write-Host "Snapshot created for: $filePath" -ForegroundColor Cyan
        }
    }
    
    [void]CompareVersions([string]$filePath) {
        if (-not $this.FileVersions.ContainsKey($filePath)) {
            Write-Host "File not being tracked: $filePath" -ForegroundColor Red
            return
        }
        
        $versions = $this.FileVersions[$filePath]
        if ($versions.Count -lt 2) {
            Write-Host "Only one version available for comparison" -ForegroundColor Yellow
            return
        }
        
        $current = $versions[-1]
        $previous = $versions[-2]
        
        Write-Host "`n=== File Changes: $filePath ===" -ForegroundColor Yellow
        Write-Host "Current: $($current.Timestamp)" -ForegroundColor Green
        Write-Host "Previous: $($previous.Timestamp)" -ForegroundColor Gray
        
        # Определение типа изменений
        if ($current.Hash -eq $previous.Hash) {
            Write-Host "No changes detected" -ForegroundColor Green
        } else {
            Write-Host "Changes detected:" -ForegroundColor Yellow
            
            if ($current.Size -gt $previous.Size) {
                Write-Host "  - Content added (+$($current.Size - $previous.Size) bytes)" -ForegroundColor Green
            } elseif ($current.Size -lt $previous.Size) {
                Write-Host "  - Content removed (-$($previous.Size - $current.Size) bytes)" -ForegroundColor Red
            } else {
                Write-Host "  - Content modified (same size)" -ForegroundColor Yellow
            }
            
            # Простое сравнение содержимого
            $currentLines = $current.Content -split "`n"
            $previousLines = $previous.Content -split "`n"
            
            $added = Compare-Object $previousLines $currentLines | Where-Object SideIndicator -eq "=>"
            $removed = Compare-Object $previousLines $currentLines | Where-Object SideIndicator -eq "<="
            
            if ($added) {
                Write-Host "  - Lines added: $($added.Count)" -ForegroundColor Green
            }
            if ($removed) {
                Write-Host "  - Lines removed: $($removed.Count)" -ForegroundColor Red
            }
        }
    }
    
    [void]ShowChangeLog([string]$filePath) {
        if (-not $this.FileVersions.ContainsKey($filePath)) {
            Write-Host "File not being tracked: $filePath" -ForegroundColor Red
            return
        }
        
        Write-Host "`n=== Change Log: $filePath ===" -ForegroundColor Yellow
        $versions = $this.FileVersions[$filePath]
        
        for ($i = 0; $i -lt $versions.Count; $i++) {
            $version = $versions[$i]
            Write-Host "Version $($i+1): $($version.Timestamp)" -ForegroundColor Gray
            Write-Host "  Size: $($version.Size) bytes, Hash: $($version.Hash.Substring(0,8))..." -ForegroundColor Gray
        }
    }
    
    [void]Rollback([string]$filePath, [int]$versionNumber) {
        if (-not $this.FileVersions.ContainsKey($filePath)) {
            Write-Host "File not being tracked: $filePath" -ForegroundColor Red
            return
        }
        
        $versions = $this.FileVersions[$filePath]
        if ($versionNumber -gt $versions.Count -or $versionNumber -lt 1) {
            Write-Host "Invalid version number" -ForegroundColor Red
            return
        }
        
        $targetVersion = $versions[$versionNumber - 1]
        
        # Восстановление из backup
        $backupFiles = Get-ChildItem $this.BackupDir -Filter "$(Split-Path $filePath -Leaf).backup.*" | Sort-Object LastWriteTime
        if ($backupFiles.Count -ge $versionNumber) {
            $backupFile = $backupFiles[$versionNumber - 1].FullName
            Copy-Item $backupFile $filePath -Force
            Write-Host "Rolled back to version $versionNumber" -ForegroundColor Green
            $this.CreateSnapshot($filePath) # Создать новую версию после отката
        } else {
            Write-Host "Backup file not found for version $versionNumber" -ForegroundColor Red
        }
    }
}

# Основная программа
$vcs = [VersionControlSystem]::new("FileBackups")

function Show-MainMenu {
    Write-Host "`n=== File Version Control ===" -ForegroundColor Green
    Write-Host "1. Track new file"
    Write-Host "2. Create snapshot"
    Write-Host "3. Compare versions" 
    Write-Host "4. Show change log"
    Write-Host "5. Rollback to previous version"
    Write-Host "6. Exit"
    
    return Read-Host "`nSelect option (1-6)"
}

do {
    $choice = Show-MainMenu
    
    switch ($choice) {
        "1" {
            $filePath = Read-Host "Enter file path to track"
            $vcs.TrackFile($filePath)
        }
        "2" {
            $filePath = Read-Host "Enter file path for snapshot"
            $vcs.CreateSnapshot($filePath)
        }
        "3" {
            $filePath = Read-Host "Enter file path to compare"
            $vcs.CompareVersions($filePath)
        }
        "4" {
            $filePath = Read-Host "Enter file path for change log"
            $vcs.ShowChangeLog($filePath)
        }
        "5" {
            $filePath = Read-Host "Enter file path to rollback"
            $version = Read-Host "Enter version number to restore"
            $vcs.Rollback($filePath, [int]$version)
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
