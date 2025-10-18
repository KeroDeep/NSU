#!/bin/bash

TASKS_FILE="tasks.csv"

init_tasks_file() {
    if [ ! -f "$TASKS_FILE" ]; then
        echo "id,description,status,created_at,completed_at" > "$TASKS_FILE"
    fi
}

add_task() {
    local description="$1"
    local id=$(date +%s)
    local timestamp=$(date +"%Y-%m-%d %H:%M:%S")
    
    echo "$id,\"$description\",pending,$timestamp," >> "$TASKS_FILE"
    echo "Task added successfully (ID: $id)"
}

list_tasks() {
    local status_filter="$1"
    
    echo "=== Tasks ==="
    printf "%-12s %-30s %-10s %-20s\n" "ID" "Description" "Status" "Created"
    printf "%-12s %-30s %-10s %-20s\n" "---" "-----------" "------" "-------"
    
    while IFS=, read -r id description task_status created_at completed_at; do
        if [ -z "$status_filter" ] || [ "$task_status" = "$status_filter" ]; then
            description=$(echo "$description" | sed 's/^"//;s/"$//')
            printf "%-12s %-30s %-10s %-20s\n" "$id" "$description" "$task_status" "$created_at"
        fi
    done < <(tail -n +2 "$TASKS_FILE")
}

complete_task() {
    local task_id="$1"
    local timestamp=$(date +"%Y-%m-%d %H:%M:%S")
    local temp_file=$(mktemp)
    
    head -1 "$TASKS_FILE" > "$temp_file"
    
    while IFS=, read -r id description task_status created_at completed_at; do
        if [ "$id" = "$task_id" ]; then
            if [ "$task_status" = "completed" ]; then
                echo "Task already completed"
            else
                echo "$id,$description,completed,$created_at,$timestamp" >> "$temp_file"
                echo "Task completed successfully"
            fi
        else
            echo "$id,$description,$task_status,$created_at,$completed_at" >> "$temp_file"
        fi
    done < <(tail -n +2 "$TASKS_FILE")
    
    mv "$temp_file" "$TASKS_FILE"
}

delete_task() {
    local task_id="$1"
    local temp_file=$(mktemp)
    
    head -1 "$TASKS_FILE" > "$temp_file"
    local found=0
    
    while IFS=, read -r id description task_status created_at completed_at; do
        if [ "$id" != "$task_id" ]; then
            echo "$id,$description,$task_status,$created_at,$completed_at" >> "$temp_file"
        else
            found=1
        fi
    done < <(tail -n +2 "$TASKS_FILE")
    
    if [ $found -eq 1 ]; then
        mv "$temp_file" "$TASKS_FILE"
        echo "Task deleted successfully"
    else
        rm "$temp_file"
        echo "Task not found"
    fi
}

search_tasks() {
    local query="$1"
    
    echo "=== Search Results for: '$query' ==="
    printf "%-12s %-30s %-10s %-20s\n" "ID" "Description" "Status" "Created"
    printf "%-12s %-30s %-10s %-20s\n" "---" "-----------" "------" "-------"
    
    while IFS=, read -r id description task_status created_at completed_at; do
        description_clean=$(echo "$description" | sed 's/^"//;s/"$//')
        if echo "$description_clean" | grep -q -i "$query"; then
            printf "%-12s %-30s %-10s %-20s\n" "$id" "$description_clean" "$task_status" "$created_at"
        fi
    done < <(tail -n +2 "$TASKS_FILE")
}

export_tasks() {
    local format="$1"
    local output_file="tasks_export_$(date +%Y%m%d_%H%M%S)"
    
    case $format in
        "csv")
            cp "$TASKS_FILE" "${output_file}.csv"
            echo "Tasks exported to: ${output_file}.csv"
            ;;
        "txt")
            {
                echo "Tasks Export - $(date)"
                echo "======================"
                list_tasks
            } > "${output_file}.txt"
            echo "Tasks exported to: ${output_file}.txt"
            ;;
        *)
            echo "Invalid format. Use 'csv' or 'txt'"
            ;;
    esac
}

show_help() {
    echo "Task Manager Commands:"
    echo "  add <description>    - Add new task"
    echo "  list [status]        - List tasks (optional: pending/completed)"
    echo "  complete <id>        - Mark task as completed"
    echo "  delete <id>          - Delete task"
    echo "  search <query>       - Search tasks"
    echo "  export <format>      - Export tasks (csv/txt)"
    echo "  help                 - Show this help"
}

main() {
    init_tasks_file
    
    local command="$1"
    shift
    
    case $command in
        "add")
            if [ -z "$1" ]; then
                echo "Error: Task description required"
                exit 1
            fi
            add_task "$*"
            ;;
        "list")
            list_tasks "$1"
            ;;
        "complete")
            if [ -z "$1" ]; then
                echo "Error: Task ID required"
                exit 1
            fi
            complete_task "$1"
            ;;
        "delete")
            if [ -z "$1" ]; then
                echo "Error: Task ID required"
                exit 1
            fi
            delete_task "$1"
            ;;
        "search")
            if [ -z "$1" ]; then
                echo "Error: Search query required"
                exit 1
            fi
            search_tasks "$1"
            ;;
        "export")
            if [ -z "$1" ]; then
                echo "Error: Export format required (csv/txt)"
                exit 1
            fi
            export_tasks "$1"
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
