# Трансформатор данных
Write-Host "=== Data Transformer ===" -ForegroundColor Green

class DataTransformer {
    [string]$WorkDirectory
    
    DataTransformer([string]$workDir) {
        $this.WorkDirectory = $workDir
        if (-not (Test-Path $workDir)) {
            New-Item -Path $workDir -ItemType Directory -Force | Out-Null
        }
    }
    
    [void]CSVToJSON([string]$csvPath, [string]$jsonPath) {
        Write-Host "Converting CSV to JSON: $csvPath -> $jsonPath" -ForegroundColor Yellow
        
        if (-not (Test-Path $csvPath)) {
            throw "CSV file not found: $csvPath"
        }
        
        try {
            $csvData = Import-Csv -Path $csvPath
            $jsonData = $csvData | ConvertTo-Json -Depth 10
            
            $jsonData | Set-Content -Path $jsonPath -Encoding UTF8
            Write-Host "✓ CSV to JSON conversion completed" -ForegroundColor Green
        }
        catch {
            throw "CSV to JSON conversion failed: $($_.Exception.Message)"
        }
    }
    
    [void]CSVToXML([string]$csvPath, [string]$xmlPath) {
        Write-Host "Converting CSV to XML: $csvPath -> $xmlPath" -ForegroundColor Yellow
        
        if (-not (Test-Path $csvPath)) {
            throw "CSV file not found: $csvPath"
        }
        
        try {
            $csvData = Import-Csv -Path $csvPath
            $xmlWriter = New-Object System.Xml.XmlTextWriter($xmlPath, $null)
            $xmlWriter.Formatting = [System.Xml.Formatting]::Indented
            
            $xmlWriter.WriteStartDocument()
            $xmlWriter.WriteStartElement("Root")
            
            foreach ($row in $csvData) {
                $xmlWriter.WriteStartElement("Record")
                
                foreach ($property in $row.PSObject.Properties) {
                    $xmlWriter.WriteElementString($property.Name, $property.Value)
                }
                
                $xmlWriter.WriteEndElement() # Record
            }
            
            $xmlWriter.WriteEndElement() # Root
            $xmlWriter.WriteEndDocument()
            $xmlWriter.Close()
            
            Write-Host "✓ CSV to XML conversion completed" -ForegroundColor Green
        }
        catch {
            throw "CSV to XML conversion failed: $($_.Exception.Message)"
        }
    }
    
    [void]JSONToCSV([string]$jsonPath, [string]$csvPath) {
        Write-Host "Converting JSON to CSV: $jsonPath -> $csvPath" -ForegroundColor Yellow
        
        if (-not (Test-Path $jsonPath)) {
            throw "JSON file not found: $jsonPath"
        }
        
        try {
            $jsonContent = Get-Content -Path $jsonPath -Raw
            $jsonData = $jsonContent | ConvertFrom-Json
            
            # Если JSON - массив объектов
            if ($jsonData -is [array]) {
                $jsonData | Export-Csv -Path $csvPath -NoTypeInformation -Encoding UTF8
            } else {
                # Если JSON - одиночный объект
                @($jsonData) | Export-Csv -Path $csvPath -NoTypeInformation -Encoding UTF8
            }
            
            Write-Host "✓ JSON to CSV conversion completed" -ForegroundColor Green
        }
        catch {
            throw "JSON to CSV conversion failed: $($_.Exception.Message)"
        }
    }
    
