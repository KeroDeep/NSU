INITIAL_CAPACITY = 16

class Stack:
    __slots__ = ('data', 'size', 'capacity')
    def __init__(self):
        self.data = [None] * INITIAL_CAPACITY
        self.size = 0
        self.capacity = INITIAL_CAPACITY

def stack_create():
    return Stack()

def stack_free(stack):
    stack.data = None
    stack.size = 0
    stack.capacity = 0

def stack_push(stack, value):
    if stack.size >= stack.capacity:
        new_capacity = stack.capacity * 2
        new_data = [None] * new_capacity
        for i in range(stack.size):
            new_data[i] = stack.data[i]
        stack.data = new_data
        stack.capacity = new_capacity
    stack.data[stack.size] = value
    stack.size += 1

def stack_pop(stack):
    if stack.size == 0:
        return None
    stack.size -= 1
    return stack.data[stack.size]

def stack_peek(stack):
    if stack.size == 0:
        return None
    return stack.data[stack.size - 1]

def stack_empty(stack):
    return stack.size == 0

def stack_size(stack):
    return stack.size

def stack_clear(stack):
    stack.size = 0
