#!/bin/bash

echo -n "Enter your score (0-100): "
read score

if ! [[ "$score" =~ ^[0-9]+$ ]] || [ "$score" -lt 0 ] || [ "$score" -gt 100 ]; then
    echo "Error: Score must be between 0 and 100"
    exit 1
fi

case $score in
    9[0-9]|100)
        grade="A"
        description="Excellent"
        ;;
    8[0-9])
        grade="B"
        description="Very Good"
        ;;
    7[0-9])
        grade="C"
        description="Good"
        ;;
    6[0-9])
        grade="D"
        description="Satisfactory"
        ;;
    *)
        grade="F"
        description="Fail"
        ;;
esac

echo "Grade: $grade"
echo "Description: $description"

if [ "$grade" = "A" ]; then
    echo "Outstanding performance!"
elif [ "$grade" = "F" ]; then
    echo "Need to improve."
fi
