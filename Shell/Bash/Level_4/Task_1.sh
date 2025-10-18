#!/bin/bash

print_multiplication_table() {
    local start=$1
    local end=$2
    
    echo "Multiplication Table from $start to $end"
    echo "========================================="
    
    printf "     "
    for ((i=start; i<=end; i++)); do
        printf "%-4d" $i
    done
    echo
    
    for ((i=start; i<=end; i++)); do
        printf "%-4d " $i
        for ((j=start; j<=end; j++)); do
            result=$((i * j))
            if [ $((result % 2)) -eq 0 ]; then
                printf "\033[32m%-4d\033[0m" $result
            else
                printf "\033[34m%-4d\033[0m" $result
            fi
        done
        echo
    done
}

echo -n "Enter start number (default 1): "
read start
start=${start:-1}

echo -n "Enter end number (default 10): "
read end
end=${end:-10}

if [ "$start" -gt "$end" ]; then
    echo "Error: Start cannot be greater than end"
    exit 1
fi

print_multiplication_table $start $end

echo
echo -n "Save to file? (y/n): "
read save_choice

if [ "$save_choice" = "y" ] || [ "$save_choice" = "Y" ]; then
    filename="multiplication_table_${start}_to_${end}.txt"
    print_multiplication_table $start $end > "$filename"
    echo "Table saved to: $filename"
fi
