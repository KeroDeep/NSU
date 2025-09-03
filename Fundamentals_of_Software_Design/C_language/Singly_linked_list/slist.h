#ifndef SLIST_H
#define SLIST_H

typedef void* Pointer;

typedef struct _SListNode {
    Pointer data;
    struct _SListNode* next;
} SListNode;

typedef SListNode* SList;

size_t slist_length(SList lst);

SList slist_prepend(SList lst, Pointer data);

Pointer slist_get(SList lst, size_t index);

SList slist_remove(SList lst, size_t index, Pointer* removed_data);

SList slist_append(SList lst, Pointer data);

Pointer slist_get_last(SList lst);

int slist_find(SList lst, Pointer data);

SList slist_remove_first(SList lst, Pointer data);

SList slist_remove_all(SList lst, Pointer data);

SList slist_copy(SList lst);

SList slist_concat(SList lst1, SList lst2);

void slist_foreach(SList lst, void (*func)(Pointer data));

SList slist_find_custom(SList lst, int (*predicate)(Pointer data), size_t* found_index);

#endif
