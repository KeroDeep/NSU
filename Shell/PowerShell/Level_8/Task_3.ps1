# REST API микросервис на PowerShell
Write-Host "=== PowerShell REST API Microservice ===" -ForegroundColor Green

class RestApiService {
    [string]$Host
    [int]$Port
    [hashtable]$Routes
    [System.Collections.ArrayList]$Middlewares
    [hashtable]$Config
    [System.Collections.ArrayList]$Logs
    [string]$OpenApiPath

    RestApiService() {
        $this.Host = "localhost"
        $this.Port = 8080
        $this.Routes = @{}
        $this.Middlewares = [System.Collections.ArrayList]::new()
        $this.Logs = [System.Collections.ArrayList]::new()
        $this.OpenApiPath = "openapi_$(Get-Date -Format 'yyyyMMdd_HHmmss').json"
        $this.Config = @{
            AuthEnabled = $true
            CorsEnabled = $true
            RateLimit = 100
            LogLevel = "INFO"
        }
    }

    # Метод для добавления middleware
    [void]AddMiddleware([scriptblock]$middleware) {
        $this.Middlewares.Add($middleware) | Out-Null
        $this.Log("Middleware added", "INFO")
    }

    # Метод для логирования
    [void]Log([string]$message, [string]$level) {
        $logEntry = @{
            Timestamp = Get-Date
            Level = $level
            Message = $message
        }
        $this.Logs.Add($logEntry) | Out-Null
        
        $color = switch($level) {
            "ERROR" { "Red" }
            "WARN" { "Yellow" } 
            "INFO" { "Green" }
            "DEBUG" { "Gray" }
            default { "White" }
        }
        
        Write-Host "[$level] $message" -ForegroundColor $color
    }

    # Метод для добавления маршрутов
    [void]AddRoute([string]$method, [string]$path, [scriptblock]$handler) {
        if (-not $this.Routes.ContainsKey($method)) {
            $this.Routes[$method] = @{}
        }
        $this.Routes[$method][$path] = $handler
        $this.Log("Route added: $method $path", "INFO")
    }

    # CRUD операции для пользователей
    [void]SetupUserRoutes() {
        $this.Log("Setting up user routes...", "DEBUG")
        
        # GET /users - получить всех пользователей
        $this.AddRoute("GET", "/users", {
            param($context)
            return @{
                StatusCode = 200
                Content = $context.DataStore.Users | ConvertTo-Json -Depth 3
                ContentType = "application/json"
            }
        })

        # GET /users/{id} - получить пользователя по ID
        $this.AddRoute("GET", "/users/{id}", {
            param($context)
            $id = [int]$context.Parameters.id
            $user = $context.DataStore.Users | Where-Object { $_.Id -eq $id }
            
            if ($user) {
                return @{
                    StatusCode = 200
                    Content = $user | ConvertTo-Json -Depth 3
                    ContentType = "application/json"
                }
            } else {
                return @{
                    StatusCode = 404
                    Content = (@{Error = "User not found"; Id = $id} | ConvertTo-Json)
                    ContentType = "application/json"
                }
            }
        })

        # POST /users - создать нового пользователя
        $this.AddRoute("POST", "/users", {
            param($context)
            try {
                $body = $context.RequestBody | ConvertFrom-Json
                $newId = if ($context.DataStore.Users.Count -gt 0) { 
                    ($context.DataStore.Users | Measure-Object -Property Id -Maximum).Maximum + 1 
                } else { 1 }
                
                $newUser = @{
                    Id = $newId
                    Name = $body.Name
                    Email = $body.Email
                    Role = $body.Role
                    CreatedAt = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss")
                }
                
                $context.DataStore.Users.Add($newUser) | Out-Null
                $context.Service.SaveDataStore()
                
                return @{
                    StatusCode = 201
                    Content = $newUser | ConvertTo-Json -Depth 3
                    ContentType = "application/json"
                }
            }
            catch {
                return @{
                    StatusCode = 400
                    Content = (@{Error = "Invalid JSON"; Message = $_.Exception.Message} | ConvertTo-Json)
                    ContentType = "application/json"
                }
            }
        })

        # PUT /users/{id} - обновить пользователя
        $this.AddRoute("PUT", "/users/{id}", {
            param($context)
            $id = [int]$context.Parameters.id
            $user = $context.DataStore.Users | Where-Object { $_.Id -eq $id }
            
            if (-not $user) {
                return @{
                    StatusCode = 404
                    Content = (@{Error = "User not found"; Id = $id} | ConvertTo-Json)
                    ContentType = "application/json"
                }
            }
            
            try {
                $body = $context.RequestBody | ConvertFrom-Json
                if ($body.Name) { $user.Name = $body.Name }
                if ($body.Email) { $user.Email = $body.Email }
                if ($body.Role) { $user.Role = $body.Role }
                
                $context.Service.SaveDataStore()
                
                return @{
                    StatusCode = 200
                    Content = $user | ConvertTo-Json -Depth 3
                    ContentType = "application/json"
                }
            }
            catch {
                return @{
                    StatusCode = 400
                    Content = (@{Error = "Invalid JSON"} | ConvertTo-Json)
                    ContentType = "application/json"
                }
            }
        })

        # DELETE /users/{id} - удалить пользователя
        $this.AddRoute("DELETE", "/users/{id}", {
            param($context)
            $id = [int]$context.Parameters.id
            $user = $context.DataStore.Users | Where-Object { $_.Id -eq $id }
            
            if (-not $user) {
                return @{
                    StatusCode = 404
                    Content = (@{Error = "User not found"; Id = $id} | ConvertTo-Json)
                    ContentType = "application/json"
                }
            }
            
            $context.DataStore.Users = $context.DataStore.Users | Where-Object { $_.Id -ne $id }
            $context.Service.SaveDataStore()
            
            return @{
                StatusCode = 200
                Content = (@{Message = "User deleted successfully"; Id = $id} | ConvertTo-Json)
                ContentType = "application/json"
            }
        })
    }

