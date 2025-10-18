#!/bin/bash

generate_password() {
    local length=$1
    local chars='abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*'
    local password=''
    
    for (( i=0; i<length; i++ )); do
        password+=${chars:$((RANDOM % ${#chars})):1}
    done
    
    echo "$password"
}

echo -n "Enter password length (minimum 8): "
read length

if [ -z "$length" ] || [ "$length" -lt 8 ]; then
    echo "Using minimum length: 8"
    length=8
fi

password=$(generate_password $length)
timestamp=$(date +%Y%m%d_%H%M%S)
filename="password_${timestamp}.txt"

echo "Generated password: $password"
echo "$password" > "$filename"
echo "Password saved to: $filename"
