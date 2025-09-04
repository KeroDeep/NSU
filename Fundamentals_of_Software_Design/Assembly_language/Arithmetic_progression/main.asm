# RISC-V Ассемблер (синтаксис GNU as)
.data
    prompt1: .asciz "Enter a1: "    # .asciz добавляет нулевой байт
    prompt2: .asciz "Enter d: "
    prompt3: .asciz "Enter N: "
    result_msg: .asciz "Sum: "

.bss
    a1: .skip 4      # Резервируем 4 байта
    d: .skip 4
    N: .skip 4
    sum: .skip 4

.text
    .globl _start

_start:
    # === ВВОД a1 ===
    # Вывод prompt1 (syscall write)
    li a7, 64            # номер syscall 'write' для RISC-V
    li a0, 1             # stdout (fd = 1)
    la a1, prompt1       # адрес строки
    li a2, 10            # длина строки "Enter a1: " (10 символов)
    ecall

    # Чтение a1 (syscall read)
    li a7, 63            # номер syscall 'read'
    li a0, 0             # stdin (fd = 0)
    la a1, a1            # адрес буфера для чтения
    li a2, 4             # читаем до 4 байт
    ecall

    # === ВВОД d ===
    # Вывод prompt2
    li a7, 64
    li a0, 1
    la a1, prompt2
    li a2, 9             # длина "Enter d: " (9 символов)
    ecall

    # Чтение d
    li a7, 63
    li a0, 0
    la a1, d
    li a2, 4
    ecall

    # === ВВОД N ===
    # Вывод prompt3
    li a7, 64
    li a0, 1
    la a1, prompt3
    li a2, 10            # длина "Enter N: " (10 символов)
    ecall

    # Чтение N
    li a7, 63
    li a0, 0
    la a1, N
    li a2, 4
    ecall

    # === ПРЕОБРАЗОВАНИЕ И РАСЧЕТ ===
    # Преобразуем ASCII в числа и загружаем в регистры
    lb t0, a1            # загружаем первый байт из a1
    addi t0, t0, -48     # преобразуем ASCII в число (вычитаем '0')
    sw t0, sum           # sum = a1_value
    
    lb t1, N             # загружаем N
    addi t1, t1, -48     # преобразуем в число
    addi t1, t1, -1      # N = N - 1 (уменьшаем на 1)
    
    lb t2, d             # загружаем d
    addi t2, t2, -48     # преобразуем в число
    
    # Загружаем начальное значение для вычислений
    mv t3, t0            # current = a1_value (аналог mov eax, [a1])
    mv t4, t0            # running_sum = a1_value

loop:
    # Проверяем условие выхода из цикла (N == 0)
    beqz t1, done        # if (t1 == 0) goto done (аналог test+jz)
    
    # Вычисляем следующий элемент прогрессии
    add t3, t3, t2       # current += d (аналог add eax, ecx)
    
    # Добавляем к общей сумме
    add t4, t4, t3       # running_sum += current
    
    # Уменьшаем счетчик
    addi t1, t1, -1      # N-- (аналог dec ebx)
    
    j loop               # продолжаем цикл

done:
    # Сохраняем итоговую сумму
    sw t4, sum           # сохраняем результат в память
    
    # === ВЫВОД РЕЗУЛЬТАТА ===
    # Выводим сообщение "Sum: "
    li a7, 64
    li a0, 1
    la a1, result_msg
    li a2, 5             # длина "Sum: " (5 символов)
    ecall

    # Преобразуем число обратно в ASCII для вывода
    lw t0, sum           # загружаем сумму
    addi t0, t0, 48      # преобразуем число в ASCII
    sb t0, sum           # сохраняем ASCII-символ обратно в память
    
    # Выводим сам символ результата
    li a7, 64
    li a0, 1
    la a1, sum           # адрес символа для вывода
    li a2, 1             # выводим 1 символ
    ecall

    # === ЗАВЕРШЕНИЕ ПРОГРАММЫ ===
    li a7, 93            # номер syscall 'exit'
    li a0, 0             # код возврата 0
    ecall
