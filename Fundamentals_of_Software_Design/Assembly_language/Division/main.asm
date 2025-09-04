.data
    prompt1:       .asciz "Enter A: "
    prompt2:       .asciz "Enter B: "
    quotient_msg:  .asciz "Quotient: "
    remainder_msg: .asciz "Remainder: "
    error_msg:     .asciz "Error: division by zero!"
    newline:       .asciz "\n"

.bss
    A:         .skip 4
    B:         .skip 4
    quotient:  .skip 4
    remainder: .skip 4

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

    beqz t1, zero_division

    li t2, 0
    mv t3, t0

loop:
    blt t3, t1, done
    sub t3, t3, t1
    addi t2, t2, 1
    j loop

done:
    sw t2, quotient
    sw t3, remainder

    li a7, 64
    li a0, 1
    la a1, quotient_msg
    li a2, 10
    ecall

    lw t0, quotient
    addi t0, t0, 48
    sb t0, quotient

    li a7, 64
    li a0, 1
    la a1, quotient
    li a2, 1
    ecall

    li a7, 64
    li a0, 1
    la a1, newline
    li a2, 1
    ecall

    li a7, 64
    li a0, 1
    la a1, remainder_msg
    li a2, 11
    ecall

    lw t0, remainder
    addi t0, t0, 48
    sb t0, remainder

    li a7, 64
    li a0, 1
    la a1, remainder
    li a2, 1
    ecall

    li a7, 64
    li a0, 1
    la a1, newline
    li a2, 1
    ecall

    j exit

zero_division:
    li a7, 64
    li a0, 1
    la a1, error_msg
    li a2, 24
    ecall

    li a7, 64
    li a0, 1
    la a1, newline
    li a2, 1
    ecall

exit:
    li a7, 93
    li a0, 0
    ecall