    # CRUD операции для продуктов
    [void]SetupProductRoutes() {
        $this.Log("Setting up product routes...", "DEBUG")
        
        # GET /products - получить все продукты
        $this.AddRoute("GET", "/products", {
            param($context)
            return @{
                StatusCode = 200
                Content = $context.DataStore.Products | ConvertTo-Json -Depth 3
                ContentType = "application/json"
            }
        })

        # POST /products - создать новый продукт
        $this.AddRoute("POST", "/products", {
            param($context)
            try {
                $body = $context.RequestBody | ConvertFrom-Json
                $newId = if ($context.DataStore.Products.Count -gt 0) { 
                    ($context.DataStore.Products | Measure-Object -Property Id -Maximum).Maximum + 1 
                } else { 1 }
                
                $newProduct = @{
                    Id = $newId
                    Name = $body.Name
                    Price = $body.Price
                    Category = $body.Category
                    Stock = $body.Stock
                    CreatedAt = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss")
                }
                
                $context.DataStore.Products.Add($newProduct) | Out-Null
                $context.Service.SaveDataStore()
                
                return @{
                    StatusCode = 201
                    Content = $newProduct | ConvertTo-Json -Depth 3
                    ContentType = "application/json"
                }
            }
            catch {
                return @{
                    StatusCode = 400
                    Content = (@{Error = "Invalid JSON"} | ConvertTo-Json)
                    ContentType = "application/json"
                }
            }
        })
    }

    # Системные маршруты
    [void]SetupSystemRoutes() {
        $this.Log("Setting up system routes...", "DEBUG")
        
        # Health check
        $this.AddRoute("GET", "/health", {
            param($context)
            return @{
                StatusCode = 200
                Content = (@{
                    Status = "Healthy"
                    Timestamp = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss")
                    Service = "PowerShell REST API"
                    Version = "1.0.0"
                } | ConvertTo-Json)
                ContentType = "application/json"
            }
        })

        # OpenAPI документация
        $this.AddRoute("GET", "/docs", {
            param($context)
            return $context.Service.GenerateOpenApiSpec()
        })

        # Информация о API
        $this.AddRoute("GET", "/", {
            param($context)
            return @{
                StatusCode = 200
                Content = (@{
                    Message = "PowerShell REST API Microservice"
                    Version = "1.0.0"
                    Documentation = "/docs"
                    HealthCheck = "/health"
                    Endpoints = @(
                        "GET /users", "GET /users/{id}", "POST /users", "PUT /users/{id}", "DELETE /users/{id}",
                        "GET /products", "POST /products", "GET /health", "GET /docs"
                    )
                } | ConvertTo-Json -Depth 3)
                ContentType = "application/json"
            }
        })
    }

