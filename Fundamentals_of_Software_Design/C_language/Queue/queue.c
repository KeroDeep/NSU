#include <stdlib.h>

#include "queue.h"

Queue* queue_create() {
    Queue* queue = (Queue*)malloc(sizeof(Queue));
    if (!queue) return NULL;
    queue->front = NULL;
    queue->rear = NULL;
    queue->size = 0;
    return queue;
}

void queue_free(Queue* queue) {
    if (!queue) return;
    queue_clear(queue);
    free(queue);
}

void queue_enqueue(Queue* queue, Pointer data) {
    if (!queue) return;
    QueueNode* new_node = (QueueNode*)malloc(sizeof(QueueNode));
    if (!new_node) return;
    new_node->data = data;
    new_node->next = NULL;
    if (queue->rear) {
        queue->rear->next = new_node;
    }
    else {
        queue->front = new_node;
    }
    queue->rear = new_node;
    queue->size++;
}

Pointer queue_dequeue(Queue* queue) {
    if (!queue || !queue->front) return NULL;
    QueueNode* temp = queue->front;
    Pointer data = temp->data;
    queue->front = queue->front->next;
    if (!queue->front) {
        queue->rear = NULL;
    }
    free(temp);
    queue->size--;
    return data;
}

Pointer queue_peek(Queue* queue) {
    if (!queue || !queue->front) return NULL;
    return queue->front->data;
}

int queue_empty(Queue* queue) {
    return !queue || queue->size == 0;
}

size_t queue_size(Queue* queue) {
    return queue ? queue->size : 0;
}

void queue_clear(Queue* queue) {
    if (!queue) return;
    while (queue->front) {
        QueueNode* temp = queue->front;
        queue->front = queue->front->next;
        free(temp);
    }
    queue->rear = NULL;
    queue->size = 0;
}
