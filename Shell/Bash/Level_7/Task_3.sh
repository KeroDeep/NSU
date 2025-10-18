#!/bin/bash

# Функция подсчета статистики
count_stats() {
    local file="$1"
    
    if [ ! -f "$file" ]; then
        echo "Error: File '$file' not found"
        return 1
    fi
    
    local lines=$(wc -l < "$file")
    local words=$(wc -w < "$file")
    local chars=$(wc -m < "$file")
    local bytes=$(wc -c < "$file")
    
    echo "=== File Statistics: $file ==="
    echo "Lines: $lines"
    echo "Words: $words"
    echo "Characters: $chars"
    echo "Bytes: $bytes"
}

# Функция поиска и замены текста
search_replace() {
    local file="$1"
    local search="$2"
    local replace="$3"
    local use_regex="${4:-false}"
    
    if [ ! -f "$file" ]; then
        echo "Error: File '$file' not found"
        return 1
    fi
    
    local temp_file=$(mktemp)
    
    if [ "$use_regex" = "true" ]; then
        # Использование regex для поиска и замены
        sed -E "s/$search/$replace/g" "$file" > "$temp_file"
    else
        # Простая замена
        sed "s/$search/$replace/g" "$file" > "$temp_file"
    fi
    
    # Показать изменения
    echo "=== Changes made ==="
    diff --color=always -u "$file" "$temp_file" | head -20
    
    # Подтверждение замены
    echo -n "Apply these changes? (y/n): "
    read confirm
    
    if [ "$confirm" = "y" ] || [ "$confirm" = "Y" ]; then
        cp "$temp_file" "$file"
        echo "Changes applied to $file"
    else
        echo "Changes discarded"
    fi
    
    rm "$temp_file"
}

# Функция удаления дубликатов строк
remove_duplicates() {
    local file="$1"
    local preserve_order="${2:-false}"
    
    if [ ! -f "$file" ]; then
        echo "Error: File '$file' not found"
        return 1
    fi
    
    local temp_file=$(mktemp)
    
    if [ "$preserve_order" = "true" ]; then
        # Удаление дубликатов с сохранением порядка
        awk '!seen[$0]++' "$file" > "$temp_file"
    else
        # Простое удаление дубликатов с сортировкой
        sort -u "$file" > "$temp_file"
    fi
    
    local original_lines=$(wc -l < "$file")
    local unique_lines=$(wc -l < "$temp_file")
    local removed=$((original_lines - unique_lines))
    
    echo "Removed $removed duplicate lines"
    echo "Original: $original_lines lines, After: $unique_lines lines"
    
    # Показать результат
    echo -n "Preview result? (y/n): "
    read preview
    if [ "$preview" = "y" ] || [ "$preview" = "Y" ]; then
        head -10 "$temp_file"
    fi
    
    # Применить изменения
    echo -n "Apply changes? (y/n): "
    read apply
    if [ "$apply" = "y" ] || [ "$apply" = "Y" ]; then
        cp "$temp_file" "$file"
        echo "File updated"
    fi
    
    rm "$temp_file"
}

# Функция сортировки строк
sort_lines() {
    local file="$1"
    local sort_options="$2"
    
    if [ ! -f "$file" ]; then
        echo "Error: File '$file' not found"
        return 1
    fi
    
    local temp_file=$(mktemp)
    
    # Применение сортировки с указанными опциями
    eval "sort $sort_options '$file'" > "$temp_file"
    
    echo "=== Sorted result preview ==="
    head -10 "$temp_file"
    
    echo -n "Apply sorting? (y/n): "
    read apply
    if [ "$apply" = "y" ] || [ "$apply" = "Y" ]; then
        cp "$temp_file" "$file"
        echo "File sorted"
    fi
    
    rm "$temp_file"
}

# Функция форматирования вывода
format_output() {
    local file="$1"
    local format_type="$2"
    
    if [ ! -f "$file" ]; then
        echo "Error: File '$file' not found"
        return 1
    fi
    
    case "$format_type" in
        "number")
            # Нумерация строк
            cat -n "$file" | head -20
            ;;
        "align")
            # Выравнивание текста (простой пример)
            awk '{printf "| %-30s |\n", $0}' "$file" | head -10
            ;;
        "indent")
            # Добавление отступов
            sed 's/^/    /' "$file" | head -10
            ;;
        *)
            echo "Unknown format type: $format_type"
            echo "Available types: number, align, indent"
            ;;
    esac
}

# Функция отображения помощи
show_help() {
    cat << EOF
Text Processor - Command Line Tool

Usage: $0 <command> [arguments]

Commands:
  stats <file>                 - Show file statistics
  replace <file> <search> <replace> [regex] - Search and replace text
  dedup <file> [preserve]      - Remove duplicate lines
  sort <file> [options]        - Sort lines (use sort options like -r, -n)
  format <file> <type>         - Format output (number, align, indent)

Examples:
  $0 stats document.txt
  $0 replace file.txt "old" "new"
  $0 replace file.txt ".*pattern.*" "replacement" true
  $0 dedup lines.txt true
  $0 sort data.txt -n -r
  $0 format text.txt number

EOF
}

main() {
    local command="$1"
    
    case "$command" in
        "stats")
            count_stats "$2"
            ;;
        "replace")
            if [ -z "$4" ]; then
                echo "Error: search and replace text required"
                echo "Usage: $0 replace <file> <search> <replace> [regex]"
                exit 1
            fi
            search_replace "$2" "$3" "$4" "$5"
            ;;
        "dedup")
            remove_duplicates "$2" "$3"
            ;;
        "sort")
            sort_lines "$2" "$3"
            ;;
        "format")
            if [ -z "$3" ]; then
                echo "Error: format type required"
                echo "Usage: $0 format <file> <type>"
                exit 1
            fi
            format_output "$2" "$3"
            ;;
        "help"|"")
            show_help
            ;;
        *)
            echo "Unknown command: $command"
            show_help
            ;;
    esac
}

main "$@"
