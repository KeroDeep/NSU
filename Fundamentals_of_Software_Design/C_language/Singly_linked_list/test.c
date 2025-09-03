#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "slist.h"

void test_print_func(Pointer data) {
    printf("%d ", *(int*)data);
}

int is_even(Pointer data) {
    return (*(int*)data) % 2 == 0;
}

int is_positive(Pointer data) {
    return (*(int*)data) > 0;
}

void test_length() {
    SList lst = NULL;
    assert(slist_length(lst) == 0);
    int values[] = {1, 2, 3};
    lst = slist_prepend(lst, &values[0]);
    lst = slist_prepend(lst, &values[1]);
    lst = slist_prepend(lst, &values[2]);
    assert(slist_length(lst) == 3);
    while (lst) {
        SListNode* temp = lst;
        lst = lst->next;
        free(temp);
    }
    printf("Test length passed\n");
}

void test_prepend_get() {
    SList lst = NULL;
    int values[] = {1, 2, 3};
    lst = slist_prepend(lst, &values[0]);
    lst = slist_prepend(lst, &values[1]);
    lst = slist_prepend(lst, &values[2]);
    assert(*(int*)slist_get(lst, 0) == 3);
    assert(*(int*)slist_get(lst, 1) == 2);
    assert(*(int*)slist_get(lst, 2) == 1);
    assert(slist_get(lst, 3) == NULL);
    while (lst) {
        SListNode* temp = lst;
        lst = lst->next;
        free(temp);
    }
    printf("Test prepend/get passed\n");
}

void test_remove() {
    SList lst = NULL;
    Pointer removed;
    int values[] = {1, 2, 3, 4};
    lst = slist_append(lst, &values[0]);
    lst = slist_append(lst, &values[1]);
    lst = slist_append(lst, &values[2]);
    lst = slist_append(lst, &values[3]);
    lst = slist_remove(lst, 1, &removed);
    assert(*(int*)removed == 2);
    assert(slist_length(lst) == 3);
    lst = slist_remove(lst, 0, &removed);
    assert(*(int*)removed == 1);
    assert(slist_length(lst) == 2);
    while (lst) {
        SListNode* temp = lst;
        lst = lst->next;
        free(temp);
    }
    printf("Test remove passed\n");
}

void test_append_get_last() {
    SList lst = NULL;
    int values[] = {1, 2, 3};
    lst = slist_append(lst, &values[0]);
    lst = slist_append(lst, &values[1]);
    lst = slist_append(lst, &values[2]);
    assert(*(int*)slist_get(lst, 0) == 1);
    assert(*(int*)slist_get(lst, 1) == 2);
    assert(*(int*)slist_get(lst, 2) == 3);
    assert(*(int*)slist_get_last(lst) == 3);
    while (lst) {
        SListNode* temp = lst;
        lst = lst->next;
        free(temp);
    }
    printf("Test append/get_last passed\n");
}

void test_find() {
    SList lst = NULL;
    int values[] = {1, 2, 3, 2, 4};
    for (int i = 0; i < 5; i++) {
        lst = slist_append(lst, &values[i]);
    }
    assert(slist_find(lst, &values[2]) == 2);
    assert(slist_find(lst, &values[4]) == 4);
    int not_found = 99;
    assert(slist_find(lst, &not_found) == -1);
    while (lst) {
        SListNode* temp = lst;
        lst = lst->next;
        free(temp);
    }
    printf("Test find passed\n");
}

void test_remove_first_all() {
    SList lst = NULL;
    int values[] = {1, 2, 2, 3, 2, 4};
    for (int i = 0; i < 6; i++) {
        lst = slist_append(lst, &values[i]);
    }
    int to_remove = 2;
    lst = slist_remove_first(lst, &to_remove);
    assert(slist_length(lst) == 5);
    assert(slist_find(lst, &to_remove) == 1);
    lst = slist_remove_all(lst, &to_remove);
    assert(slist_length(lst) == 3);
    assert(slist_find(lst, &to_remove) == -1);
    while (lst) {
        SListNode* temp = lst;
        lst = lst->next;
        free(temp);
    }
    printf("Test remove_first/all passed\n");
}

void test_copy_concat() {
    SList lst1 = NULL;
    SList lst2 = NULL;
    int values1[] = {1, 2, 3};
    int values2[] = {4, 5, 6};
    for (int i = 0; i < 3; i++) {
        lst1 = slist_append(lst1, &values1[i]);
        lst2 = slist_append(lst2, &values2[i]);
    }
    SList copy = slist_copy(lst1);
    assert(slist_length(copy) == 3);
    assert(*(int*)slist_get(copy, 0) == 1);
    SList concated = slist_concat(lst1, lst2);
    assert(slist_length(concated) == 6);
    assert(*(int*)slist_get(concated, 5) == 6);
    while (lst1) {
        SListNode* temp = lst1;
        lst1 = lst1->next;
        free(temp);
    }
    while (lst2) {
        SListNode* temp = lst2;
        lst2 = lst2->next;
        free(temp);
    }
    while (copy) {
        SListNode* temp = copy;
        copy = copy->next;
        free(temp);
    }
    while (concated) {
        SListNode* temp = concated;
        concated = concated->next;
        free(temp);
    }
    printf("Test copy/concat passed\n");
}

void test_foreach_find_custom() {
    SList lst = NULL;
    int values[] = {1, -2, 3, -4, 5};
    for (int i = 0; i < 5; i++) {
        lst = slist_append(lst, &values[i]);
    }
    size_t found_index;
    SListNode* found = slist_find_custom(lst, is_even, &found_index);
    assert(found != NULL);
    assert(*(int*)found->data == -2);
    assert(found_index == 1);
    found = slist_find_custom(lst, is_positive, &found_index);
    assert(found != NULL);
    assert(*(int*)found->data == 1);
    assert(found_index == 0);
    while (lst) {
        SListNode* temp = lst;
        lst = lst->next;
        free(temp);
    }
    printf("Test foreach/find_custom passed\n");
}

int main() {
    test_length();
    test_prepend_get();
    test_remove();
    test_append_get_last();
    test_find();
    test_remove_first_all();
    test_copy_concat();
    test_foreach_find_custom();
    printf("All SList tests passed successfully!\n");
    return 0;
}
