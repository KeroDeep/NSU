#!/bin/bash

echo -n "Enter your name: "
read name

echo -n "Enter your birth year: "
read birth_year

current_year=$(date +%Y)

if ! [[ "$birth_year" =~ ^[0-9]{4}$ ]] || [ "$birth_year" -gt "$current_year" ]; then
    echo "Error: Invalid birth year"
    exit 1
fi

age=$((current_year - birth_year))

echo "Hello, $name! You are $age years old."