    [void]ParseUnstructuredLogs([string]$logPath, [string]$outputPath) {
        Write-Host "Parsing unstructured logs: $logPath" -ForegroundColor Yellow
        
        if (-not (Test-Path $logPath)) {
            throw "Log file not found: $logPath"
        }
        
        $logContent = Get-Content -Path $logPath
        $parsedData = @()
        
        # Шаблоны для парсинга распространенных форматов логов
        $logPatterns = @(
            @{
                Name = "IISLog"
                Pattern = '^(\S+)\s+(\S+)\s+(\S+)\s+(\S+)\s+(\S+)\s+(\S+)\s+(\S+)\s+(\S+)\s+(\S+)\s+(".*?")\s+(\S+)\s+(\S+)\s+(\S+)\s+(\S+)'
                Fields = @("Date", "Time", "ServerIP", "Method", "URI", "Query", "Port", "Username", "ClientIP", "UserAgent", "Status", "SubStatus", "Win32Status", "TimeTaken")
            },
            @{
                Name = "ApacheLog"
                Pattern = '^(\S+)\s+(\S+)\s+(\S+)\s+\[([^\]]+)\]\s+"(\S+)\s+(\S+)\s+([^"]*)"\s+(\S+)\s+(\S+)'
                Fields = @("IP", "Identity", "User", "Timestamp", "Method", "URL", "Protocol", "Status", "Size")
            },
            @{
                Name = "WindowsEvent"
                Pattern = '^(\d+-\d+-\d+\s+\d+:\d+:\d+)\s+(\S+)\s+(\S+)\s+(.+)'
                Fields = @("Timestamp", "Level", "Source", "Message")
            }
        )
        
        $lineNumber = 1
        foreach ($line in $logContent) {
            if ([string]::IsNullOrWhiteSpace($line)) {
                $lineNumber++
                continue
            }
            
            $parsedLine = $null
            
            foreach ($pattern in $logPatterns) {
                if ($line -match $pattern.Pattern) {
                    $parsedObject = @{ LineNumber = $lineNumber }
                    
                    for ($i = 0; $i -lt $pattern.Fields.Count; $i++) {
                        $fieldName = $pattern.Fields[$i]
                        $fieldValue = $matches[$i + 1]
                        $parsedObject[$fieldName] = $fieldValue
                    }
                    
                    $parsedObject["LogType"] = $pattern.Name
                    $parsedLine = [PSCustomObject]$parsedObject
                    break
                }
            }
            
            if (-not $parsedLine) {
                # Если ни один шаблон не подошел, сохраняем как нераспознанную строку
                $parsedLine = [PSCustomObject]@{
                    LineNumber = $lineNumber
                    LogType = "Unrecognized"
                    RawContent = $line
                }
            }
            
            $parsedData += $parsedLine
            $lineNumber++
        }
        
        $parsedData | Export-Csv -Path $outputPath -NoTypeInformation -Encoding UTF8
        Write-Host "✓ Log parsing completed. Parsed $($parsedData.Count) lines" -ForegroundColor Green
    }
    
    [void]NormalizeData([string]$inputPath, [string]$outputPath, [hashtable]$normalizationRules) {
        Write-Host "Normalizing data: $inputPath" -ForegroundColor Yellow
        
        if (-not (Test-Path $inputPath)) {
            throw "Input file not found: $inputPath"
        }
        
        $fileExtension = [System.IO.Path]::GetExtension($inputPath).ToLower()
        $data = @()
        
        # Загрузка данных в зависимости от формата
        switch ($fileExtension) {
            ".csv" {
                $data = Import-Csv -Path $inputPath
            }
            ".json" {
                $jsonContent = Get-Content -Path $inputPath -Raw
                $data = $jsonContent | ConvertFrom-Json
                if ($data -isnot [array]) {
                    $data = @($data)
                }
            }
            default {
                throw "Unsupported file format for normalization: $fileExtension"
            }
        }
        
        $normalizedData = @()
        
        foreach ($item in $data) {
            $normalizedItem = @{}
            
            foreach ($property in $item.PSObject.Properties) {
                $propertyName = $property.Name
                $propertyValue = $property.Value
                
                # Применение правил нормализации
                if ($normalizationRules.ContainsKey($propertyName)) {
                    $rule = $normalizationRules[$propertyName]
                    
                    switch ($rule.Action) {
                        "Trim" {
                            $propertyValue = $propertyValue.ToString().Trim()
                        }
                        "LowerCase" {
                            $propertyValue = $propertyValue.ToString().ToLower()
                        }
                        "UpperCase" {
                            $propertyValue = $propertyValue.ToString().ToUpper()
                        }
                        "RemoveSpecialChars" {
                            $propertyValue = $propertyValue -replace '[^\w\s]', ''
                        }
                        "DateFormat" {
                            try {
                                $date = [datetime]::Parse($propertyValue)
                                $propertyValue = $date.ToString($rule.Format)
                            }
                            catch {
                                Write-Host "Warning: Could not parse date '$propertyValue'" -ForegroundColor Yellow
                            }
                        }
                        "PhoneFormat" {
                            $digits = $propertyValue -replace '\D', ''
                            if ($digits.Length -eq 10) {
                                $propertyValue = "($($digits.Substring(0,3))) $($digits.Substring(3,3))-$($digits.Substring(6,4))"
                            }
                        }
                    }
                }
                
                $normalizedItem[$propertyName] = $propertyValue
            }
            
            $normalizedData += [PSCustomObject]$normalizedItem
        }
        
        $normalizedData | Export-Csv -Path $outputPath -NoTypeInformation -Encoding UTF8
        Write-Host "✓ Data normalization completed" -ForegroundColor Green
    }
    
