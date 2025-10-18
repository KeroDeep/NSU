#!/bin/bash

LOG_FILE="system_monitor.log"
INTERVAL=2

get_cpu_usage() {
    echo $(top -bn1 | grep "Cpu(s)" | awk '{print $2}' | cut -d'%' -f1)
}

get_memory_usage() {
    free | grep Mem | awk '{printf "%.1f", $3/$2 * 100.0}'
}

get_disk_usage() {
    df / | awk 'NR==2 {print $5}' | cut -d'%' -f1
}

get_color() {
    local value=$1
    if [ $(echo "$value < 50" | bc) -eq 1 ]; then
        echo "32"
    elif [ $(echo "$value < 80" | bc) -eq 1 ]; then
        echo "33"
    else
        echo "31"
    fi
}

echo "Starting System Monitor (Press 'q' to quit)"
echo "Monitoring interval: ${INTERVAL}s"
echo "Log file: $LOG_FILE"
echo

> "$LOG_FILE"

while true; do
    read -t 0.1 -n 1 key
    if [[ $key == "q" ]]; then
        echo
        echo "Monitoring stopped"
        break
    fi
    
    cpu_usage=$(get_cpu_usage)
    mem_usage=$(get_memory_usage)
    disk_usage=$(get_disk_usage)
    
    cpu_color=$(get_color $cpu_usage)
    mem_color=$(get_color $mem_usage)
    disk_color=$(get_color $disk_usage)
    
    clear
    echo "=== System Monitor ==="
    echo "Time: $(date)"
    echo
    printf "CPU Usage:  \033[${cpu_color}m%5.1f%%\033[0m\n" $cpu_usage
    printf "Memory Usage:\033[${mem_color}m%5.1f%%\033[0m\n" $mem_usage
    printf "Disk Usage:  \033[${disk_color}m%5.1f%%\033[0m\n" $disk_usage
    echo
    echo "Press 'q' to quit"
    
    echo "$(date), CPU: ${cpu_usage}%, Memory: ${mem_usage}%, Disk: ${disk_usage}%" >> "$LOG_FILE"
    
    sleep $INTERVAL
done
