.data
    prompt1: .asciz "Enter A: "
    prompt2: .asciz "Enter B: "
    result_msg: .asciz "Product: "
    A: .skip 4
    B: .skip 4
    product: .skip 4
    newline: .asciz "\n"

.text
    .globl _start

_start:
    li a7, 64
    li a0, 1
    la a1, prompt1
    li a2, 9
    ecall

    li a7, 63
    li a0, 0
    la a1, A
    li a2, 4
    ecall

    li a7, 64
    li a0, 1
    la a1, prompt2
    li a2, 9
    ecall

    li a7, 63
    li a0, 0
    la a1, B
    li a2, 4
    ecall

    lb t0, A
    addi t0, t0, -48
    lb t1, B
    addi t1, t1, -48
    beqz t1, zero_case

    li t2, 0
mul_loop:
    andi t3, t1, 1
    beqz t3, skip_add
    add t2, t2, t0
skip_add:
    slli t0, t0, 1
    srli t1, t1, 1
    bnez t1, mul_loop
    sw t2, product
    j print_result

zero_case:
    sw x0, product

print_result:
    li a7, 64
    li a0, 1
    la a1, result_msg
    li a2, 9
    ecall

    lw t4, product
    addi t4, t4, 48
    sb t4, product

    li a7, 64
    li a0, 1
    la a1, product
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
