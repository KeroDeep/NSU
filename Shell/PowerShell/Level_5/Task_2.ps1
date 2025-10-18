# Рекурсивный обработчик XML/JSON
Write-Host "=== Recursive XML/JSON Processor ===" -ForegroundColor Green

class DataProcessor {
    [string]$FilePath
    [string]$FileType
    [object]$Data
    
    DataProcessor([string]$path) {
        $this.FilePath = $path
        $this.FileType = [System.IO.Path]::GetExtension($path).ToLower()
        $this.LoadData()
    }
    
    [void]LoadData() {
        if (-not (Test-Path $this.FilePath)) {
            throw "File not found: $($this.FilePath)"
        }
        
        switch ($this.FileType) {
            ".xml" {
                $xmlContent = Get-Content $this.FilePath -Raw
                $this.Data = [xml]$xmlContent
            }
            ".json" {
                $jsonContent = Get-Content $this.FilePath -Raw
                $this.Data = $jsonContent | ConvertFrom-Json
            }
            default {
                throw "Unsupported file type: $($this.FileType)"
            }
        }
    }
    
    [object]FindByCriteria([scriptblock]$criteria) {
        $results = @()
        $this.TraverseData($this.Data, "", $criteria, [ref]$results)
        return $results
    }
    
    [void]TraverseData([object]$current, [string]$path, [scriptblock]$criteria, [ref]$results) {
        if ($current -eq $null) { return }
        
        # Проверка критериев для текущего элемента
        $match = Invoke-Command -ScriptBlock $criteria -ArgumentList $current, $path
        if ($match) {
            $results.Value += [PSCustomObject]@{
                Path = $path
                Value = $current
                Type = $current.GetType().Name
            }
        }
        
        # Рекурсивный обход для разных типов данных
        if ($current -is [System.Collections.IEnumerable] -and $current -isnot [string]) {
            $index = 0
            foreach ($item in $current) {
                $newPath = if ($path) { "$path[$index]" } else { "[$index]" }
                $this.TraverseData($item, $newPath, $criteria, $results)
                $index++
            }
        }
        elseif ($current -is [PSCustomObject]) {
            foreach ($property in $current.PSObject.Properties) {
                $newPath = if ($path) { "$path.$($property.Name)" } else { $property.Name }
                $this.TraverseData($property.Value, $newPath, $criteria, $results)
            }
        }
        elseif ($current -is [System.Xml.XmlNode]) {
            foreach ($child in $current.ChildNodes) {
                $newPath = if ($path) { "$path.$($child.Name)" } else { $child.Name }
                $this.TraverseData($child, $newPath, $criteria, $results)
            }
        }
    }
    
    [void]ModifyData([scriptblock]$modifier) {
        $this.ModifyRecursive($this.Data, "", $modifier)
        $this.SaveData()
    }
    
    [void]ModifyRecursive([object]$current, [string]$path, [scriptblock]$modifier) {
        if ($current -eq $null) { return }
        
        # Применение модификатора
        $newValue = Invoke-Command -ScriptBlock $modifier -ArgumentList $current, $path
        
        if ($newValue -ne $current) {
            # Здесь должна быть логика обновления родительской структуры
            # Для упрощения, мы модифицируем только листовые значения
            if ($current -isnot [System.Collections.IEnumerable] -or $current -is [string]) {
                $this.UpdateParent($path, $newValue)
            }
        }
        
        # Рекурсивный обход
        if ($current -is [System.Collections.IEnumerable] -and $current -isnot [string]) {
            $index = 0
            foreach ($item in $current) {
                $newPath = if ($path) { "$path[$index]" } else { "[$index]" }
                $this.ModifyRecursive($item, $newPath, $modifier)
                $index++
            }
        }
        elseif ($current -is [PSCustomObject]) {
            foreach ($property in $current.PSObject.Properties) {
                $newPath = if ($path) { "$path.$($property.Name)" } else { $property.Name }
                $this.ModifyRecursive($property.Value, $newPath, $modifier)
            }
        }
    }
    
    [void]UpdateParent([string]$path, [object]$newValue) {
        # Упрощенная реализация обновления - в реальном приложении нужна более сложная логика
        Write-Host "Would update $path to $newValue" -ForegroundColor Yellow
    }
    
    [bool]ValidateSchema([string]$schemaDefinition) {
        # Базовая валидация структуры
        $isValid = $true
        $validationErrors = @()
        
        $this.ValidateRecursive($this.Data, "", $schemaDefinition, [ref]$validationErrors, [ref]$isValid)
        
        if ($validationErrors.Count -gt 0) {
            Write-Host "Validation Errors:" -ForegroundColor Red
            foreach ($error in $validationErrors) {
                Write-Host "  $error" -ForegroundColor Red
            }
        }
        
        return $isValid
    }
    
    [void]ValidateRecursive([object]$current, [string]$path, [string]$schema, [ref]$errors, [ref]$isValid) {
        # Упрощенная валидация - проверка на null/empty
        if ($current -eq $null -or $current -eq "") {
            $errors.Value += "Empty value at path: $path"
            $isValid.Value = $false
        }
        
        # Рекурсивная валидация для сложных структур
        if ($current -is [System.Collections.IEnumerable] -and $current -isnot [string]) {
            $index = 0
            foreach ($item in $current) {
                $newPath = if ($path) { "$path[$index]" } else { "[$index]" }
                $this.ValidateRecursive($item, $newPath, $schema, $errors, $isValid)
                $index++
            }
        }
        elseif ($current -is [PSCustomObject]) {
            foreach ($property in $current.PSObject.Properties) {
                $newPath = if ($path) { "$path.$($property.Name)" } else { $property.Name }
                $this.ValidateRecursive($property.Value, $newPath, $schema, $errors, $isValid)
            }
        }
    }
    