    [void]ValidateAndCleanData([string]$inputPath, [string]$outputPath, [hashtable]$validationRules) {
        Write-Host "Validating and cleaning data: $inputPath" -ForegroundColor Yellow
        
        if (-not (Test-Path $inputPath)) {
            throw "Input file not found: $inputPath"
        }
        
        $fileExtension = [System.IO.Path]::GetExtension($inputPath).ToLower()
        $data = @()
        
        switch ($fileExtension) {
            ".csv" {
                $data = Import-Csv -Path $inputPath
            }
            ".json" {
                $jsonContent = Get-Content -Path $inputPath -Raw
                $data = $jsonContent | ConvertFrom-Json
                if ($data -isnot [array]) {
                    $data = @($data)
                }
            }
            default {
                throw "Unsupported file format for validation: $fileExtension"
            }
        }
        
        $cleanedData = @()
        $validationErrors = @()
        
        $recordNumber = 1
        foreach ($item in $data) {
            $isValid = $true
            $recordErrors = @()
            
            foreach ($property in $item.PSObject.Properties) {
                $propertyName = $property.Name
                $propertyValue = $property.Value
                
                if ($validationRules.ContainsKey($propertyName)) {
                    $rule = $validationRules[$propertyName]
                    
                    # Проверка обязательных полей
                    if ($rule.Required -and [string]::IsNullOrWhiteSpace($propertyValue)) {
                        $isValid = $false
                        $recordErrors += "$propertyName is required"
                    }
                    
                    # Проверка минимальной длины
                    if ($rule.MinLength -and $propertyValue.ToString().Length -lt $rule.MinLength) {
                        $isValid = $false
                        $recordErrors += "$propertyName is too short (min: $($rule.MinLength))"
                    }
                    
                    # Проверка максимальной длины
                    if ($rule.MaxLength -and $propertyValue.ToString().Length -gt $rule.MaxLength) {
                        $isValid = $false
                        $recordErrors += "$propertyName is too long (max: $($rule.MaxLength))"
                    }
                    
                    # Проверка по шаблону
                    if ($rule.Pattern -and $propertyValue -notmatch $rule.Pattern) {
                        $isValid = $false
                        $recordErrors += "$propertyName does not match required pattern"
                    }
                    
                    # Проверка типа данных
                    if ($rule.Type) {
                        switch ($rule.Type) {
                            "Email" {
                                if ($propertyValue -notmatch '^[^@]+@[^@]+\.[^@]+$') {
                                    $isValid = $false
                                    $recordErrors += "$propertyName is not a valid email"
                                }
                            }
                            "Phone" {
                                $digits = $propertyValue -replace '\D', ''
                                if ($digits.Length -lt 10) {
                                    $isValid = $false
                                    $recordErrors += "$propertyName is not a valid phone number"
                                }
                            }
                            "Number" {
                                if (-not [double]::TryParse($propertyValue, [ref]$null)) {
                                    $isValid = $false
                                    $recordErrors += "$propertyName is not a valid number"
                                }
                            }
                        }
                    }
                }
            }
            
            if ($isValid) {
                $cleanedData += $item
            } else {
                $validationErrors += @{
                    RecordNumber = $recordNumber
                    Errors = $recordErrors
                    Data = $item
                }
            }
            
            $recordNumber++
        }
        
        # Сохранение очищенных данных
        $cleanedData | Export-Csv -Path $outputPath -NoTypeInformation -Encoding UTF8
        
        # Сохранение ошибок валидации
        if ($validationErrors.Count -gt 0) {
            $errorsPath = [System.IO.Path]::ChangeExtension($outputPath, "_validation_errors.json")
            $validationErrors | ConvertTo-Json -Depth 3 | Set-Content -Path $errorsPath -Encoding UTF8
            Write-Host "⚠ Validation completed with $($validationErrors.Count) errors" -ForegroundColor Yellow
            Write-Host "  Validation errors saved to: $errorsPath" -ForegroundColor Gray
        } else {
            Write-Host "✓ Data validation completed. No errors found." -ForegroundColor Green
        }
    }
    
