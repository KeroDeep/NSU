#!/bin/bash

# Функция валидации email
validate_email() {
    local email="$1"
    local pattern="^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$"
    
    if [[ $email =~ $pattern ]]; then
        echo "✓ VALID email: $email"
        return 0
    else
        echo "✗ INVALID email: $email"
        return 1
    fi
}

# Функция валидации номера телефона
validate_phone() {
    local phone="$1"
    local pattern="^\+7\s?\([0-9]{3}\)\s?[0-9]{3}-[0-9]{2}-[0-9]{2}$"
    
    if [[ $phone =~ $pattern ]]; then
        echo "✓ VALID phone: $phone"
        return 0
    else
        echo "✗ INVALID phone: $phone"
        return 1
    fi
}

# Функция валидации IP-адреса
validate_ip() {
    local ip="$1"
    local pattern="^([0-9]{1,3}\.){3}[0-9]{1,3}$"
    
    if [[ $ip =~ $pattern ]]; then
        # Проверка каждого октета
        IFS='.' read -ra octets <<< "$ip"
        local valid=0
        for octet in "${octets[@]}"; do
            if [ "$octet" -gt 255 ]; then
                valid=1
                break
            fi
        done
        
        if [ $valid -eq 0 ]; then
            echo "✓ VALID IP: $ip"
            return 0
        fi
    fi
    
    echo "✗ INVALID IP: $ip"
    return 1
}

# Функция валидации даты
validate_date() {
    local date="$1"
    local pattern="^(0[1-9]|[12][0-9]|3[01])\.(0[1-9]|1[0-2])\.([0-9]{4})$"
    
    if [[ $date =~ $pattern ]]; then
        local day=${BASH_REMATCH[1]}
        local month=${BASH_REMATCH[2]}
        local year=${BASH_REMATCH[3]}
        
        # Проверка корректности даты
        if date -d "$year-$month-$day" >/dev/null 2>&1; then
            echo "✓ VALID date: $date"
            return 0
        fi
    fi
    
    echo "✗ INVALID date: $date"
    return 1
}

# Функция создания тестового файла
create_test_file() {
    local test_file="test_data.txt"
    
    cat > "$test_file" << EOF
emails:
test@example.com
invalid-email
user.name@domain.co.uk
another@test

phones:
+7(123)456-78-90
+7 (999) 123-45-67
81234567890
+7(111)222-33

IPs:
192.168.1.1
256.300.1.1
10.0.0.1
999.999.999.999

dates:
31.12.2023
29.02.2023
15.06.2024
32.01.2022
EOF

    echo "Test file created: $test_file"
}

# Функция обработки тестового файла
process_test_file() {
    local test_file="${1:-test_data.txt}"
    
    if [ ! -f "$test_file" ]; then
        echo "Error: Test file '$test_file' not found"
        return 1
    fi
    
    echo "Processing test file: $test_file"
    echo "================================="
    
    local current_section=""
    
    while IFS= read -r line; do
        # Пропуск пустых строк
        if [[ -z "$line" ]]; then
            continue
        fi
        
        # Определение секции
        if [[ "$line" =~ ^[a-zA-Z]+:$ ]]; then
            current_section="${line%:}"
            echo
            echo "=== $current_section ==="
            continue
        fi
        
        # Валидация в зависимости от секции
        case "$current_section" in
            "emails")
                validate_email "$line"
                ;;
            "phones")
                validate_phone "$line"
                ;;
            "IPs")
                validate_ip "$line"
                ;;
            "dates")
                validate_date "$line"
                ;;
        esac
        
    done < "$test_file"
}

main() {
    local mode="${1:-file}"
    
    case "$mode" in
        "file")
            if [ ! -f "test_data.txt" ]; then
                create_test_file
                echo
            fi
            process_test_file "test_data.txt"
            ;;
        "interactive")
            echo "Interactive Data Validator"
            echo "1. Validate Email"
            echo "2. Validate Phone"
            echo "3. Validate IP"
            echo "4. Validate Date"
            echo -n "Choose option (1-4): "
            read option
            
            echo -n "Enter value to validate: "
            read value
            
            case $option in
                1) validate_email "$value" ;;
                2) validate_phone "$value" ;;
                3) validate_ip "$value" ;;
                4) validate_date "$value" ;;
                *) echo "Invalid option" ;;
            esac
            ;;
        "create-test")
            create_test_file
            ;;
        *)
            echo "Usage: $0 [file|interactive|create-test]"
            echo "  file - Process test file (default)"
            echo "  interactive - Interactive validation"
            echo "  create-test - Create test data file"
            ;;
    esac
}

main "$@"
