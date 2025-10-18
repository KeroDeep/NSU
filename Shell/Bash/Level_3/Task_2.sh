#!/bin/bash

echo -n "Enter file path: "
read filepath

echo "=== File Report ==="

if [ -e "$filepath" ]; then
    echo "✓ File exists"
else
    echo "✗ File does not exist"
    exit 1
fi

if [ -f "$filepath" ]; then
    echo "✓ Type: Regular file"
elif [ -d "$filepath" ]; then
    echo "✓ Type: Directory"
elif [ -L "$filepath" ]; then
    echo "✓ Type: Symbolic link"
else
    echo "? Type: Other"
fi

echo "--- Permissions ---"
[ -r "$filepath" ] && echo "✓ Readable" || echo "✗ Not readable"
[ -w "$filepath" ] && echo "✓ Writable" || echo "✗ Not writable"
[ -x "$filepath" ] && echo "✓ Executable" || echo "✗ Not executable"

echo "--- Details ---"
echo "Size: $(du -h "$filepath" | cut -f1)"
echo "Last modified: $(stat -c %y "$filepath" 2>/dev/null || stat -f %Sm "$filepath")"
echo "Owner: $(stat -c %U "$filepath" 2>/dev/null || stat -f %Su "$filepath")"