    # Middleware для аутентификации
    [scriptblock]GetAuthMiddleware() {
        return {
            param($context)
            
            if (-not $context.Service.Config.AuthEnabled) {
                return $true
            }
            
            $authHeader = $context.Headers["Authorization"]
            if (-not $authHeader) {
                $context.Response = @{
                    StatusCode = 401
                    Content = (@{Error = "Unauthorized"; Message = "Authorization header required"} | ConvertTo-Json)
                    ContentType = "application/json"
                }
                return $false
            }
            
            # Простая проверка Bearer токена
            $token = $authHeader -replace "Bearer ", ""
            $validTokens = @{
                "admin-token-123" = @{Role = "Admin"}
                "user-token-456" = @{Role = "User"}
            }
            
            if (-not $validTokens.ContainsKey($token)) {
                $context.Response = @{
                    StatusCode = 401
                    Content = (@{Error = "Unauthorized"; Message = "Invalid token"} | ConvertTo-Json)
                    ContentType = "application/json"
                }
                return $false
            }
            
            $context.User = $validTokens[$token]
            return $true
        }
    }

    # Middleware для CORS
    [scriptblock]GetCorsMiddleware() {
        return {
            param($context)
            
            if ($context.Service.Config.CorsEnabled -and $context.Method -eq "OPTIONS") {
                $context.Response = @{
                    StatusCode = 200
                    Headers = @{
                        "Access-Control-Allow-Origin" = "*"
                        "Access-Control-Allow-Methods" = "GET, POST, PUT, DELETE, OPTIONS"
                        "Access-Control-Allow-Headers" = "Content-Type, Authorization"
                    }
                    Content = ""
                }
                return $false
            }
            return $true
        }
    }

    # Middleware для логирования
    [scriptblock]GetLoggingMiddleware() {
        return {
            param($context)
            
            $context.Service.Log("$($context.Method) $($context.Path) from $($context.RemoteEndPoint)", "INFO")
            return $true
        }
    }

    # Генерация OpenAPI спецификации
    [hashtable]GenerateOpenApiSpec() {
        $this.Log("Generating OpenAPI specification...", "DEBUG")
        
        $openapi = @{
            openapi = "3.0.0"
            info = @{
                title = "PowerShell REST API Microservice"
                description = "Full-featured REST API built with PowerShell"
                version = "1.0.0"
                contact = @{
                    name = "API Support"
                    email = "support@example.com"
                }
            }
            servers = @(
                @{
                    url = "http://$($this.Host):$($this.Port)"
                    description = "Development server"
                }
            )
            paths = @{
                "/users" = @{
                    get = @{
                        summary = "Get all users"
                        description = "Retrieve list of all users"
                        responses = @{
                            "200" = @{
                                description = "Successful operation"
                                content = @{
                                    "application/json" = @{
                                        schema = @{type = "array"}
                                    }
                                }
                            }
                        }
                    }
                    post = @{
                        summary = "Create new user"
                        requestBody = @{
                            required = $true
                            content = @{
                                "application/json" = @{
                                    schema = @{
                                        type = "object"
                                        properties = @{
                                            Name = @{type = "string"; example = "John Doe"}
                                            Email = @{type = "string"; example = "john@example.com"}
                                            Role = @{type = "string"; example = "User"}
                                        }
                                    }
                                }
                            }
                        }
                        responses = @{
                            "201" = @{description = "User created"}
                        }
                    }
                }
                "/health" = @{
                    get = @{
                        summary = "Health check"
                        responses = @{
                            "200" = @{description = "Service is healthy"}
                        }
                    }
                }
            }
            components = @{
                securitySchemes = @{
                    BearerAuth = @{
                        type = "http"
                        scheme = "bearer"
                    }
                }
            }
        }

        # Сохранение спецификации в файл
        $openapi | ConvertTo-Json -Depth 10 | Out-File -FilePath $this.OpenApiPath -Encoding UTF8
        $this.Log("OpenAPI specification saved: $($this.OpenApiPath)", "INFO")

        return @{
            StatusCode = 200
            Content = $openapi | ConvertTo-Json -Depth 10
            ContentType = "application/json"
        }
    }

