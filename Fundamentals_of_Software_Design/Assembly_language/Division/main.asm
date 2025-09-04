# RISC-V Ассемблер (синтаксис GNU as)
.data
    prompt1: .asciz "Enter A: "
    prompt2: .asciz "Enter B: "
    quotient_msg: .asciz "Quotient: "
    remainder_msg: .asciz "Remainder: "
    error_msg: .asciz "Error: division by zero!"
    newline: .asciz "\n"

.bss
    A: .skip 4          # переменная для A
    B: .skip 4          # переменная для B
    quotient: .skip 4   # переменная для частного
    remainder: .skip 4  # переменная для остатка

.text
    .globl _start

_start:
    # === ВВОД A ===
    li a7, 64           # syscall write
    li a0, 1            # stdout
    la a1, prompt1      # адрес строки
    li a2, 9            # длина "Enter A: "
    ecall

    li a7, 63           # syscall read
    li a0, 0            # stdin
    la a1, A            # буфер для A
    li a2, 4            # читаем 4 байта
    ecall

    # === ВВОД B ===
    li a7, 64
    li a0, 1
    la a1, prompt2
    li a2, 9            # длина "Enter B: "
    ecall

    li a7, 63
    li a0, 0
    la a1, B            # буфер для B
    li a2, 4
    ecall

    # === ПРЕОБРАЗОВАНИЕ ASCII В ЧИСЛА ===
    lb t0, A            # загружаем первый байт A
    addi t0, t0, -48    # преобразуем ASCII в число (A_value)
    
    lb t1, B            # загружаем первый байт B
    addi t1, t1, -48    # преобразуем ASCII в число (B_value)

    # === ПРОВЕРКА ДЕЛЕНИЯ НА НОЛЬ ===
    beqz t1, zero_division  # если B == 0, ошибка

    # === ДЕЛЕНИЕ В ЦИКЛЕ ===
    li t2, 0            # quotient = 0 (аналог xor ecx, ecx)
    mv t3, t0           # remainder = A (аналог mov edx, eax)

loop:
    blt t3, t1, done    # если remainder < B, выходим (аналог cmp+jl)
    
    sub t3, t3, t1      # remainder -= B (аналог sub edx, ebx)
    addi t2, t2, 1      # quotient++ (аналог inc ecx)
    
    j loop              # продолжаем цикл

done:
    # === СОХРАНЕНИЕ РЕЗУЛЬТАТА ===
    sw t2, quotient     # сохраняем частное
    sw t3, remainder    # сохраняем остаток

    # === ВЫВОД ЧАСТНОГО ===
    li a7, 64
    li a0, 1
    la a1, quotient_msg
    li a2, 10           # длина "Quotient: "
    ecall

    # Преобразуем частное в ASCII
    lw t0, quotient
    addi t0, t0, 48     # число в ASCII
    sb t0, quotient     # сохраняем ASCII-символ

    li a7, 64
    li a0, 1
    la a1, quotient     # адрес символа частного
    li a2, 1            # выводим 1 символ
    ecall

    # Выводим перевод строки
    li a7, 64
    li a0, 1
    la a1, newline
    li a2, 1
    ecall

    # === ВЫВОД ОСТАТКА ===
    li a7, 64
    li a0, 1
    la a1, remainder_msg
    li a2, 11           # длина "Remainder: "
    ecall

    # Преобразуем остаток в ASCII
    lw t0, remainder
    addi t0, t0, 48     # число в ASCII
    sb t0, remainder    # сохраняем ASCII-символ

    li a7, 64
    li a0, 1
    la a1, remainder    # адрес символа остатка
    li a2, 1            # выводим 1 символ
    ecall

    # Выводим перевод строки
    li a7, 64
    li a0, 1
    la a1, newline
    li a2, 1
    ecall

    j exit              # переходим к завершению

zero_division:
    # === ОБРАБОТКА ОШИБКИ ДЕЛЕНИЯ НА НОЛЬ ===
    li a7, 64
    li a0, 1
    la a1, error_msg
    li a2, 24           # длина "Error: division by zero!"
    ecall

    # Выводим перевод строки
    li a7, 64
    li a0, 1
    la a1, newline
    li a2, 1
    ecall

exit:
    # === ЗАВЕРШЕНИЕ ПРОГРАММЫ ===
    li a7, 93           # syscall exit
    li a0, 0            # код возврата 0
    ecall
