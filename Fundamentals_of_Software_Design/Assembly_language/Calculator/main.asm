.data
    prompt1:    .asciz "Enter the first number: "
    prompt2:    .asciz "Enter the second number: "
    prompt3:    .asciz "Choose operation (1=Sum, 2=Subtraction, 3=Multiplication, 4=Division, 5=Modulus): "
    msg:        .asciz "Result: "
    newline:    .asciz "\n"

.bss
    num1:   .skip 4
    num2:   .skip 4
    result: .skip 4
    choice: .skip 4

.text
    .globl _start

_start:
    li a7, 64
    li a0, 1
    la a1, prompt1
    li a2, 23
    ecall

    li a7, 63
    li a0, 0
    la a1, num1
    li a2, 4
    ecall

    li a7, 64
    li a0, 1
    la a1, prompt2
    li a2, 25
    ecall

    li a7, 63
    li a0, 0
    la a1, num2
    li a2, 4
    ecall

    li a7, 64
    li a0, 1
    la a1, prompt3
    li a2, 72
    ecall

    li a7, 63
    li a0, 0
    la a1, choice
    li a2, 4
    ecall

    lb t0, choice
    addi t0, t0, -48

    lb t1, num1
    addi t1, t1, -48
    lb t2, num2
    addi t2, t2, -48

    li t3, 1
    beq t0, t3, sum
    li t3, 2
    beq t0, t3, sub
    li t3, 3
    beq t0, t3, mul
    li t3, 4
    beq t0, t3, div
    li t3, 5
    beq t0, t3, mod
    j done

sum:
    add t4, t1, t2
    sw t4, result
    j print_result

sub:
    sub t4, t1, t2
    sw t4, result
    j print_result

mul:
    mul t4, t1, t2
    sw t4, result
    j print_result

div:
    beqz t2, div_error
    div t4, t1, t2
    sw t4, result
    j print_result

mod:
    beqz t2, div_error
    rem t4, t1, t2
    sw t4, result
    j print_result

div_error:
    li t4, -1
    sw t4, result
    j print_result

print_result:
    li a7, 64
    li a0, 1
    la a1, msg
    li a2, 8
    ecall

    lw t0, result
    addi t0, t0, 48
    sb t0, result

    li a7, 64
    li a0, 1
    la a1, result
    li a2, 1
    ecall

    li a7, 64
    li a0, 1
    la a1, newline
    li a2, 1
    ecall

done:
    li a7, 93
    li a0, 0
    ecall
