# Управление файловой системой
Write-Host "=== File System Manager ===" -ForegroundColor Green

# Создание сложной структуры директорий
$basePath = "TestProject"
Write-Host "Creating directory structure..." -ForegroundColor Yellow

$directories = @(
    "$basePath\Source\Code",
    "$basePath\Source\Config",
    "$basePath\Docs\Technical",
    "$basePath\Docs\User",
    "$basePath\Backup"
)

foreach ($dir in $directories) {
    New-Item -Path $dir -ItemType Directory -Force | Out-Null
    Write-Host "Created: $dir"
}

# Создание файлов с различным содержимым
Write-Host "`nCreating files..." -ForegroundColor Yellow

$files = @{
    "$basePath\Source\Code\main.ps1" = '# PowerShell script' + "`n" + 'Write-Host "Hello World"'
    "$basePath\Source\Config\settings.json" = '{ "app": "test", "version": "1.0" }'
    "$basePath\Docs\Technical\spec.txt" = 'Technical specifications document'
    "$basePath\Docs\User\manual.txt" = 'User manual content'
    "$basePath\readme.txt" = 'Project documentation'
}

foreach ($file in $files.GetEnumerator()) {
    Set-Content -Path $file.Key -Value $file.Value
    Write-Host "Created: $($file.Key)"
}

# Изменение атрибутов файлов
Write-Host "`nSetting file attributes..." -ForegroundColor Yellow

Set-ItemProperty -Path "$basePath\Source\Config\settings.json" -Name IsReadOnly -Value $true
Set-ItemProperty -Path "$basePath\Backup" -Name Attributes -Value "Hidden"

Write-Host "Made settings.json read-only"
Write-Host "Made Backup directory hidden"

# Поиск файлов по шаблону
Write-Host "`nSearching for text files..." -ForegroundColor Yellow
Get-ChildItem -Path $basePath -Recurse -Filter "*.txt" | 
    Select-Object FullName, Length, LastWriteTime | Format-Table -AutoSize

# Экспорт структуры в XML
Write-Host "`nExporting structure to XML..." -ForegroundColor Yellow
$structure = Get-ChildItem -Path $basePath -Recurse
$xmlFile = "project_structure.xml"
$structure | Export-Clixml -Path $xmlFile
Write-Host "Exported to: $xmlFile" -ForegroundColor Cyan

Write-Host "`nFile system operations completed!" -ForegroundColor Green

# Очистка (раскомментировать для удаления)
# Write-Host "`nCleaning up..." -ForegroundColor Yellow
# Remove-Item -Path $basePath -Recurse -Force
