#!/bin/bash

# Функция анализа файла /etc/passwd
analyze_passwd() {
    echo "=== User Analysis from /etc/passwd ==="
    echo
    
    # Проверка существования файла
    if [ ! -f "/etc/passwd" ]; then
        echo "Error: /etc/passwd not found"
        return 1
    fi
    
    # Общее количество пользователей
    local total_users=$(wc -l < /etc/passwd)
    echo "Total users: $total_users"
    
    # Пользователи с bash оболочкой
    echo
    echo "=== Users with /bin/bash shell ==="
    grep "/bin/bash" /etc/passwd | cut -d: -f1,6 | while IFS=: read -r user home; do
        echo "User: $user, Home: $home"
    done
    
    # Пользователи без пароля (поле пароля пустое)
    echo
    echo "=== Users with Empty Password Field ==="
    awk -F: '($2 == "" || $2 == "!") {print $1}' /etc/passwd | while read -r user; do
        echo "User: $user (no password or locked)"
    done
    
    # Системные пользователи (UID < 1000)
    echo
    echo "=== System Users (UID < 1000) ==="
    awk -F: '($3 < 1000) {print "User: " $1 ", UID: " $3 ", Shell: " $7}' /etc/passwd
}

# Функция анализа файла /etc/group
analyze_group() {
    echo
    echo "=== Group Analysis from /etc/group ==="
    echo
    
    if [ ! -f "/etc/group" ]; then
        echo "Error: /etc/group not found"
        return 1
    fi
    
    # Общее количество групп
    local total_groups=$(wc -l < /etc/group)
    echo "Total groups: $total_groups"
    
    # Группы с наибольшим количеством пользователей
    echo
    echo "=== Groups with Most Users ==="
    awk -F: '{print $1 ":" $4}' /etc/group | while IFS=: read -r group members; do
        local count=$(echo "$members" | tr ',' '\n' | grep -c .)
        echo "$group: $count members"
    done | sort -t: -k2 -nr | head -10
}

# Функция генерации HTML отчета
generate_html_report() {
    local html_file="user_analysis_$(date +%Y%m%d_%H%M%S).html"
    
    cat > "$html_file" << EOF
<!DOCTYPE html>
<html>
<head>
    <title>User and Group Analysis</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        h1 { color: #333; }
        h2 { color: #666; border-bottom: 1px solid #ddd; padding-bottom: 5px; }
        table { border-collapse: collapse; width: 100%; margin-bottom: 20px; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background-color: #f2f2f2; }
        .warning { color: #d9534f; }
        .info { color: #5bc0de; }
    </style>
</head>
<body>
    <h1>User and Group Analysis Report</h1>
    <p>Generated on: $(date)</p>
    
    <h2>System Users</h2>
    <table>
        <tr><th>Username</th><th>UID</th><th>Shell</th></tr>
EOF

    # Добавление системных пользователей в таблицу
    awk -F: '($3 < 1000) {print "<tr><td>" $1 "</td><td>" $3 "</td><td>" $7 "</td></tr>"}' /etc/passwd >> "$html_file"
    
    cat >> "$html_file" << EOF
    </table>
    
    <h2>Users with Bash Shell</h2>
    <ul>
EOF

    # Добавление пользователей с bash
    grep "/bin/bash" /etc/passwd | cut -d: -f1 | while read -r user; do
        echo "        <li>$user</li>" >> "$html_file"
    done
    
    cat >> "$html_file" << EOF
    </ul>
    
    <h2 class="warning">Users Without Password</h2>
    <ul>
EOF

    # Добавление пользователей без пароля
    awk -F: '($2 == "" || $2 == "!") {print $1}' /etc/passwd | while read -r user; do
        echo "        <li>$user</li>" >> "$html_file"
    done
    
    cat >> "$html_file" << EOF
    </ul>
</body>
</html>
EOF

    echo "HTML report generated: $html_file"
}

main() {
    echo "Starting user and group analysis..."
    echo
    
    # Анализ пользователей
    analyze_passwd
    
    # Анализ групп
    analyze_group
    
    # Генерация HTML отчета
    echo
    generate_html_report
    
    echo
    echo "Analysis completed"
}

main "$@"