    [void]SaveData() {
        $outputPath = $this.FilePath -replace "\.(xml|json)$", "_modified.$($this.FileType.TrimStart('.'))"
        
        switch ($this.FileType) {
            ".xml" {
                $this.Data.Save($outputPath)
            }
            ".json" {
                $this.Data | ConvertTo-Json -Depth 10 | Set-Content $outputPath
            }
        }
        
        Write-Host "Modified data saved to: $outputPath" -ForegroundColor Green
    }
    
    [void]GenerateReport([string]$reportPath) {
        $reportData = @{
            FilePath = $this.FilePath
            FileType = $this.FileType
            ProcessedDate = Get-Date
            TotalElements = 0
            Structure = @()
        }
        
        $this.AnalyzeStructure($this.Data, "", [ref]$reportData)
        
        $reportData | ConvertTo-Json -Depth 5 | Set-Content $reportPath
        Write-Host "Analysis report saved to: $reportPath" -ForegroundColor Cyan
    }
    
    [void]AnalyzeStructure([object]$current, [string]$path, [ref]$report) {
        $report.Value.TotalElements++
        
        $elementInfo = @{
            Path = if ($path) { $path } else { "root" }
            Type = $current.GetType().Name
            Value = if ($current -is [string] -or $current -is [int] -or $current -is [bool]) { $current } else { "complex" }
        }
        
        $report.Value.Structure += $elementInfo
        
        # Рекурсивный анализ
        if ($current -is [System.Collections.IEnumerable] -and $current -isnot [string]) {
            $index = 0
            foreach ($item in $current) {
                $newPath = if ($path) { "$path[$index]" } else { "[$index]" }
                $this.AnalyzeStructure($item, $newPath, $report)
                $index++
            }
        }
        elseif ($current -is [PSCustomObject]) {
            foreach ($property in $current.PSObject.Properties) {
                $newPath = if ($path) { "$path.$($property.Name)" } else { $property.Name }
                $this.AnalyzeStructure($property.Value, $newPath, $report)
            }
        }
    }
}

# Демонстрация работы процессора
function Show-Demo {
    # Создание тестовых данных
    $testData = @{
        users = @(
            @{ name = "John Doe"; age = 30; active = $true },
            @{ name = "Jane Smith"; age = 25; active = $false },
            @{ name = "Bob Johnson"; age = 35; active = $true }
        )
        metadata = @{
            version = "1.0"
            created = (Get-Date).ToString("yyyy-MM-dd")
        }
    }
    
    $jsonFile = "test_data.json"
    $testData | ConvertTo-Json -Depth 3 | Set-Content $jsonFile
    
    Write-Host "Created test file: $jsonFile" -ForegroundColor Green
    
    # Обработка данных
    $processor = [DataProcessor]::new($jsonFile)
    
    Write-Host "`n=== Search Demo ===" -ForegroundColor Yellow
    $results = $processor.FindByCriteria({
        param($item, $path)
        return $item -eq $true -or $item -eq "John Doe"
    })
    
    $results | Format-Table -AutoSize
    
    Write-Host "`n=== Structure Analysis ===" -ForegroundColor Yellow
    $reportFile = "structure_report.json"
    $processor.GenerateReport($reportFile)
    
    Write-Host "`n=== Validation Demo ===" -ForegroundColor Yellow
    $isValid = $processor.ValidateSchema("basic")
    Write-Host "Data validation: $(if ($isValid) { 'PASS' } else { 'FAIL' })" -ForegroundColor $(if ($isValid) { 'Green' } else { 'Red' })
}

# Основное меню
function Show-MainMenu {
    Write-Host "`n=== XML/JSON Processor ===" -ForegroundColor Green
    Write-Host "1. Process existing file"
    Write-Host "2. Create and process demo data"
    Write-Host "3. Exit"
    
    return Read-Host "`nSelect option (1-3)"
}

do {
    $choice = Show-MainMenu
    
    switch ($choice) {
        "1" {
            $filePath = Read-Host "Enter file path"
            if (Test-Path $filePath) {
                try {
                    $processor = [DataProcessor]::new($filePath)
                    Write-Host "File loaded successfully: $filePath" -ForegroundColor Green
                    
                    # Дополнительные операции с файлом...
                    $searchTerm = Read-Host "Enter search term (or press Enter to skip)"
                    if ($searchTerm) {
                        $results = $processor.FindByCriteria({
                            param($item, $path)
                            return $item -like "*$searchTerm*"
                        })
                        $results | Format-Table -AutoSize
                    }
                }
                catch {
                    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
                }
            } else {
                Write-Host "File not found: $filePath" -ForegroundColor Red
            }
        }
        "2" {
            Show-Demo
        }
        "3" {
            Write-Host "Goodbye!" -ForegroundColor Green
        }
        default {
            Write-Host "Invalid option" -ForegroundColor Red
        }
    }
    
    if ($choice -ne "3") {
        Read-Host "`nPress Enter to continue..."
    }
} while ($choice -ne "3")

# Очистка тестовых файлов
if (Test-Path "test_data.json") { Remove-Item "test_data.json" }
if (Test-Path "structure_report.json") { Remove-Item "structure_report.json" }
