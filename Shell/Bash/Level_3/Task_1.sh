#!/bin/bash

LOG_FILE="calculator_history.txt"

add() {
    echo "$1 + $2 = $(($1 + $2))"
    echo "$(date): $1 + $2 = $(($1 + $2))" >> "$LOG_FILE"
}

subtract() {
    echo "$1 - $2 = $(($1 - $2))"
    echo "$(date): $1 - $2 = $(($1 - $2))" >> "$LOG_FILE"
}

multiply() {
    echo "$1 * $2 = $(($1 * $2))"
    echo "$(date): $1 * $2 = $(($1 * $2))" >> "$LOG_FILE"
}

divide() {
    if [ "$2" -eq 0 ]; then
        echo "Error: Division by zero!"
        return 1
    fi
    result=$(echo "scale=2; $1 / $2" | bc)
    echo "$1 / $2 = $result"
    echo "$(date): $1 / $2 = $result" >> "$LOG_FILE"
}

echo "Simple Calculator"
echo "1. Addition"
echo "2. Subtraction"
echo "3. Multiplication"
echo "4. Division"
echo -n "Choose operation (1-4): "
read operation

echo -n "Enter first number: "
read num1
echo -n "Enter second number: "
read num2

case $operation in
    1) add $num1 $num2 ;;
    2) subtract $num1 $num2 ;;
    3) multiply $num1 $num2 ;;
    4) divide $num1 $num2 ;;
    *) echo "Invalid operation" ;;
esac

echo
echo "Calculation logged to $LOG_FILE"
