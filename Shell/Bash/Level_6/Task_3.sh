#!/bin/bash

# Конфигурация
MONITOR_DIR="${1:-.}"
LOG_FILE="file_changes.log"
CHECK_INTERVAL=5

# Функция получения хеша файла
get_file_hash() {
    local file="$1"
    if [ -f "$file" ]; then
        md5sum "$file" 2>/dev/null | cut -d' ' -f1
    else
        echo "none"
    fi
}

# Функция получения информации о файле
get_file_info() {
    local file="$1"
    if [ -e "$file" ]; then
        if [ -f "$file" ]; then
            echo "file:$(stat -c %s "$file" 2>/dev/null || stat -f %z "$file"):$(stat -c %Y "$file" 2>/dev/null || stat -f %m "$file")"
        elif [ -d "$file" ]; then
            echo "directory"
        elif [ -L "$file" ]; then
            echo "symlink"
        fi
    else
        echo "nonexistent"
    fi
}

# Функция логирования изменений
log_change() {
    local timestamp=$(date +"%Y-%m-%d %H:%M:%S")
    local change_type="$1"
    local file="$2"
    local details="$3"
    
    echo "[$timestamp] $change_type: $file $details" | tee -a "$LOG_FILE"
    
    # Отправка уведомления для критических изменений
    if [[ "$change_type" == "DELETED" || "$change_type" == "PERMISSION_CHANGE" ]]; then
        echo "ALERT: Critical change detected - $change_type: $file"
    fi
}

# Функция сравнения файлов
compare_files() {
    local file="$1"
    local old_hash="$2"
    local new_hash="$3"
    
    if [ "$old_hash" != "$new_hash" ]; then
        if [ "$old_hash" = "none" ] && [ "$new_hash" != "none" ]; then
            log_change "CREATED" "$file" "(size: $(stat -c %s "$file" 2>/dev/null || stat -f %z "$file"))"
        elif [ "$old_hash" != "none" ] && [ "$new_hash" = "none" ]; then
            log_change "DELETED" "$file" ""
        else
            log_change "MODIFIED" "$file" "(hash changed)"
        fi
    fi
}

# Функция мониторинга в реальном времени
monitor_realtime() {
    echo "Starting real-time monitoring of: $MONITOR_DIR"
    echo "Log file: $LOG_FILE"
    echo "Press Ctrl+C to stop"
    echo
    
    # Инициализация хешей
    declare -A file_hashes
    declare -A file_permissions
    
    while read -r -d '' file; do
        file_hashes["$file"]=$(get_file_hash "$file")
        file_permissions["$file"]=$(stat -c %a "$file" 2>/dev/null || stat -f %A "$file")
    done < <(find "$MONITOR_DIR" -type f -print0 2>/dev/null)
    
    # Основной цикл мониторинга
    while true; do
        # Проверка существующих файлов
        while read -r -d '' file; do
            current_hash=$(get_file_hash "$file")
            current_perm=$(stat -c %a "$file" 2>/dev/null || stat -f %A "$file")
            
            # Проверка изменений содержимого
            compare_files "$file" "${file_hashes["$file"]}" "$current_hash"
            
            # Проверка изменений прав доступа
            if [ "${file_permissions["$file"]}" != "$current_perm" ]; then
                log_change "PERMISSION_CHANGE" "$file" "from ${file_permissions["$file"]} to $current_perm"
                file_permissions["$file"]=$current_perm
            fi
            
            file_hashes["$file"]=$current_hash
        done < <(find "$MONITOR_DIR" -type f -print0 2>/dev/null)
        
        # Проверка удаленных файлов
        for file in "${!file_hashes[@]}"; do
            if [ ! -e "$file" ]; then
                log_change "DELETED" "$file" ""
                unset file_hashes["$file"]
                unset file_permissions["$file"]
            fi
        done
        
        sleep "$CHECK_INTERVAL"
    done
}

# Функция периодической проверки
monitor_periodic() {
    local snapshot_file="snapshot_$(date +%Y%m%d_%H%M%S).txt"
    
    echo "Creating snapshot: $snapshot_file"
    find "$MONITOR_DIR" -type f -exec ls -la {} \; > "$snapshot_file" 2>/dev/null
    
    echo "Snapshot created. Use diff to compare with previous snapshots."
}

main() {
    local mode="${2:-realtime}"
    
    echo "=== File System Monitor ==="
    echo "Directory: $MONITOR_DIR"
    echo "Mode: $mode"
    echo
    
    case "$mode" in
        "realtime")
            # Перехват сигнала Ctrl+C для graceful shutdown
            trap 'echo -e "\nMonitoring stopped"; exit 0' INT
            monitor_realtime
            ;;
        "snapshot")
            monitor_periodic
            ;;
        *)
            echo "Invalid mode. Use 'realtime' or 'snapshot'"
            echo "Usage: $0 [directory] [realtime|snapshot]"
            exit 1
            ;;
    esac
}

main "$@"
