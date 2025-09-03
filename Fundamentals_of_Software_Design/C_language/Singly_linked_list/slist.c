#include <stdlib.h>

#include "slist.h"

size_t slist_length(SList lst) {
    size_t count = 0;
    SListNode* current = lst;
    while (current) {
        count++;
        current = current->next;
    }
    return count;
}

SList slist_prepend(SList lst, Pointer data) {
    SListNode* new_node = (SListNode*)malloc(sizeof(SListNode));
    if (!new_node) return lst;
    new_node->data = data;
    new_node->next = lst;
    return new_node;
}

Pointer slist_get(SList lst, size_t index) {
    SListNode* current = lst;
    size_t i = 0;
    while (current && i < index) {
        current = current->next;
        i++;
    }
    return current ? current->data : NULL;
}

SList slist_remove(SList lst, size_t index, Pointer* removed_data) {
    if (!lst) return NULL;
    if (index == 0) {
        SListNode* to_remove = lst;
        if (removed_data) *removed_data = to_remove->data;
        SList new_head = lst->next;
        free(to_remove);
        return new_head;
    }
    SListNode* current = lst;
    size_t i = 0;
    while (current->next && i < index - 1) {
        current = current->next;
        i++;
    }
    if (!current->next) return lst;
    SListNode* to_remove = current->next;
    if (removed_data) *removed_data = to_remove->data;
    current->next = to_remove->next;
    free(to_remove);
    return lst;
}

SList slist_append(SList lst, Pointer data) {
    SListNode* new_node = (SListNode*)malloc(sizeof(SListNode));
    if (!new_node) return lst;
    new_node->data = data;
    new_node->next = NULL;
    if (!lst) return new_node;
    SListNode* current = lst;
    while (current->next) {
        current = current->next;
    }
    current->next = new_node;
    return lst;
}

Pointer slist_get_last(SList lst) {
    if (!lst) return NULL;
    SListNode* current = lst;
    while (current->next) {
        current = current->next;
    }
    return current->data;
}

int slist_find(SList lst, Pointer data) {
    SListNode* current = lst;
    int index = 0;
    while (current) {
        if (current->data == data) {
            return index;
        }
        current = current->next;
        index++;
    }
    return -1;
}

SList slist_remove_first(SList lst, Pointer data) {
    if (!lst) return NULL;
    if (lst->data == data) {
        SListNode* to_remove = lst;
        SList new_head = lst->next;
        free(to_remove);
        return new_head;
    }
    SListNode* current = lst;
    while (current->next) {
        if (current->next->data == data) {
            SListNode* to_remove = current->next;
            current->next = to_remove->next;
            free(to_remove);
            return lst;
        }
        current = current->next;
    }
    return lst;
}

SList slist_remove_all(SList lst, Pointer data) {
    SList result = lst;
    while (result && result->data == data) {
        SListNode* to_remove = result;
        result = result->next;
        free(to_remove);
    }
    if (!result) return NULL;
    SListNode* current = result;
    while (current->next) {
        if (current->next->data == data) {
            SListNode* to_remove = current->next;
            current->next = to_remove->next;
            free(to_remove);
        }
        else {
            current = current->next;
        }
    }
    return result;
}

SList slist_copy(SList lst) {
    if (!lst) return NULL;
    SList new_list = NULL;
    SListNode* current = lst;
    SListNode** last_ptr = &new_list;
    while (current) {
        SListNode* new_node = (SListNode*)malloc(sizeof(SListNode));
        if (!new_node) {
            while (new_list) {
                SListNode* temp = new_list;
                new_list = new_list->next;
                free(temp);
            }
            return NULL;
        }
        new_node->data = current->data;
        new_node->next = NULL;
        *last_ptr = new_node;
        last_ptr = &new_node->next;
        current = current->next;
    }
    return new_list;
}

SList slist_concat(SList lst1, SList lst2) {
    if (!lst1) return slist_copy(lst2);
    if (!lst2) return slist_copy(lst1);
    SList result = slist_copy(lst1);
    if (!result) return NULL;
    SListNode* current = result;
    while (current->next) {
        current = current->next;
    }
    SList copy2 = slist_copy(lst2);
    if (!copy2) {
        while (result) {
            SListNode* temp = result;
            result = result->next;
            free(temp);
        }
        return NULL;
    }
    current->next = copy2;
    return result;
}

void slist_foreach(SList lst, void (*func)(Pointer data)) {
    SListNode* current = lst;
    while (current) {
        func(current->data);
        current = current->next;
    }
}

SList slist_find_custom(SList lst, int (*predicate)(Pointer data), size_t* found_index) {
    SListNode* current = lst;
    size_t index = 0;
    while (current) {
        if (predicate(current->data)) {
            if (found_index) *found_index = index;
            return current;
        }
        current = current->next;
        index++;
    }
    if (found_index) *found_index = (size_t)-1;
    return NULL;
}
