#!/bin/bash

total_size=0
declare -A file_types
declare -a large_files

analyze_directory() {
    local dir="$1"
    local prefix="$2"
    
    echo "${prefix}📁 $(basename "$dir")/"
    
    local items=()
    while IFS= read -r -d '' item; do
        items+=("$item")
    done < <(find "$dir" -maxdepth 1 -mindepth 1 -print0 2>/dev/null | sort -z)
    
    local count=${#items[@]}
    local i=0
    
    for item in "${items[@]}"; do
        i=$((i + 1))
        local name=$(basename "$item")
        
        if [ -d "$item" ]; then
            if [ $i -eq $count ]; then
                analyze_directory "$item" "${prefix}    "
            else
                analyze_directory "$item" "${prefix}│   "
            fi
        else
            local size=$(stat -f%z "$item" 2>/dev/null || stat -c%s "$item" 2>/dev/null)
            total_size=$((total_size + size))
            
            local extension="${name##*.}"
            if [ "$extension" = "$name" ]; then
                extension="no extension"
            fi
            file_types["$extension"]=$((file_types["$extension"] + 1))
            
            large_files+=("$size:$item")
            
            if [ $i -eq $count ]; then
                echo "${prefix}└── 📄 $name ($(format_size $size))"
            else
                echo "${prefix}├── 📄 $name ($(format_size $size))"
            fi
        fi
    done
}

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

show_largest_files() {
    echo
    echo "=== Top 10 Largest Files ==="
    printf "%-60s %-15s\n" "File" "Size"
    printf "%-60s %-15s\n" "----" "----"
    
    for entry in $(printf "%s\n" "${large_files[@]}" | sort -nr | head -10); do
        size=$(echo "$entry" | cut -d: -f1)
        file=$(echo "$entry" | cut -d: -f2-)
        printf "%-60s %-15s\n" "$(basename "$file")" "$(format_size $size)"
    done
}

show_file_types() {
    echo
    echo "=== File Types Summary ==="
    printf "%-20s %-10s\n" "Extension" "Count"
    printf "%-20s %-10s\n" "---------" "-----"
    
    for ext in "${!file_types[@]}"; do
        printf "%-20s %-10s\n" "$ext" "${file_types[$ext]}"
    done
}

main() {
    local start_dir="${1:-.}"
    
    if [ ! -d "$start_dir" ]; then
        echo "Error: Directory '$start_dir' not found"
        exit 1
    fi
    
    echo "Analyzing directory: $(realpath "$start_dir")"
    echo
    
    analyze_directory "$start_dir" ""
    
    echo
    echo "=== Summary ==="
    echo "Total size: $(format_size $total_size)"
    echo "Total files: ${#large_files[@]}"
    
    show_largest_files
    show_file_types
}

main "$@"
