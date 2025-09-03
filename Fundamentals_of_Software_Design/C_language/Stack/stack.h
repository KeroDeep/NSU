#ifndef STACK_H
#define STACK_H

typedef void* Pointer;

typedef struct _Stack {
    Pointer* data;
    size_t size;
    size_t capacity;
} Stack;

Stack* stack_create();

void stack_free(Stack* stack);

void stack_push(Stack* stack, Pointer value);

Pointer stack_pop(Stack* stack);

Pointer stack_peek(Stack* stack);

int stack_empty(Stack* stack);

size_t stack_size(Stack* stack);

void stack_clear(Stack* stack);

#endif
