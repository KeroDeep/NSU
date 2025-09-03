#include <stdlib.h>

#include "stack.h"

#define INITIAL_CAPACITY 16

Stack* stack_create() {
    Stack* stack = (Stack*)malloc(sizeof(Stack));
    if (!stack) return NULL;
    stack->data = (Pointer*)malloc(INITIAL_CAPACITY * sizeof(Pointer));
    if (!stack->data) {
        free(stack);
        return NULL;
    }
    stack->size = 0;
    stack->capacity = INITIAL_CAPACITY;
    return stack;
}

void stack_free(Stack* stack) {
    if (!stack) return;
    free(stack->data);
    free(stack);
}

void stack_push(Stack* stack, Pointer value) {
    if (!stack) return;
    if (stack->size >= stack->capacity) {
        size_t new_capacity = stack->capacity * 2;
        Pointer* new_data = (Pointer*)realloc(stack->data, new_capacity * sizeof(Pointer));
        if (!new_data) return;
        stack->data = new_data;
        stack->capacity = new_capacity;
    }
    stack->data[stack->size++] = value;
}

Pointer stack_pop(Stack* stack) {
    if (!stack || stack->size == 0) return NULL;
    return stack->data[--stack->size];
}

Pointer stack_peek(Stack* stack) {
    if (!stack || stack->size == 0) return NULL;
    return stack->data[stack->size - 1];
}

int stack_empty(Stack* stack) {
    return !stack || stack->size == 0;
}

size_t stack_size(Stack* stack) {
    return stack ? stack->size : 0;
}

void stack_clear(Stack* stack) {
    if (stack) stack->size = 0;
}
