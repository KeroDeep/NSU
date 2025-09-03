#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "queue.h"

void test_create_free() {
    Queue* queue = queue_create();
    assert(queue != NULL);
    assert(queue_empty(queue));
    assert(queue_size(queue) == 0);
    queue_free(queue);
    printf("Test create/free passed\n");
}

void test_enqueue_dequeue() {
    Queue* queue = queue_create();
    int value1 = 10;
    int value2 = 20;
    int value3 = 30;
    queue_enqueue(queue, &value1);
    assert(!queue_empty(queue));
    assert(queue_size(queue) == 1);
    queue_enqueue(queue, &value2);
    queue_enqueue(queue, &value3);
    assert(queue_size(queue) == 3);
    int* dequeued = (int*)queue_dequeue(queue);
    assert(*dequeued == 10);
    assert(queue_size(queue) == 2);
    dequeued = (int*)queue_dequeue(queue);
    assert(*dequeued == 20);
    dequeued = (int*)queue_dequeue(queue);
    assert(*dequeued == 30);
    assert(queue_empty(queue));
    assert(queue_dequeue(queue) == NULL);
    queue_free(queue);
    printf("Test enqueue/dequeue passed\n");
}

void test_peek() {
    Queue* queue = queue_create();
    int value1 = 100;
    int value2 = 200;
    queue_enqueue(queue, &value1);
    int* peeked = (int*)queue_peek(queue);
    assert(*peeked == 100);
    assert(queue_size(queue) == 1);
    queue_enqueue(queue, &value2);
    peeked = (int*)queue_peek(queue);
    assert(*peeked == 100);
    queue_dequeue(queue);
    peeked = (int*)queue_peek(queue);
    assert(*peeked == 200);
    queue_free(queue);
    printf("Test peek passed\n");
}

void test_empty_peek_dequeue() {
    Queue* queue = queue_create();
    assert(queue_peek(queue) == NULL);
    assert(queue_dequeue(queue) == NULL);
    assert(queue_empty(queue));
    queue_free(queue);
    printf("Test empty operations passed\n");
}

void test_clear() {
    Queue* queue = queue_create();
    int values[5] = {1, 2, 3, 4, 5};
    for (int i = 0; i < 5; i++) {
        queue_enqueue(queue, &values[i]);
    }
    assert(queue_size(queue) == 5);
    queue_clear(queue);
    assert(queue_empty(queue));
    assert(queue_size(queue) == 0);
    queue_free(queue);
    printf("Test clear passed\n");
}

void test_fifo_order() {
    Queue* queue = queue_create();
    int values[100];
    for (int i = 0; i < 100; i++) {
        values[i] = i;
        queue_enqueue(queue, &values[i]);
    }
    assert(queue_size(queue) == 100);
    for (int i = 0; i < 100; i++) {
        int* value = (int*)queue_dequeue(queue);
        assert(*value == i);
    }
    assert(queue_empty(queue));
    queue_free(queue);
    printf("Test FIFO order passed\n");
}

void test_mixed_operations() {
    Queue* queue = queue_create();
    char* str1 = "hello";
    char* str2 = "world";
    char* str3 = "queue";
    queue_enqueue(queue, str1);
    queue_enqueue(queue, str2);
    assert(strcmp(queue_peek(queue), "hello") == 0);
    queue_enqueue(queue, str3);
    assert(strcmp(queue_dequeue(queue), "hello") == 0);
    assert(strcmp(queue_peek(queue), "world") == 0);
    assert(strcmp(queue_dequeue(queue), "world") == 0);
    assert(strcmp(queue_dequeue(queue), "queue") == 0);
    queue_free(queue);
    printf("Test mixed operations passed\n");
}

void test_repeated_operations() {
    Queue* queue = queue_create();
    for (int i = 0; i < 1000; i++) {
        int* value = malloc(sizeof(int));
        *value = i;
        queue_enqueue(queue, value);
    }
    assert(queue_size(queue) == 1000);
    for (int i = 0; i < 1000; i++) {
        int* value = (int*)queue_dequeue(queue);
        assert(*value == i);
        free(value);
    }
    assert(queue_empty(queue));
    queue_free(queue);
    printf("Test repeated operations passed\n");
}

int main() {
    test_create_free();
    test_enqueue_dequeue();
    test_peek();
    test_empty_peek_dequeue();
    test_clear();
    test_fifo_order();
    test_mixed_operations();
    test_repeated_operations();
    printf("All Queue tests passed successfully!\n");
    return 0;
}
