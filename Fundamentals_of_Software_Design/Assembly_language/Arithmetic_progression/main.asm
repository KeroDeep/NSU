.data
    prompt1:    .asciz "Enter a1: "
    prompt2:    .asciz "Enter d: "
    prompt3:    .asciz "Enter N: "
    result_msg: .asciz "Sum: "

.bss
    a1:   .skip 4
    d:    .skip 4
    N:    .skip 4
    sum:  .skip 4

.text
    .globl _start

_start:
    li a7, 64
    li a0, 1
    la a1, prompt1
    li a2, 10
    ecall

    li a7, 63
    li a0, 0
    la a1, a1
    li a2, 4
    ecall

    li a7, 64
    li a0, 1
    la a1, prompt2
    li a2, 9
    ecall

    li a7, 63
    li a0, 0
    la a1, d
    li a2, 4
    ecall

    li a7, 64
    li a0, 1
    la a1, prompt3
    li a2, 10
    ecall

    li a7, 63
    li a0, 0
    la a1, N
    li a2, 4
    ecall

    lb t0, a1
    addi t0, t0, -48
    sw t0, sum

    lb t1, N
    addi t1, t1, -48
    addi t1, t1, -1

    lb t2, d
    addi t2, t2, -48

    mv t3, t0
    mv t4, t0

loop:
    beqz t1, done
    add t3, t3, t2
    add t4, t4, t3
    addi t1, t1, -1
    j loop

done:
    sw t4, sum

    li a7, 64
    li a0, 1
    la a1, result_msg
    li a2, 5
    ecall

    lw t0, sum
    addi t0, t0, 48
    sb t0, sum

    li a7, 64
    li a0, 1
    la a1, sum
    li a2, 1
    ecall

    li a7, 93
    li a0, 0
    ecall
