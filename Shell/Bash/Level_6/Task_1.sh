#!/bin/bash

# Функция для форматирования размера файла
format_size() {
    local size=$1
    if [ $size -ge 1073741824 ]; then
        echo "$(echo "scale=2; $size/1073741824" | bc) GB"
    elif [ $size -ge 1048576 ]; then
        echo "$(echo "scale=2; $size/1048576" | bc) MB"
    elif [ $size -ge 1024 ]; then
        echo "$(echo "scale=2; $size/1024" | bc) KB"
    else
        echo "$size bytes"
    fi
}

# Функция создания бэкапа
create_backup() {
    local source_dir="$1"
    local backup_dir="$2"
    local backup_name="backup_$(date +%Y%m%d_%H%M%S).tar.gz"
    local backup_path="$backup_dir/$backup_name"
    
    echo "Creating backup of: $source_dir"
    echo "Backup location: $backup_path"
    
    # Создание бэкапа
    if tar -czf "$backup_path" -C "$(dirname "$source_dir")" "$(basename "$source_dir")" 2>/dev/null; then
        echo "✓ Backup created successfully"
        echo "$(date): Created $backup_path" >> "$backup_dir/backup.log"
        return 0
    else
        echo "✗ Backup creation failed"
        return 1
    fi
}

# Функция проверки целостности архива
verify_backup() {
    local backup_file="$1"
    echo "Verifying backup integrity..."
    
    if tar -tzf "$backup_file" > /dev/null 2>&1; then
        echo "✓ Backup verification successful"
        return 0
    else
        echo "✗ Backup verification failed"
        return 1
    fi
}

# Функция ротации бэкапов
rotate_backups() {
    local backup_dir="$1"
    local keep_count=7
    
    echo "Rotating backups (keeping last $keep_count)..."
    
    # Удаление старых бэкапов
    find "$backup_dir" -name "backup_*.tar.gz" -type f | \
    sort -r | \
    tail -n +$((keep_count + 1)) | \
    while read -r old_backup; do
        echo "Removing old backup: $(basename "$old_backup")"
        rm "$old_backup"
        echo "$(date): Removed $old_backup" >> "$backup_dir/backup.log"
    done
    
    echo "Backup rotation completed"
}

main() {
    local source_dir="${1:-.}"
    local backup_dir="${2:-./backups}"
    
    # Проверка существования исходной директории
    if [ ! -d "$source_dir" ]; then
        echo "Error: Source directory '$source_dir' not found"
        exit 1
    fi
    
    # Создание директории для бэкапов
    mkdir -p "$backup_dir"
    
    echo "=== Backup System ==="
    echo "Source: $source_dir"
    echo "Backup directory: $backup_dir"
    echo
    
    # Создание бэкапа
    if create_backup "$source_dir" "$backup_dir"; then
        local latest_backup=$(ls -t "$backup_dir"/backup_*.tar.gz 2>/dev/null | head -1)
        
        # Проверка целостности
        if verify_backup "$latest_backup"; then
            # Ротация бэкапов
            rotate_backups "$backup_dir"
            
            # Вывод информации о бэкапах
            echo
            echo "=== Current Backups ==="
            ls -la "$backup_dir"/backup_*.tar.gz 2>/dev/null | while read -r line; do
                echo "$line"
            done
            
            echo
            echo "Total backup size: $(format_size $(du -sb "$backup_dir" | cut -f1))"
        else
            echo "Backup verification failed - removing corrupted backup"
            rm "$latest_backup"
        fi
    fi
    
    echo
    echo "Backup process completed"
}

main "$@"
