#include <assert.h>
#include <stdio.h>
#include <stdlib.h>

#include "stack.h"

void test_create_free() {
    Stack* stack = stack_create();
    assert(stack != NULL);
    assert(stack_empty(stack));
    assert(stack_size(stack) == 0);
    stack_free(stack);
    printf("Test create/free passed\n");
}

void test_push_pop() {
    Stack* stack = stack_create();
    int value1 = 10;
    int value2 = 20;
    int value3 = 30;
    stack_push(stack, &value1);
    assert(!stack_empty(stack));
    assert(stack_size(stack) == 1);
    stack_push(stack, &value2);
    stack_push(stack, &value3);
    assert(stack_size(stack) == 3);
    int* popped = (int*)stack_pop(stack);
    assert(*popped == 30);
    assert(stack_size(stack) == 2);
    popped = (int*)stack_pop(stack);
    assert(*popped == 20);
    popped = (int*)stack_pop(stack);
    assert(*popped == 10);
    assert(stack_empty(stack));
    assert(stack_pop(stack) == NULL);
    stack_free(stack);
    printf("Test push/pop passed\n");
}

void test_peek() {
    Stack* stack = stack_create();
    int value1 = 100;
    int value2 = 200;
    stack_push(stack, &value1);
    int* peeked = (int*)stack_peek(stack);
    assert(*peeked == 100);
    assert(stack_size(stack) == 1);
    stack_push(stack, &value2);
    peeked = (int*)stack_peek(stack);
    assert(*peeked == 200);
    stack_pop(stack);
    peeked = (int*)stack_peek(stack);
    assert(*peeked == 100);
    stack_free(stack);
    printf("Test peek passed\n");
}

void test_empty_peek_pop() {
    Stack* stack = stack_create();
    assert(stack_peek(stack) == NULL);
    assert(stack_pop(stack) == NULL);
    assert(stack_empty(stack));
    stack_free(stack);
    printf("Test empty operations passed\n");
}

void test_clear() {
    Stack* stack = stack_create();
    int values[5] = {1, 2, 3, 4, 5};
    for (int i = 0; i < 5; i++) {
        stack_push(stack, &values[i]);
    }
    assert(stack_size(stack) == 5);
    stack_clear(stack);
    assert(stack_empty(stack));
    assert(stack_size(stack) == 0);
    stack_free(stack);
    printf("Test clear passed\n");
}

void test_resize() {
    Stack* stack = stack_create();
    int values[100];
    for (int i = 0; i < 100; i++) {
        values[i] = i;
        stack_push(stack, &values[i]);
    }
    assert(stack_size(stack) == 100);
    assert(stack->capacity >= 100);
    for (int i = 99; i >= 0; i--) {
        int* value = (int*)stack_pop(stack);
        assert(*value == i);
    }
    assert(stack_empty(stack));
    stack_free(stack);
    printf("Test resize passed\n");
}

void test_mixed_operations() {
    Stack* stack = stack_create();
    char* str1 = "hello";
    char* str2 = "world";
    char* str3 = "stack";
    stack_push(stack, str1);
    stack_push(stack, str2);
    assert(strcmp(stack_peek(stack), "world") == 0);
    stack_push(stack, str3);
    assert(strcmp(stack_pop(stack), "stack") == 0);
    assert(strcmp(stack_peek(stack), "world") == 0);
    assert(strcmp(stack_pop(stack), "world") == 0);
    assert(strcmp(stack_pop(stack), "hello") == 0);
    stack_free(stack);
    printf("Test mixed operations passed\n");
}

int main() {
    test_create_free();
    test_push_pop();
    test_peek();
    test_empty_peek_pop();
    test_clear();
    test_resize();
    test_mixed_operations();
    printf("All Stack tests passed successfully!\n");
    return 0;
}
