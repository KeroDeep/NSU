#!/bin/bash

add() {
    echo "$1 + $2" | bc
}

subtract() {
    echo "$1 - $2" | bc
}

multiply() {
    echo "$1 * $2" | bc
}

divide() {
    if [ "$(echo "$2 == 0" | bc)" -eq 1 ]; then
        echo "Error: Division by zero" >&2
        return 1
    fi
    echo "scale=2; $1 / $2" | bc
}

power() {
    echo "$1 ^ $2" | bc
}

sqrt() {
    if [ "$(echo "$1 < 0" | bc)" -eq 1 ]; then
        echo "Error: Square root of negative number" >&2
        return 1
    fi
    echo "scale=2; sqrt($1)" | bc -l
}

validate_number() {
    [[ "$1" =~ ^-?[0-9]+\.?[0-9]*$ ]]
}

validate_operation() {
    [[ "$1" =~ ^(\+|-|\*|/|pow|sqrt)$ ]]
}

LOG_FILE="calculator_history.csv"

log_operation() {
    local op=$1 num1=$2 num2=$3 result=$4
    local timestamp=$(date +"%Y-%m-%d %H:%M:%S")
    
    if [ ! -f "$LOG_FILE" ]; then
        echo "timestamp,operation,number1,number2,result" > "$LOG_FILE"
    fi
    
    echo "$timestamp,$op,$num1,$num2,$result" >> "$LOG_FILE"
}

show_history() {
    if [ -f "$LOG_FILE" ]; then
        echo "=== Calculation History ==="
        tail -10 "$LOG_FILE" | column -t -s,
    else
        echo "No history found"
    fi
}

echo "Advanced Calculator"
echo "Operations: +, -, *, /, pow, sqrt"

while true; do
    echo
    echo -n "Enter operation (or 'quit' to exit): "
    read operation
    
    if [ "$operation" = "quit" ]; then
        echo "Goodbye!"
        break
    fi
    
    if ! validate_operation "$operation"; then
        echo "Error: Invalid operation"
        continue
    fi
    
    if [ "$operation" = "sqrt" ]; then
        echo -n "Enter number: "
        read num1
        if ! validate_number "$num1"; then
            echo "Error: Invalid number"
            continue
        fi
        result=$(sqrt "$num1")
    else
        echo -n "Enter first number: "
        read num1
        echo -n "Enter second number: "
        read num2
        
        if ! validate_number "$num1" || ! validate_number "$num2"; then
            echo "Error: Invalid numbers"
            continue
        fi
        
        case $operation in
            "+") result=$(add "$num1" "$num2") ;;
            "-") result=$(subtract "$num1" "$num2") ;;
            "*") result=$(multiply "$num1" "$num2") ;;
            "/") result=$(divide "$num1" "$num2") ;;
            "pow") result=$(power "$num1" "$num2") ;;
        esac
    fi
    
    if [ $? -eq 0 ]; then
        echo "Result: $result"
        log_operation "$operation" "$num1" "$num2" "$result"
    fi
done

echo
show_history
