#!/bin/bash

# Конфигурация
LOG_FILE="deployment.log"
BACKUP_DIR="backups"
CONFIG_DIR="configs"

# Функция логирования
log() {
    local message="$1"
    local timestamp=$(date +"%Y-%m-%d %H:%M:%S")
    echo "[$timestamp] $message" | tee -a "$LOG_FILE"
}

# Функция проверки зависимостей
check_dependencies() {
    log "Checking system dependencies..."
    
    local missing_deps=()
    
    # Проверка обязательных пакетов
    for package in curl wget tar; do
        if ! command -v "$package" &> /dev/null; then
            missing_deps+=("$package")
        fi
    done
    
    if [ ${#missing_deps[@]} -gt 0 ]; then
        log "Missing dependencies: ${missing_deps[*]}"
        return 1
    fi
    
    log "✓ All dependencies satisfied"
    return 0
}

# Функция проверки окружения
validate_environment() {
    log "Validating deployment environment..."
    
    # Проверка свободного места на диске
    local free_space=$(df / | awk 'NR==2 {print $4}')
    if [ "$free_space" -lt 1048576 ]; then  # Меньше 1GB
        log "✗ Insufficient disk space: ${free_space}KB available"
        return 1
    fi
    
    # Проверка доступности интернета
    if ! curl -s --connect-timeout 5 http://google.com > /dev/null; then
        log "✗ No internet connection"
        return 1
    fi
    
    # Проверка прав доступа
    if [ ! -w "." ]; then
        log "✗ No write permission in current directory"
        return 1
    fi
    
    log "✓ Environment validation passed"
    return 0
}

# Функция установки пакетов
install_packages() {
    local packages=("nginx" "nodejs" "python3" "git")
    local installed=()
    local failed=()
    
    log "Installing required packages..."
    
    for package in "${packages[@]}"; do
        log "Installing $package..."
        
        if command -v apt-get &> /dev/null; then
            # Ubuntu/Debian
            if sudo apt-get install -y "$package" 2>/dev/null; then
                installed+=("$package")
                log "✓ Installed $package"
            else
                failed+=("$package")
                log "✗ Failed to install $package"
            fi
        elif command -v yum &> /dev/null; then
            # CentOS/RHEL
            if sudo yum install -y "$package" 2>/dev/null; then
                installed+=("$package")
                log "✓ Installed $package"
            else
                failed+=("$package")
                log "✗ Failed to install $package"
            fi
        else
            log "✗ Package manager not supported"
            return 1
        fi
    done
    
    if [ ${#failed[@]} -gt 0 ]; then
        log "Failed to install: ${failed[*]}"
        return 1
    fi
    
    log "✓ All packages installed successfully: ${installed[*]}"
    return 0
}

# Функция настройки конфигурационных файлов
setup_configurations() {
    local app_name="${1:-myapp}"
    
    log "Setting up configuration files..."
    
    mkdir -p "$CONFIG_DIR"
    
    # Создание конфига nginx
    cat > "$CONFIG_DIR/nginx.conf" << EOF
server {
    listen 80;
    server_name $app_name.local;
    
    location / {
        proxy_pass http://localhost:3000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host \$host;
        proxy_cache_bypass \$http_upgrade;
    }
    
    access_log /var/log/nginx/$app_name.access.log;
    error_log /var/log/nginx/$app_name.error.log;
}
EOF

    # Создание конфига приложения
    cat > "$CONFIG_DIR/app.env" << EOF
APP_NAME=$app_name
APP_PORT=3000
APP_ENV=production
DB_HOST=localhost
DB_PORT=5432
EOF

    log "✓ Configuration files created in $CONFIG_DIR/"
}

# Функция создания бэкапа
create_backup() {
    local backup_name="backup_$(date +%Y%m%d_%H%M%S).tar.gz"
    
    log "Creating backup: $backup_name"
    
    mkdir -p "$BACKUP_DIR"
    
    # Бэкап важных директорий
    local backup_items=("$CONFIG_DIR" "app" "data")
    local items_to_backup=()
    
    for item in "${backup_items[@]}"; do
        if [ -e "$item" ]; then
            items_to_backup+=("$item")
        fi
    done
    
    if [ ${#items_to_backup[@]} -eq 0 ]; then
        log "No items to backup"
        return 0
    fi
    
    if tar -czf "$BACKUP_DIR/$backup_name" "${items_to_backup[@]}" 2>/dev/null; then
        log "✓ Backup created: $backup_name"
        return 0
    else
        log "✗ Backup creation failed"
        return 1
    fi
}

# Функция отката
rollback_deployment() {
    local backup_pattern="$1"
    
    log "Starting rollback process..."
    
    if [ -z "$backup_pattern" ]; then
        # Найти последний бэкап
        local latest_backup=$(ls -t "$BACKUP_DIR"/backup_*.tar.gz 2>/dev/null | head -1)
        if [ -z "$latest_backup" ]; then
            log "✗ No backup found for rollback"
            return 1
        fi
        backup_pattern="$latest_backup"
    fi
    
    if [ ! -f "$backup_pattern" ]; then
        log "✗ Backup file not found: $backup_pattern"
        return 1
    fi
    
    log "Restoring from backup: $backup_pattern"
    
    # Восстановление из бэкапа
    if tar -xzf "$backup_pattern" -C / 2>/dev/null; then
        log "✓ Rollback completed successfully"
        return 0
    else
        log "✗ Rollback failed"
        return 1
    fi
}

# Функция развертывания приложения
deploy_application() {
    local repo_url="${1:-https://github.com/example/app.git}"
    local branch="${2:-main}"
    
    log "Starting application deployment..."
    log "Repository: $repo_url"
    log "Branch: $branch"
    
    # Создание бэкапа перед развертыванием
    if ! create_backup; then
        log "✗ Backup creation failed - aborting deployment"
        return 1
    fi
    
    # Клонирование/обновление репозитория
    if [ -d "app" ]; then
        log "Updating existing application..."
        cd app && git pull origin "$branch" && cd - || {
            log "✗ Failed to update application"
            return 1
        }
    else
        log "Cloning application repository..."
        if git clone -b "$branch" "$repo_url" app 2>/dev/null; then
            log "✓ Application cloned successfully"
        else
            log "✗ Failed to clone application"
            return 1
        fi
    fi
    
    # Установка зависимостей приложения
    log "Installing application dependencies..."
    if [ -f "app/package.json" ]; then
        cd app && npm install && cd - || {
            log "✗ Failed to install Node.js dependencies"
            return 1
        }
    fi
    
    if [ -f "app/requirements.txt" ]; then
        cd app && pip3 install -r requirements.txt && cd - || {
            log "✗ Failed to install Python dependencies"
            return 1
        }
    fi
    
    # Запуск приложения
    log "Starting application..."
    # Здесь должна быть логика запуска конкретного приложения
    
    log "✓ Application deployment completed"
    return 0
}

# Основная функция развертывания
main_deployment() {
    local action="${1:-deploy}"
    local app_name="${2:-myapp}"
    
    log "=== Deployment System ==="
    log "Action: $action"
    log "Application: $app_name"
    
    case "$action" in
        "deploy")
            if ! check_dependencies; then
                log "Deployment aborted due to missing dependencies"
                exit 1
            fi
            
            if ! validate_environment; then
                log "Deployment aborted due to environment issues"
                exit 1
            fi
            
            install_packages
            setup_configurations "$app_name"
            deploy_application
            
            log "🎉 Deployment completed successfully!"
            ;;
        "rollback")
            rollback_deployment "$2"
            ;;
        "backup")
            create_backup
            ;;
        "validate")
            check_dependencies
            validate_environment
            ;;
        *)
            log "Unknown action: $action"
            echo "Usage: $0 [deploy|rollback|backup|validate] [app_name]"
            exit 1
            ;;
    esac
}

main_deployment "$@"
