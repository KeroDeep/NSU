#ifndef DLIST_H
#define DLIST_H

typedef void* Pointer;

typedef struct _DListNode {
    Pointer data;
    struct _DListNode* prev;
    struct _DListNode* next;
} DListNode;

typedef struct {
    DListNode* head;
    DListNode* tail;
    size_t size;
} DList;

void dlist_create(DList* dl);

void dlist_destroy(DList* dl);

void dlist_prepend(DList* dl, Pointer data);

void dlist_append(DList* dl, Pointer data);

Pointer dlist_nth(DList* dl, size_t n);

size_t dlist_size(DList* dl);

int dlist_empty(DList* dl);

void dlist_remove(DList* dl, size_t n);

void dlist_insert(DList* dl, size_t n, Pointer data);

Pointer dlist_first(DList* dl);

Pointer dlist_last(DList* dl);

void dlist_clear(DList* dl);

#endif