    [void]GenerateDataSchema([string]$inputPath, [string]$schemaPath) {
        Write-Host "Generating data schema: $inputPath" -ForegroundColor Yellow
        
        if (-not (Test-Path $inputPath)) {
            throw "Input file not found: $inputPath"
        }
        
        $fileExtension = [System.IO.Path]::GetExtension($inputPath).ToLower()
        $data = @()
        
        switch ($fileExtension) {
            ".csv" {
                $data = Import-Csv -Path $inputPath
            }
            ".json" {
                $jsonContent = Get-Content -Path $inputPath -Raw
                $data = $jsonContent | ConvertFrom-Json
                if ($data -isnot [array]) {
                    $data = @($data)
                }
            }
            default {
                throw "Unsupported file format for schema generation: $fileExtension"
            }
        }
        
        if ($data.Count -eq 0) {
            Write-Host "No data found to generate schema" -ForegroundColor Yellow
            return
        }
        
        $schema = @{
            SourceFile = $inputPath
            Generated = Get-Date
            RecordCount = $data.Count
            Fields = @()
        }
        
        # Анализ первого объекта для определения полей
        $sampleObject = $data[0]
        
        foreach ($property in $sampleObject.PSObject.Properties) {
            $fieldName = $property.Name
            $sampleValue = $property.Value
            
            $fieldInfo = @{
                Name = $fieldName
                Type = $this.DetectDataType($sampleValue)
                SampleValues = @($sampleValue)
                UniqueValues = @()
                Stats = @{}
            }
            
            # Сбор статистики по полю
            $allValues = $data | Select-Object -ExpandProperty $fieldName
            $fieldInfo.UniqueValues = $allValues | Sort-Object | Get-Unique
            
            # Базовая статистика
            if ($fieldInfo.Type -eq "Number") {
                $numbers = $allValues | ForEach-Object { 
                    if ([double]::TryParse($_, [ref]$null)) { [double]$_ } 
                } | Where-Object { $_ -ne $null }
                
                if ($numbers.Count -gt 0) {
                    $fieldInfo.Stats = @{
                        Min = ($numbers | Measure-Object -Minimum).Minimum
                        Max = ($numbers | Measure-Object -Maximum).Maximum
                        Average = ($numbers | Measure-Object -Average).Average
                        Count = $numbers.Count
                    }
                }
            } else {
                $fieldInfo.Stats = @{
                    UniqueCount = $fieldInfo.UniqueValues.Count
                    MostFrequent = $allValues | Group-Object | Sort-Object Count -Descending | Select-Object -First 1 -ExpandProperty Name
                }
            }
            
            $schema.Fields += $fieldInfo
        }
        
        $schema | ConvertTo-Json -Depth 5 | Set-Content -Path $schemaPath -Encoding UTF8
        Write-Host "✓ Data schema generated: $schemaPath" -ForegroundColor Green
    }
    
