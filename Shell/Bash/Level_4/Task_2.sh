#!/bin/bash

LOG_FILE=${1:-"/var/log/syslog"}
REPORT_FILE="log_analysis_report.txt"

if [ ! -f "$LOG_FILE" ]; then
    echo "Error: Log file not found: $LOG_FILE"
    echo "Try: sudo $0 /var/log/syslog"
    exit 1
fi

echo "Analyzing log file: $LOG_FILE"
echo "This may take a moment..."

error_count=$(grep -i "error" "$LOG_FILE" | wc -l)
warning_count=$(grep -i "warning" "$LOG_FILE" | wc -l)
info_count=$(grep -i "info" "$LOG_FILE" | wc -l)

echo "=== Log Analysis Report ===" > "$REPORT_FILE"
echo "Generated: $(date)" >> "$REPORT_FILE"
echo "Log file: $LOG_FILE" >> "$REPORT_FILE"
echo "============================" >> "$REPORT_FILE"

echo >> "$REPORT_FILE"
echo "=== Message Type Summary ===" >> "$REPORT_FILE"
echo "ERROR:   $error_count" >> "$REPORT_FILE"
echo "WARNING: $warning_count" >> "$REPORT_FILE"
echo "INFO:    $info_count" >> "$REPORT_FILE"

echo >> "$REPORT_FILE"
echo "=== Top 5 Most Frequent Messages ===" >> "$REPORT_FILE"
grep -i "error" "$LOG_FILE" | \
    sed 's/.*error//i' | \
    sort | uniq -c | sort -nr | head -5 >> "$REPORT_FILE"

echo >> "$REPORT_FILE"
echo "=== Recent ERROR Messages ===" >> "$REPORT_FILE"
grep -i "error" "$LOG_FILE" | tail -10 >> "$REPORT_FILE"

echo "Analysis complete!"
echo "Report saved to: $REPORT_FILE"

echo
echo "=== Quick Summary ==="
echo "Total ERROR messages: $error_count"
echo "Total WARNING messages: $warning_count"
echo "Total INFO messages: $info_count"
