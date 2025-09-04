.data
    newline:   .asciz "\n"
    buffer:    .space 32

.text
    .globl my_printf

my_printf:
    addi sp, sp, -32
    sw ra, 28(sp)
    sw s0, 24(sp)
    addi s0, sp, 32
    mv t0, a0
    addi s1, a1, 0

loop:
    lbu t1, 0(t0)
    beqz t1, done
    addi t0, t0, 1
    li t2, '%'
    beq t1, t2, format
    li a7, 64
    li a0, 1
    addi a1, t0, -1
    li a2, 1
    ecall
    j loop

format:
    lbu t1, 0(t0)
    addi t0, t0, 1
    li t2, '%'
    beq t1, t2, print_char
    li t2, 's'
    beq t1, t2, print_str
    li t2, 'd'
    beq t1, t2, print_int
    j loop

print_char:
    li a7, 64
    li a0, 1
    addi a1, t0, -1
    li a2, 1
    ecall
    j loop

print_str:
    lw a1, 0(s1)
    addi s1, s1, 4
    mv a0, a1
    call strlen
    mv a2, a0
    li a7, 64
    li a0, 1
    ecall
    j loop

print_int:
    lw a0, 0(s1)
    addi s1, s1, 4
    call itoa
    mv a1, a0
    mv a0, a0
    call strlen
    mv a2, a0
    li a7, 64
    li a0, 1
    ecall
    j loop

done:
    lw ra, 28(sp)
    lw s0, 24(sp)
    addi sp, sp, 32
    ret

strlen:
    mv t0, a0
    li t1, 0
strlen_loop:
    lbu t2, 0(t0)
    beqz t2, strlen_done
    addi t1, t1, 1
    addi t0, t0, 1
    j strlen_loop
strlen_done:
    mv a0, t1
    ret

itoa:
    mv t0, a0
    addi t1, zero, 10
    la t2, buffer
    addi t2, t2, 31
    sb zero, 0(t2)
itoa_loop:
    rem t3, t0, t1
    div t0, t0, t1
    addi t3, t3, 48
    addi t2, t2, -1
    sb t3, 0(t2)
    bnez t0, itoa_loop
    mv a0, t2
    ret