    # Инициализация хранилища данных
    [hashtable]InitializeDataStore() {
        $dataFile = "api_data.json"
        $defaultData = @{
            Users = [System.Collections.ArrayList]@(
                @{Id=1; Name="John Doe"; Email="john@example.com"; Role="Admin"; CreatedAt=(Get-Date).ToString("yyyy-MM-dd HH:mm:ss")}
                @{Id=2; Name="Jane Smith"; Email="jane@example.com"; Role="User"; CreatedAt=(Get-Date).ToString("yyyy-MM-dd HH:mm:ss")}
            )
            Products = [System.Collections.ArrayList]@(
                @{Id=1; Name="PowerShell Guide"; Price=29.99; Category="Books"; Stock=100; CreatedAt=(Get-Date).ToString("yyyy-MM-dd HH:mm:ss")}
                @{Id=2; Name="REST API Course"; Price=99.99; Category="Education"; Stock=50; CreatedAt=(Get-Date).ToString("yyyy-MM-dd HH:mm:ss")}
            )
        }

        try {
            if (Test-Path $dataFile) {
                $loadedData = Get-Content $dataFile -Raw | ConvertFrom-Json
                $this.Log("Data store loaded from file", "INFO")
                return @{
                    Users = [System.Collections.ArrayList]@($loadedData.Users)
                    Products = [System.Collections.ArrayList]@($loadedData.Products)
                }
            }
        }
        catch {
            $this.Log("Error loading data store: $($_.Exception.Message)", "ERROR")
        }

        $this.Log("Using default data store", "INFO")
        return $defaultData
    }

    # Сохранение хранилища данных
    [void]SaveDataStore() {
        $dataFile = "api_data.json"
        try {
            $this.DataStore | ConvertTo-Json -Depth 5 | Out-File -FilePath $dataFile -Encoding UTF8
            $this.Log("Data store saved", "DEBUG")
        }
        catch {
            $this.Log("Error saving data store: $($_.Exception.Message)", "ERROR")
        }
    }

