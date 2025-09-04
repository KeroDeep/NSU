.data
    prompt1: .asciz "Enter the number of elements (N): "
    prompt2: .asciz "Enter the numbers: "
    msg:     .asciz "Maximum: "
    newline: .asciz "\n"

.bss
    N:   .skip 4
    num: .skip 4
    max: .skip 4

.text
    .globl _start

_start:
    li a7, 64
    li a0, 1
    la a1, prompt1
    li a2, 30
    ecall

    li a7, 63
    li a0, 0
    la a1, N
    li a2, 4
    ecall

    lb t0, N
    addi t0, t0, -48
    mv t1, t0

    li t2, 0
    sw t2, max

input_loop:
    li a7, 64
    li a0, 1
    la a1, prompt2
    li a2, 15
    ecall

    li a7, 63
    li a0, 0
    la a1, num
    li a2, 4
    ecall

    lb t3, num
    addi t3, t3, -48

    lw t4, max
    ble t3, t4, skip_update
    sw t3, max

skip_update:
    addi t1, t1, -1
    bnez t1, input_loop

    li a7, 64
    li a0, 1
    la a1, msg
    li a2, 9
    ecall

    lw t5, max
    addi t5, t5, 48
    sb t5, max

    li a7, 64
    li a0, 1
    la a1, max
    li a2, 1
    ecall

    li a7, 64
    li a0, 1
    la a1, newline
    li a2, 1
    ecall

    li a7, 93
    li a0, 0
    ecall