    [string]DetectDataType([object]$value) {
        if ($value -eq $null) { return "Null" }
        
        $stringValue = $value.ToString()
        
        if ([int]::TryParse($stringValue, [ref]$null)) { return "Integer" }
        if ([double]::TryParse($stringValue, [ref]$null)) { return "Number" }
        if ([datetime]::TryParse($stringValue, [ref]$null)) { return "DateTime" }
        if ($stringValue -match '^[^@]+@[^@]+\.[^@]+$') { return "Email" }
        if ($stringValue -match '^\d{3}-\d{3}-\d{4}$' -or $stringValue -match '^\(\d{3}\) \d{3}-\d{4}$') { return "Phone" }
        
        return "String"
    }
}

# Демонстрация работы трансформатора
function Show-Demo {
    $transformer = [DataTransformer]::new("C:\DataTransformer")
    
    # Создание тестовых данных
    $testData = @(
        @{ Name = "John Doe"; Email = "john@example.com"; Phone = "123-456-7890"; Age = "30"; Salary = "50000" },
        @{ Name = "Jane Smith"; Email = "jane@example.com"; Phone = "987-654-3210"; Age = "25"; Salary = "60000" },
        @{ Name = " Bob Johnson "; Email = "bob@example.com"; Phone = "555-123-4567"; Age = "35"; Salary = "70000" }
    )
    
    $testCsv = "C:\DataTransformer\test_data.csv"
    $testData | Export-Csv -Path $testCsv -NoTypeInformation
    
    Write-Host "Demo data created: $testCsv" -ForegroundColor Green
    
    # Демонстрация преобразований
    $transformer.CSVToJSON($testCsv, "C:\DataTransformer\test_data.json")
    $transformer.CSVToXML($testCsv, "C:\DataTransformer\test_data.xml")
    
    # Демонстрация нормализации
    $normalizationRules = @{
        Name = @{ Action = "Trim" }
        Email = @{ Action = "LowerCase" }
        Phone = @{ Action = "PhoneFormat" }
    }
    
    $transformer.NormalizeData($testCsv, "C:\DataTransformer\normalized_data.csv", $normalizationRules)
    
    # Демонстрация валидации
    $validationRules = @{
        Name = @{ Required = $true; MinLength = 2; MaxLength = 50 }
        Email = @{ Required = $true; Type = "Email" }
        Phone = @{ Required = $true; Type = "Phone" }
        Age = @{ Type = "Number" }
    }
    
    $transformer.ValidateAndCleanData($testCsv, "C:\DataTransformer\cleaned_data.csv", $validationRules)
    
    # Генерация схемы
    $transformer.GenerateDataSchema($testCsv, "C:\DataTransformer\data_schema.json")
    
    Write-Host "`nDemo completed! Check generated files in C:\DataTransformer" -ForegroundColor Green
}

# Основное меню
function Show-MainMenu {
    Write-Host "`n=== Data Transformer ===" -ForegroundColor Green
    Write-Host "1. CSV to JSON"
    Write-Host "2. CSV to XML"
    Write-Host "3. JSON to CSV"
    Write-Host "4. Parse Unstructured Logs"
    Write-Host "5. Normalize Data"
    Write-Host "6. Validate and Clean Data"
    Write-Host "7. Generate Data Schema"
    Write-Host "8. Run Demo"
    Write-Host "9. Exit"
    
    return Read-Host "`nSelect option (1-9)"
}

