#!/bin/bash

USD_TO_RUB=90
RUB_TO_USD=0.011

echo "Currency Converter"
echo "1. USD to RUB"
echo "2. RUB to USD"
echo -n "Choose conversion direction (1 or 2): "
read choice

case $choice in
    1)
        echo -n "Enter amount in USD: "
        read usd_amount
        rub_amount=$(echo "$usd_amount * $USD_TO_RUB" | bc)
        echo "$usd_amount USD = $rub_amount RUB"
        ;;
    2)
        echo -n "Enter amount in RUB: "
        read rub_amount
        usd_amount=$(echo "$rub_amount * $RUB_TO_USD" | bc)
        echo "$rub_amount RUB = $usd_amount USD"
        ;;
    *)
        echo "Invalid choice"
        exit 1
        ;;
esac
