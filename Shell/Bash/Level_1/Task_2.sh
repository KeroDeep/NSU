#!/bin/bash

echo "=== System Information ==="
echo "System info: $(uname -a)"
echo
echo "=== Disk Usage ==="
df -h
echo
echo "=== Memory Usage ==="
free -h
echo
echo "=== Running Processes ==="
ps aux --sort=-%cpu | head -20