# Инициализация трансформатора
$transformer = [DataTransformer]::new("C:\DataTransformer")

Write-Host "Data Transformer initialized" -ForegroundColor Green
Write-Host "Work directory: C:\DataTransformer" -ForegroundColor Cyan

do {
    $choice = Show-MainMenu
    
    try {
        switch ($choice) {
            "1" {
                $csvPath = Read-Host "Enter CSV file path"
                $jsonPath = Read-Host "Enter output JSON file path"
                $transformer.CSVToJSON($csvPath, $jsonPath)
            }
            "2" {
                $csvPath = Read-Host "Enter CSV file path"
                $xmlPath = Read-Host "Enter output XML file path"
                $transformer.CSVToXML($csvPath, $xmlPath)
            }
            "3" {
                $jsonPath = Read-Host "Enter JSON file path"
                $csvPath = Read-Host "Enter output CSV file path"
                $transformer.JSONToCSV($jsonPath, $csvPath)
            }
            "4" {
                $logPath = Read-Host "Enter log file path"
                $outputPath = Read-Host "Enter output CSV file path"
                $transformer.ParseUnstructuredLogs($logPath, $outputPath)
            }
            "5" {
                $inputPath = Read-Host "Enter input file path"
                $outputPath = Read-Host "Enter output file path"
                
                Write-Host "Define normalization rules (field=action):" -ForegroundColor Yellow
                Write-Host "Available actions: Trim, LowerCase, UpperCase, RemoveSpecialChars, DateFormat, PhoneFormat" -ForegroundColor Gray
                
                $rules = @{}
                do {
                    $ruleInput = Read-Host "Enter rule (or 'done' to finish)"
                    if ($ruleInput -ne 'done' -and $ruleInput -match "(.+)=(.+)") {
                        $field = $matches[1].Trim()
                        $action = $matches[2].Trim()
                        $rules[$field] = @{ Action = $action }
                        
                        if ($action -eq "DateFormat") {
                            $format = Read-Host "Enter date format (e.g., yyyy-MM-dd)"
                            $rules[$field].Format = $format
                        }
                    }
                } while ($ruleInput -ne 'done')
                
                $transformer.NormalizeData($inputPath, $outputPath, $rules)
            }
            "6" {
                $inputPath = Read-Host "Enter input file path"
                $outputPath = Read-Host "Enter output file path"
                
                Write-Host "Define validation rules:" -ForegroundColor Yellow
                $rules = @{}
                do {
                    $field = Read-Host "Enter field name (or 'done' to finish)"
                    if ($field -ne 'done') {
                        $fieldRules = @{}
                        
                        $required = Read-Host "Required? (y/n)"
                        if ($required -eq 'y') { $fieldRules.Required = $true }
                        
                        $minLength = Read-Host "Minimum length (or Enter to skip)"
                        if ($minLength) { $fieldRules.MinLength = [int]$minLength }
                        
                        $maxLength = Read-Host "Maximum length (or Enter to skip)"
                        if ($maxLength) { $fieldRules.MaxLength = [int]$maxLength }
                        
                        $type = Read-Host "Data type (Email/Phone/Number or Enter to skip)"
                        if ($type) { $fieldRules.Type = $type }
                        
                        $pattern = Read-Host "Regex pattern (or Enter to skip)"
                        if ($pattern) { $fieldRules.Pattern = $pattern }
                        
                        $rules[$field] = $fieldRules
                    }
                } while ($field -ne 'done')
                
                $transformer.ValidateAndCleanData($inputPath, $outputPath, $rules)
            }
            "7" {
                $inputPath = Read-Host "Enter input file path"
                $schemaPath = Read-Host "Enter output schema file path"
                $transformer.GenerateDataSchema($inputPath, $schemaPath)
            }
            "8" {
                Show-Demo
            }
            "9" {
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
    
    if ($choice -ne "9") {
        Read-Host "`nPress Enter to continue..."
    }
} while ($choice -ne "9")
