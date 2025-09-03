#include <stdlib.h>

#include "dlist.h"

void dlist_create(DList* dl) {
    dl->head = NULL;
    dl->tail = NULL;
    dl->size = 0;
}

void dlist_destroy(DList* dl) {
    dlist_clear(dl);
}

void dlist_prepend(DList* dl, Pointer data) {
    DListNode* new_node = (DListNode*)malloc(sizeof(DListNode));
    if (!new_node) return;
    new_node->data = data;
    new_node->prev = NULL;
    new_node->next = dl->head;
    if (dl->head) {
        dl->head->prev = new_node;
    }
    else {
        dl->tail = new_node;
    }
    dl->head = new_node;
    dl->size++;
}

void dlist_append(DList* dl, Pointer data) {
    DListNode* new_node = (DListNode*)malloc(sizeof(DListNode));
    if (!new_node) return;
    new_node->data = data;
    new_node->next = NULL;
    new_node->prev = dl->tail;
    if (dl->tail) {
        dl->tail->next = new_node;
    }
    else {
        dl->head = new_node;
    }
    dl->tail = new_node;
    dl->size++;
}

Pointer dlist_nth(DList* dl, size_t n) {
    if (n >= dl->size) return NULL;
    DListNode* current;
    if (n < dl->size / 2) {
        current = dl->head;
        for (size_t i = 0; i < n; i++) {
            current = current->next;
        }
    }
    else {
        current = dl->tail;
        for (size_t i = dl->size - 1; i > n; i--) {
            current = current->prev;
        }
    }
    return current->data;
}

size_t dlist_size(DList* dl) {
    return dl->size;
}

int dlist_empty(DList* dl) {
    return dl->size == 0;
}

void dlist_remove(DList* dl, size_t n) {
    if (n >= dl->size) return;
    DListNode* to_remove;
    if (n < dl->size / 2) {
        to_remove = dl->head;
        for (size_t i = 0; i < n; i++) {
            to_remove = to_remove->next;
        }
    }
    else {
        to_remove = dl->tail;
        for (size_t i = dl->size - 1; i > n; i--) {
            to_remove = to_remove->prev;
        }
    }
    if (to_remove->prev) {
        to_remove->prev->next = to_remove->next;
    }
    else {
        dl->head = to_remove->next;
    }
    if (to_remove->next) {
        to_remove->next->prev = to_remove->prev;
    }
    else {
        dl->tail = to_remove->prev;
    }
    free(to_remove);
    dl->size--;
}

void dlist_insert(DList* dl, size_t n, Pointer data) {
    if (n > dl->size) return;
    if (n == 0) {
        dlist_prepend(dl, data);
        return;
    }
    if (n == dl->size) {
        dlist_append(dl, data);
        return;
    }
    DListNode* current;
    if (n < dl->size / 2) {
        current = dl->head;
        for (size_t i = 0; i < n; i++) {
            current = current->next;
        }
    }
    else {
        current = dl->tail;
        for (size_t i = dl->size - 1; i > n; i--) {
            current = current->prev;
        }
    }
    DListNode* new_node = (DListNode*)malloc(sizeof(DListNode));
    if (!new_node) return;
    new_node->data = data;
    new_node->prev = current->prev;
    new_node->next = current;
    current->prev->next = new_node;
    current->prev = new_node;
    dl->size++;
}

Pointer dlist_first(DList* dl) {
    return dl->head ? dl->head->data : NULL;
}

Pointer dlist_last(DList* dl) {
    return dl->tail ? dl->tail->data : NULL;
}

void dlist_clear(DList* dl) {
    DListNode* current = dl->head;
    while (current) {
        DListNode* next = current->next;
        free(current);
        current = next;
    }
    dl->head = NULL;
    dl->tail = NULL;
    dl->size = 0;
}
