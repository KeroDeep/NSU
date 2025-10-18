#!/bin/bash

mkdir -p test_project
cd test_project

for i in {1..5}; do
    filename="file_${i}.txt"
    echo "Created: $(date)" > "$filename"
    echo "Created file: $filename"
done

echo
echo "=== File List with Sizes ==="
ls -la *.txt

echo
echo "=== File Contents ==="
for file in *.txt; do
    echo "--- $file ---"
    cat "$file"
done

cd ..
echo
echo "Cleaning up..."
rm -rf test_project
echo "Directory removed"
