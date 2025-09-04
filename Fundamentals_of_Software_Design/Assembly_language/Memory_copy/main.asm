.text
    .globl memcpy

memcpy:
    mv t0, a0
    mv t1, a1
    mv t2, a2
    bge t0, t1, reverse_copy

forward_copy:
    beqz t2, done
forward_loop:
    lb t3, 0(t1)
    sb t3, 0(t0)
    addi t1, t1, 1
    addi t0, t0, 1
    addi t2, t2, -1
    bnez t2, forward_loop
    j done

reverse_copy:
    add t1, t1, t2
    add t0, t0, t2
    beqz t2, done
reverse_loop:
    addi t1, t1, -1
    addi t0, t0, -1
    lb t3, 0(t1)
    sb t3, 0(t0)
    addi t2, t2, -1
    bnez t2, reverse_loop

done:
    ret
