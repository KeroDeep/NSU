#ifndef QUEUE_H
#define QUEUE_H

typedef void* Pointer;

typedef struct _QueueNode {
    Pointer data;
    struct _QueueNode* next;
} QueueNode;

typedef struct _Queue {
    QueueNode* front;
    QueueNode* rear;
    size_t size;
} Queue;

Queue* queue_create();

void queue_free(Queue* queue);

void queue_enqueue(Queue* queue, Pointer data);

Pointer queue_dequeue(Queue* queue);

Pointer queue_peek(Queue* queue);

int queue_empty(Queue* queue);

size_t queue_size(Queue* queue);

void queue_clear(Queue* queue);

#endif