    # Запуск сервера
    [void]Start() {
        $this.Log("Starting REST API Server...", "INFO")
        $this.Log("Host: $($this.Host)", "INFO")
        $this.Log("Port: $($this.Port)", "INFO")
        $this.Log("Authentication: $(if($this.Config.AuthEnabled){'Enabled'}else{'Disabled'})", "INFO")

        # Инициализация данных
        $this.DataStore = $this.InitializeDataStore()

        # Настройка middleware
        $this.AddMiddleware($this.GetCorsMiddleware())
        $this.AddMiddleware($this.GetLoggingMiddleware())
        $this.AddMiddleware($this.GetAuthMiddleware())

        # Настройка маршрутов
        $this.SetupUserRoutes()
        $this.SetupProductRoutes()
        $this.SetupSystemRoutes()

        # Генерация OpenAPI документации
        $this.GenerateOpenApiSpec()

        # Создание HTTP listener
        $listener = New-Object System.Net.HttpListener
        $prefix = "http://$($this.Host):$($this.Port)/"
        $listener.Prefixes.Add($prefix)

        try {
            $listener.Start()
            $this.Log("Server started successfully! Press Ctrl+C to stop.", "INFO")
            $this.Log("API Documentation: http://$($this.Host):$($this.Port)/docs", "INFO")
            $this.Log("Health Check: http://$($this.Host):$($this.Port)/health", "INFO")

            # Основной цикл обработки запросов
            while ($listener.IsListening) {
                $context = $listener.GetContext()
                
                # Обработка запроса в отдельном потоке
                Start-Job -ScriptBlock {
                    param($Context, $ServiceInstance)
                    
                    $request = $Context.Request
                    $response = $Context.Response
                    
                    # Подготовка контекста запроса
                    $requestContext = @{
                        Method = $request.HttpMethod
                        Path = $request.Url.LocalPath
                        Headers = $request.Headers
                        RemoteEndPoint = $request.RemoteEndPoint.ToString()
                        Service = $ServiceInstance
                        DataStore = $ServiceInstance.DataStore
                    }

                    # Чтение тела запроса
                    try {
                        $bodyStream = $request.InputStream
                        $reader = New-Object System.IO.StreamReader($bodyStream, $request.ContentEncoding)
                        $requestContext.RequestBody = $reader.ReadToEnd()
                        $reader.Close()
                        $bodyStream.Close()
                    }
                    catch {
                        $requestContext.RequestBody = ""
                    }

                    # Выполнение middleware
                    $continueProcessing = $true
                    foreach ($middleware in $ServiceInstance.Middlewares) {
                        $result = & $middleware $requestContext
                        if (-not $result) {
                            $continueProcessing = $false
                            break
                        }
                    }

                    # Обработка маршрута
                    if ($continueProcessing) {
                        $methodRoutes = $ServiceInstance.Routes[$requestContext.Method]
                        if ($methodRoutes) {
                            $matched = $false
                            
                            foreach ($routePath in $methodRoutes.Keys) {
                                $pattern = $routePath -replace '{[^}]+}', '([^/]+)'
                                if ($requestContext.Path -match "^$pattern`$") {
                                    $matched = $true
                                    
                                    # Извлечение параметров
                                    $requestContext.Parameters = @{}
                                    $paramNames = @()
                                    if ($routePath -match '{([^}]+)}') {
                                        $paramNames = [regex]::Matches($routePath, '{([^}]+)}') | ForEach-Object { $_.Groups[1].Value }
                                    }
                                    
                                    for ($i = 0; $i -lt $paramNames.Count; $i++) {
                                        $requestContext.Parameters[$paramNames[$i]] = $matches[$i + 1]
                                    }
                                    
                                    # Выполнение обработчика
                                    $handler = $methodRoutes[$routePath]
                                    $result = & $handler $requestContext
                                    $requestContext.Response = $result
                                    break
                                }
                            }
                            
                            if (-not $matched) {
                                $requestContext.Response = @{
                                    StatusCode = 404
                                    Content = (@{Error = "Endpoint not found"; Path = $requestContext.Path} | ConvertTo-Json)
                                    ContentType = "application/json"
                                }
                            }
                        } else {
                            $requestContext.Response = @{
                                StatusCode = 405
                                Content = (@{Error = "Method not allowed"; Method = $requestContext.Method} | ConvertTo-Json)
                                ContentType = "application/json"
                            }
                        }
                    }

                    # Отправка ответа
                    $responseData = $requestContext.Response
                    $buffer = [System.Text.Encoding]::UTF8.GetBytes($responseData.Content)
                    $response.ContentLength64 = $buffer.Length
                    $response.StatusCode = $responseData.StatusCode
                    $response.ContentType = $responseData.ContentType
                    
                    # CORS headers
                    if ($ServiceInstance.Config.CorsEnabled) {
                        $response.Headers.Add("Access-Control-Allow-Origin", "*")
                        $response.Headers.Add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
                        $response.Headers.Add("Access-Control-Allow-Headers", "Content-Type, Authorization")
                    }
                    
                    $output = $response.OutputStream
                    $output.Write($buffer, 0, $buffer.Length)
                    $output.Close()
                    
                } -ArgumentList $context, $this | Out-Null
            }
        }
        catch {
            $this.Log("Server error: $($_.Exception.Message)", "ERROR")
        }
        finally {
            if ($listener.IsListening) {
                $listener.Stop()
            }
            $listener.Close()
            $this.Log("Server stopped", "INFO")
        }
    }
}

# Docker конфигурация
function New-DockerConfig {
    Write-Host "Creating Docker configuration..." -ForegroundColor Yellow
    
    $dockerfile = @"
# PowerShell REST API Microservice Dockerfile
FROM mcr.microsoft.com/powershell:7.2-nanoserver-ltsc2022

# Install dependencies
RUN pwsh -Command "Install-Module -Name PSScriptAnalyzer -Force"

# Set working directory
WORKDIR /app

# Copy application files
COPY . .

# Expose port
EXPOSE 8080

# Start the application
CMD ["pwsh", "-File", "Task_3.ps1", "-DockerMode"]
"@

    $dockerfile | Out-File -FilePath "Dockerfile" -Encoding UTF8
    
    $dockerCompose = @"
version: '3.8'
services:
  ps-rest-api:
    build: .
    ports:
      - "8080:8080"
    environment:
      - ASPNETCORE_ENVIRONMENT=Development
    volumes:
      - ./data:/app/data
    restart: unless-stopped
"@

    $dockerCompose | Out-File -FilePath "docker-compose.yml" -Encoding UTF8
    
    Write-Host "Docker configuration created successfully!" -ForegroundColor Green
    Write-Host "To build and run: docker-compose up --build" -ForegroundColor Cyan
}

