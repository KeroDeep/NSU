#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "avl_tree.h"

int int_cmp(Pointer a, Pointer b) {
    int int_a = *(int*)a;
    int int_b = *(int*)b;
    return (int_a > int_b) - (int_a < int_b);
}
int str_cmp(Pointer a, Pointer b) {
    return strcmp((const char*)a, (const char*)b);
}

void test_create_destroy() {
    BST* tree = bst_create(int_cmp);
    assert(tree != NULL);
    assert(bst_size(tree) == 0);
    bst_destroy(tree);
    printf("Test create/destroy passed\n");
}

void test_insert_find() {
    BST* tree = bst_create(int_cmp);
    int values[] = {5, 3, 7, 2, 4, 6, 8};
    size_t count = sizeof(values) / sizeof(values[0]);
    for (size_t i = 0; i < count; i++) {
        Pointer result = bst_insert(tree, &values[i]);
        assert(result == NULL);
    }
    assert(bst_size(tree) == count);
    for (size_t i = 0; i < count; i++) {
        int* found = (int*)bst_find(tree, &values[i]);
        assert(found != NULL);
        assert(*found == values[i]);
    }
    int not_found = 999;
    assert(bst_find(tree, &not_found) == NULL);
    bst_destroy(tree);
    printf("Test insert/find passed\n");
}

void test_insert_replace() {
    BST* tree = bst_create(int_cmp);
    int value1 = 42;
    int value2 = 42;
    int value3 = 100;
    Pointer result1 = bst_insert(tree, &value1);
    assert(result1 == NULL);
    Pointer result2 = bst_insert(tree, &value2);
    assert(result2 == &value1);
    Pointer result3 = bst_insert(tree, &value3);
    assert(result3 == NULL);
    assert(bst_size(tree) == 2);
    bst_destroy(tree);
    printf("Test insert replace passed\n");
}

void test_delete() {
    BST* tree = bst_create(int_cmp);
    int values[] = {50, 30, 70, 20, 40, 60, 80};
    size_t count = sizeof(values) / sizeof(values[0]);
    for (size_t i = 0; i < count; i++) {
        bst_insert(tree, &values[i]);
    }
    assert(bst_size(tree) == count);
    int to_delete1 = 20;
    int* deleted1 = (int*)bst_delete(tree, &to_delete1);
    assert(deleted1 != NULL);
    assert(*deleted1 == 20);
    assert(bst_size(tree) == count - 1);
    assert(bst_find(tree, &to_delete1) == NULL);
    int to_delete2 = 30;
    int* deleted2 = (int*)bst_delete(tree, &to_delete2);
    assert(deleted2 != NULL);
    assert(*deleted2 == 30);
    assert(bst_size(tree) == count - 2);
    int to_delete3 = 50;
    int* deleted3 = (int*)bst_delete(tree, &to_delete3);
    assert(deleted3 != NULL);
    assert(*deleted3 == 50);
    assert(bst_size(tree) == count - 3);
    int not_exists = 999;
    assert(bst_delete(tree, &not_exists) == NULL);
    bst_destroy(tree);
    printf("Test delete passed\n");
}

void test_foreach() {
    BST* tree = bst_create(int_cmp);
    int values[] = {5, 3, 7, 1, 4, 6, 9};
    size_t count = sizeof(values) / sizeof(values[0]);
    for (size_t i = 0; i < count; i++) {
        bst_insert(tree, &values[i]);
    }
    int sum = 0;
    void sum_func(Pointer data, Pointer extra_data) {
        int* value = (int*)data;
        int* sum_ptr = (int*)extra_data;
        *sum_ptr += *value;
    }
    bst_foreach(tree, sum_func, &sum);
    int expected_sum = 5 + 3 + 7 + 1 + 4 + 6 + 9;
    assert(sum == expected_sum);
    bst_destroy(tree);
    printf("Test foreach passed\n");
}

void test_string_keys() {
    BST* tree = bst_create(str_cmp);
    const char* strings[] = {"apple", "banana", "cherry", "date", "elderberry"};
    size_t count = sizeof(strings) / sizeof(strings[0]);
    for (size_t i = 0; i < count; i++) {
        bst_insert(tree, (Pointer)strings[i]);
    }
    assert(bst_size(tree) == count);
    const char* found = (const char*)bst_find(tree, "banana");
    assert(found != NULL);
    assert(strcmp(found, "banana") == 0);
    const char* not_found = (const char*)bst_find(tree, "fig");
    assert(not_found == NULL);
    bst_destroy(tree);
    printf("Test string keys passed\n");
}

void test_avl_balance() {
    BST* tree = bst_create(int_cmp);
    for (int i = 0; i < 100; i++) {
        int* value = malloc(sizeof(int));
        *value = i;
        bst_insert(tree, value);
    }
    assert(bst_size(tree) == 100);
    for (int i = 0; i < 100; i++) {
        int* found = (int*)bst_find(tree, &i);
        assert(found != NULL);
        assert(*found == i);
    }
    void free_func(Pointer data, Pointer extra_data) {
        (void)extra_data;
        free(data);
    }
    bst_foreach(tree, free_func, NULL);
    bst_destroy(tree);
    printf("Test AVL balance passed\n");
}

void test_clear() {
    BST* tree = bst_create(int_cmp);
    int values[] = {1, 2, 3, 4, 5};
    for (size_t i = 0; i < 5; i++) {
        bst_insert(tree, &values[i]);
    }
    assert(bst_size(tree) == 5);
    bst_clear(tree);
    assert(bst_size(tree) == 0);
    assert(tree->root == NULL);
    bst_destroy(tree);
    printf("Test clear passed\n");
}

int main() {
    test_create_destroy();
    test_insert_find();
    test_insert_replace();
    test_delete();
    test_foreach();
    test_string_keys();
    test_avl_balance();
    test_clear();
    printf("All AVL tree tests passed successfully!\n");
    return 0;
}
