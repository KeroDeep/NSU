.text
    .globl is_power_of_two

is_power_of_two:
    beqz a0, not_power
    addi t0, a0, -1
    and a0, a0, t0
    beqz a0, is_power
not_power:
    li a0, 0
    ret
is_power:
    li a0, 1
    ret
