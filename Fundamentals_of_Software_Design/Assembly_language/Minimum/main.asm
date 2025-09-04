.data
    prompt: .asciz "Enter three numbers: "
    msg: .asciz "The minimum is: "
    num1: .skip 4
    num2: .skip 4
    num3: .skip 4
    min:  .skip 4
    newline: .asciz "\n"

.text
    .globl _start

_start:
    li a7, 64
    li a0, 1
    la a1, prompt
    li a2, 20
    ecall

    li a7, 63
    li a0, 0
    la a1, num1
    li a2, 4
    ecall

    li a7, 63
    li a0, 0
    la a1, num2
    li a2, 4
    ecall

    li a7, 63
    li a0, 0
    la a1, num3
    li a2, 4
    ecall

    lb t0, num1
    addi t0, t0, -48
    lb t1, num2
    addi t1, t1, -48
    lb t2, num3
    addi t2, t2, -48

    ble t0, t1, keep_t0
    mv t0, t1
keep_t0:
    ble t0, t2, keep_t0_again
    mv t0, t2
keep_t0_again:
    sw t0, min

    li a7, 64
    li a0, 1
    la a1, msg
    li a2, 18
    ecall

    lw t3, min
    addi t3, t3, 48
    sb t3, min

    li a7, 64
    li a0, 1
    la a1, min
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