# Демонстрация работы API
function Show-ApiDemo {
    Write-Host "Starting API Demo..." -ForegroundColor Yellow
    
    $apiService = [RestApiService]::new()
    $apiService.Config.AuthEnabled = $false  # Отключаем аутентификацию для демо
    
    # Запуск сервера в фоновом режиме
    $serverJob = Start-Job -ScriptBlock {
        param($Service)
        $Service.Start()
    } -ArgumentList $apiService
    
    # Ждем запуска сервера
    Start-Sleep -Seconds 3
    
    $baseUrl = "http://localhost:8080"
    
    try {
        Write-Host "Testing API endpoints..." -ForegroundColor Cyan
        
        # Тест health check
        Write-Host "1. Health Check..." -NoNewline
        $health = Invoke-RestMethod -Uri "$baseUrl/health" -Method Get
        Write-Host " ✓ Healthy" -ForegroundColor Green
        
        # Тест получения пользователей
        Write-Host "2. Get Users..." -NoNewline
        $users = Invoke-RestMethod -Uri "$baseUrl/users" -Method Get
        Write-Host " ✓ $($users.Count) users found" -ForegroundColor Green
        
        # Тест создания пользователя
        Write-Host "3. Create User..." -NoNewline
        $newUser = @{
            Name = "Demo User"
            Email = "demo@example.com"
            Role = "User"
        } | ConvertTo-Json
        
        $createdUser = Invoke-RestMethod -Uri "$baseUrl/users" -Method Post -Body $newUser -ContentType "application/json"
        Write-Host " ✓ User created (ID: $($createdUser.Id))" -ForegroundColor Green
        
        # Тест получения конкретного пользователя
        Write-Host "4. Get User by ID..." -NoNewline
        $user = Invoke-RestMethod -Uri "$baseUrl/users/1" -Method Get
        Write-Host " ✓ $($user.Name)" -ForegroundColor Green
        
        # Тест документации
        Write-Host "5. OpenAPI Documentation..." -NoNewline
        $docs = Invoke-RestMethod -Uri "$baseUrl/docs" -Method Get
        Write-Host " ✓ $($docs.info.title)" -ForegroundColor Green
        
        Write-Host "`n🎉 API Demo completed successfully!" -ForegroundColor Green
        
    }
    catch {
        Write-Host " ❌ Demo failed: $($_.Exception.Message)" -ForegroundColor Red
    }
    finally {
        # Останавливаем сервер
        Stop-Job $serverJob
        Receive-Job $serverJob
    }
}

# Основное меню
function Show-MainMenu {
    Write-Host "`n=== PowerShell REST API Microservice ===" -ForegroundColor Green
    Write-Host "1. Start API Server"
    Write-Host "2. Run API Demo"
    Write-Host "3. Generate Docker Configuration"
    Write-Host "4. View Logs"
    Write-Host "5. Exit"
    
    return Read-Host "`nSelect option (1-5)"
}

# Параметры командной строки
param(
    [switch]$DockerMode,
    [string]$Host = "localhost",
    [int]$Port = 8080,
    [switch]$NoAuth
)

# Основная логика
if ($DockerMode) {
    Write-Host "Starting in Docker mode..." -ForegroundColor Yellow
    $apiService = [RestApiService]::new()
    $apiService.Host = "0.0.0.0"
    $apiService.Port = $Port
    if ($NoAuth) { $apiService.Config.AuthEnabled = $false }
    $apiService.Start()
}
else {
    Write-Host "PowerShell REST API Microservice Initialized" -ForegroundColor Green
    
    do {
        $choice = Show-MainMenu
        
        switch ($choice) {
            "1" {
                $customHost = Read-Host "Host [default: localhost]"
                $customPort = Read-Host "Port [default: 8080]"
                $disableAuth = Read-Host "Disable authentication? (y/n) [default: n]"
                
                $apiService = [RestApiService]::new()
                if ($customHost) { $apiService.Host = $customHost }
                if ($customPort) { $apiService.Port = [int]$customPort }
                if ($disableAuth -eq "y") { $apiService.Config.AuthEnabled = $false }
                
                $apiService.Start()
            }
            "2" {
                Show-ApiDemo
            }
            "3" {
                New-DockerConfig
            }
            "4" {
                Write-Host "`nRecent Logs:" -ForegroundColor Yellow
                # Здесь можно добавить просмотр логов
                Write-Host "Log viewing feature coming soon..." -ForegroundColor Cyan
            }
            "5" {
                Write-Host "Goodbye! 🚀" -ForegroundColor Green
            }
            default {
                Write-Host "Invalid option" -ForegroundColor Red
            }
        }
        
        if ($choice -ne "5") {
            Read-Host "`nPress Enter to continue..."
        }
    } while ($choice -ne "5")
}
