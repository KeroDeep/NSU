#!/bin/bash

# Конфигурация
LOG_FILE="${1:-access.log}"
REPORT_DIR="log_analysis_$(date +%Y%m%d_%H%M%S)"

# Функция создания директории для отчетов
setup_report_dir() {
    mkdir -p "$REPORT_DIR"
    echo "Report directory: $REPORT_DIR"
}

# Функция проверки существования лог-файла
check_log_file() {
    if [ ! -f "$LOG_FILE" ]; then
        echo "Error: Log file '$LOG_FILE' not found"
        echo "Please provide a valid access.log file"
        exit 1
    fi
    echo "Analyzing log file: $LOG_FILE"
    echo "Total lines: $(wc -l < "$LOG_FILE")"
}

# Функция анализа топ IP-адресов
analyze_top_ips() {
    echo "=== Top 10 IP Addresses ===" > "$REPORT_DIR/top_ips.txt"
    echo "Count IP-Address" >> "$REPORT_DIR/top_ips.txt"
    echo "----- ----------" >> "$REPORT_DIR/top_ips.txt"
    
    awk '{print $1}' "$LOG_FILE" | sort | uniq -c | sort -nr | head -10 >> "$REPORT_DIR/top_ips.txt"
    
    echo "✓ Top IP analysis completed"
}

# Функция анализа популярных страниц
analyze_popular_pages() {
    echo "=== Top 10 Popular Pages ===" > "$REPORT_DIR/popular_pages.txt"
    echo "Count URL" >> "$REPORT_DIR/popular_pages.txt"
    echo "----- ---" >> "$REPORT_DIR/popular_pages.txt"
    
    awk '{print $7}' "$LOG_FILE" | sort | uniq -c | sort -nr | head -10 >> "$REPORT_DIR/popular_pages.txt"
    
    echo "✓ Popular pages analysis completed"
}

# Функция анализа ошибочных запросов
analyze_error_requests() {
    echo "=== Error Analysis (4xx and 5xx) ===" > "$REPORT_DIR/errors.txt"
    echo "Count Status URL" >> "$REPORT_DIR/errors.txt"
    echo "----- ------ ---" >> "$REPORT_DIR/errors.txt"
    
    # 4xx errors
    awk '$9 ~ /^4[0-9]{2}$/ {print $9 " " $7}' "$LOG_FILE" | \
    sort | uniq -c | sort -nr | head -10 >> "$REPORT_DIR/errors.txt"
    
    echo "" >> "$REPORT_DIR/errors.txt"
    echo "=== 5xx Server Errors ===" >> "$REPORT_DIR/errors.txt"
    
    # 5xx errors
    awk '$9 ~ /^5[0-9]{2}$/ {print $9 " " $7}' "$LOG_FILE" | \
    sort | uniq -c | sort -nr | head -10 >> "$REPORT_DIR/errors.txt"
    
    echo "✓ Error analysis completed"
}

# Функция анализа трафика по IP
analyze_traffic() {
    echo "=== Traffic by IP ===" > "$REPORT_DIR/traffic.txt"
    echo "IP-Address Requests Traffic-Bytes" >> "$REPORT_DIR/traffic.txt"
    echo "--------- -------- -------------" >> "$REPORT_DIR/traffic.txt"
    
    awk '
    {
        ip = $1
        bytes = $10
        if (bytes == "-") bytes = 0
        requests[ip]++
        traffic[ip] += bytes
    }
    END {
        for (ip in requests) {
            printf "%-15s %-8d %-12d\n", ip, requests[ip], traffic[ip]
        }
    }
    ' "$LOG_FILE" | sort -k3 -nr >> "$REPORT_DIR/traffic.txt"
    
    echo "✓ Traffic analysis completed"
}

# Функция временного анализа
analyze_timeline() {
    echo "=== Request Timeline ===" > "$REPORT_DIR/timeline.txt"
    echo "Hour Requests" >> "$REPORT_DIR/timeline.txt"
    echo "---- --------" >> "$REPORT_DIR/timeline.txt"
    
    # Извлечение часа из временной метки и подсчет
    awk -F'[:[]' '{print $2}' "$LOG_FILE" | sort | uniq -c >> "$REPORT_DIR/timeline.txt"
    
    echo "✓ Timeline analysis completed"
}

# Функция генерации сводного отчета
generate_summary() {
    local total_requests=$(wc -l < "$LOG_FILE")
    local unique_ips=$(awk '{print $1}' "$LOG_FILE" | sort -u | wc -l)
    local error_4xx=$(awk '$9 ~ /^4[0-9]{2}$/' "$LOG_FILE" | wc -l)
    local error_5xx=$(awk '$9 ~ /^5[0-9]{2}$/' "$LOG_FILE" | wc -l)
    local total_traffic=$(awk '{sum += $10} END {print sum}' "$LOG_FILE")
    
    cat > "$REPORT_DIR/summary.txt" << EOF
=== Log Analysis Summary ===
Log file: $LOG_FILE
Analysis date: $(date)

Total Requests: $total_requests
Unique IP Addresses: $unique_ips
4xx Client Errors: $error_4xx
5xx Server Errors: $error_5xx
Total Traffic: $total_traffic bytes

Generated reports:
- top_ips.txt (Top 10 IP addresses)
- popular_pages.txt (Top 10 URLs)
- errors.txt (Error analysis)
- traffic.txt (Traffic by IP)
- timeline.txt (Request timeline)
EOF

    echo "✓ Summary report generated"
}

# Функция отображения краткой статистики
show_quick_stats() {
    echo
    echo "=== Quick Statistics ==="
    echo "Total requests: $(wc -l < "$LOG_FILE")"
    echo "Unique IPs: $(awk '{print $1}' "$LOG_FILE" | sort -u | wc -l)"
    echo "Most active IP: $(awk '{print $1}' "$LOG_FILE" | sort | uniq -c | sort -nr | head -1)"
    echo "Most popular page: $(awk '{print $7}' "$LOG_FILE" | sort | uniq -c | sort -nr | head -1)"
}

main() {
    echo "Starting Apache/Nginx Log Analysis"
    echo "=================================="
    
    check_log_file
    setup_report_dir
    
    echo
    echo "Running analyses..."
    
    analyze_top_ips
    analyze_popular_pages
    analyze_error_requests
    analyze_traffic
    analyze_timeline
    generate_summary
    
    show_quick_stats
    
    echo
    echo "Analysis completed!"
    echo "Reports saved in: $REPORT_DIR"
}

main "$@"
