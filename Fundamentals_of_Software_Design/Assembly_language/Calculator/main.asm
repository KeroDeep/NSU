# RISC-V Ассемблер (синтаксис GNU as)
.data
    prompt1: .asciz "Enter the first number: "
    prompt2: .asciz "Enter the second number: "
    prompt3: .asciz "Choose operation (1=Sum, 2=Subtraction, 3=Multiplication, 4=Division, 5=Modulus): "
    msg: .asciz "Result: "
    newline: .asciz "\n"

.bss
    num1: .skip 4      # переменная для первого числа
    num2: .skip 4      # переменная для второго числа
    result: .skip 4    # переменная для результата
    choice: .skip 4    # переменная для выбора операции

.text
    .globl _start

_start:
    # === ВВОД ПЕРВОГО ЧИСЛА ===
    li a7, 64           # syscall write
    li a0, 1            # stdout
    la a1, prompt1      # адрес строки
    li a2, 23           # длина строки
    ecall

    li a7, 63           # syscall read
    li a0, 0            # stdin
    la a1, num1         # буфер для числа
    li a2, 4            # читаем 4 байта
    ecall

    # === ВВОД ВТОРОГО ЧИСЛА ===
    li a7, 64
    li a0, 1
    la a1, prompt2
    li a2, 25           # длина строки
    ecall

    li a7, 63
    li a0, 0
    la a1, num2
    li a2, 4
    ecall

    # === ВЫБОР ОПЕРАЦИИ ===
    li a7, 64
    li a0, 1
    la a1, prompt3
    li a2, 72           # длина строки
    ecall

    li a7, 63
    li a0, 0
    la a1, choice       # сохраняем выбор в отдельную переменную
    li a2, 4
    ecall

    # === ПРЕОБРАЗОВАНИЕ И ВЫБОР ОПЕРАЦИИ ===
    lb t0, choice       # загружаем выбор операции
    addi t0, t0, -48    # преобразуем ASCII в число

    # Загружаем числа и преобразуем их
    lb t1, num1
    addi t1, t1, -48    # num1_value
    lb t2, num2
    addi t2, t2, -48    # num2_value

    # Ветвление по выбору операции
    li t3, 1
    beq t0, t3, sum     # if choice == 1

    li t3, 2
    beq t0, t3, sub     # if choice == 2

    li t3, 3
    beq t0, t3, mul     # if choice == 3

    li t3, 4
    beq t0, t3, div     # if choice == 4

    li t3, 5
    beq t0, t3, mod     # if choice == 5

    j done              # неверный выбор

sum:
    # СЛОЖЕНИЕ
    add t4, t1, t2      # result = num1 + num2
    sw t4, result
    j print_result

sub:
    # ВЫЧИТАНИЕ
    sub t4, t1, t2      # result = num1 - num2
    sw t4, result
    j print_result

mul:
    # УМНОЖЕНИЕ
    mul t4, t1, t2      # result = num1 * num2
    sw t4, result
    j print_result

div:
    # ДЕЛЕНИЕ
    beqz t2, div_error  # проверка деления на ноль
    div t4, t1, t2      # result = num1 / num2
    sw t4, result
    j print_result

mod:
    # ОСТАТОК ОТ ДЕЛЕНИЯ
    beqz t2, div_error  # проверка деления на ноль
    rem t4, t1, t2      # result = num1 % num2
    sw t4, result
    j print_result

div_error:
    # Обработка ошибки деления на ноль
    li t4, -1           # специальное значение для ошибки
    sw t4, result
    j print_result

print_result:
    # === ВЫВОД РЕЗУЛЬТАТА ===
    # Выводим "Result: "
    li a7, 64
    li a0, 1
    la a1, msg
    li a2, 8            # длина "Result: "
    ecall

    # Преобразуем число в ASCII и выводим
    lw t0, result
    addi t0, t0, 48     # преобразуем число в ASCII
    sb t0, result       # сохраняем ASCII-символ

    li a7, 64
    li a0, 1
    la a1, result       # адрес символа результата
    li a2, 1            # выводим 1 символ
    ecall

    # Выводим перевод строки
    li a7, 64
    li a0, 1
    la a1, newline
    li a2, 1
    ecall

done:
    # === ЗАВЕРШЕНИЕ ПРОГРАММЫ ===
    li a7, 93           # syscall exit
    li a0, 0            # код возврата 0
    ecall
