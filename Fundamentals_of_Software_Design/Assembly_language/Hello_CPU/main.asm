.data
    message: .asciz "Hello CPU!"

.text
    .globl _start

_start:
    li a7, 64          # syscall write
    li a0, 1           # stdout
    la a1, message     # адрес строки
    li a2, 11          # длина строки "Hello CPU!"
    ecall

    li a7, 93          # syscall exit
    li a0, 0           # код возврата
    ecall
