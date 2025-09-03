#include <assert.h>
#include <stdio.h>
#include <string.h>

#include "dlist.h"

void test_create_destroy() {
    DList dl;
    dlist_create(&dl);
    assert(dlist_empty(&dl));
    assert(dlist_size(&dl) == 0);
    dlist_destroy(&dl);
    printf("Test create/destroy passed\n");
}

void test_prepend_append() {
    DList dl;
    dlist_create(&dl);
    dlist_prepend(&dl, "bb");
    dlist_prepend(&dl, "aa");
    dlist_append(&dl, "yy");
    dlist_append(&dl, "zz");
    assert(dlist_size(&dl) == 4);
    assert(strcmp(dlist_nth(&dl, 0), "aa") == 0);
    assert(strcmp(dlist_nth(&dl, 1), "bb") == 0);
    assert(strcmp(dlist_nth(&dl, 2), "yy") == 0);
    assert(strcmp(dlist_nth(&dl, 3), "zz") == 0);
    dlist_destroy(&dl);
    printf("Test prepend/append passed\n");
}

void test_first_last() {
    DList dl;
    dlist_create(&dl);
    dlist_append(&dl, "first");
    dlist_append(&dl, "middle");
    dlist_append(&dl, "last");
    assert(strcmp(dlist_first(&dl), "first") == 0);
    assert(strcmp(dlist_last(&dl), "last") == 0);
    dlist_destroy(&dl);
    printf("Test first/last passed\n");
}

void test_remove() {
    DList dl;
    dlist_create(&dl);
    dlist_append(&dl, "one");
    dlist_append(&dl, "two");
    dlist_append(&dl, "three");
    dlist_append(&dl, "four");
    dlist_remove(&dl, 1);
    assert(dlist_size(&dl) == 3);
    assert(strcmp(dlist_nth(&dl, 0), "one") == 0);
    assert(strcmp(dlist_nth(&dl, 1), "three") == 0);
    assert(strcmp(dlist_nth(&dl, 2), "four") == 0);
    dlist_remove(&dl, 0);
    assert(dlist_size(&dl) == 2);
    assert(strcmp(dlist_nth(&dl, 0), "three") == 0);
    dlist_remove(&dl, 1);
    assert(dlist_size(&dl) == 1);
    assert(strcmp(dlist_nth(&dl, 0), "three") == 0);
    dlist_destroy(&dl);
    printf("Test remove passed\n");
}

void test_insert() {
    DList dl;
    dlist_create(&dl);
    dlist_append(&dl, "a");
    dlist_append(&dl, "d");
    dlist_insert(&dl, 1, "b");
    dlist_insert(&dl, 2, "c");
    assert(dlist_size(&dl) == 4);
    assert(strcmp(dlist_nth(&dl, 0), "a") == 0);
    assert(strcmp(dlist_nth(&dl, 1), "b") == 0);
    assert(strcmp(dlist_nth(&dl, 2), "c") == 0);
    assert(strcmp(dlist_nth(&dl, 3), "d") == 0);
    dlist_insert(&dl, 0, "start");
    assert(strcmp(dlist_nth(&dl, 0), "start") == 0);
    dlist_insert(&dl, 5, "end");
    assert(strcmp(dlist_nth(&dl, 5), "end") == 0);
    dlist_destroy(&dl);
    printf("Test insert passed\n");
}

void test_clear() {
    DList dl;
    dlist_create(&dl);
    for (int i = 0; i < 10; i++) {
        dlist_append(&dl, "data");
    }
    assert(dlist_size(&dl) == 10);
    dlist_clear(&dl);
    assert(dlist_empty(&dl));
    assert(dlist_size(&dl) == 0);
    dlist_destroy(&dl);
    printf("Test clear passed\n");
}

void test_edge_cases() {
    DList dl;
    dlist_create(&dl);
    assert(dlist_nth(&dl, 0) == NULL);
    assert(dlist_nth(&dl, 100) == NULL);
    assert(dlist_first(&dl) == NULL);
    assert(dlist_last(&dl) == NULL);
    dlist_remove(&dl, 0);
    dlist_insert(&dl, 1, "test");
    dlist_append(&dl, "single");
    assert(dlist_size(&dl) == 1);
    assert(strcmp(dlist_first(&dl), "single") == 0);
    assert(strcmp(dlist_last(&dl), "single") == 0);
    dlist_remove(&dl, 0);
    assert(dlist_empty(&dl));
    dlist_destroy(&dl);
    printf("Test edge cases passed\n");
}

void test_bidirectional_traversal() {
    DList dl;
    dlist_create(&dl);
    char* values[] = {"a", "b", "c", "d", "e"};
    for (int i = 0; i < 5; i++) {
        dlist_append(&dl, values[i]);
    }
    for (int i = 0; i < 5; i++) {
        assert(strcmp(dlist_nth(&dl, i), values[i]) == 0);
    }
    for (int i = 4; i >= 0; i--) {
        assert(strcmp(dlist_nth(&dl, i), values[i]) == 0);
    }
    dlist_destroy(&dl);
    printf("Test bidirectional traversal passed\n");
}

int main() {
    test_create_destroy();
    test_prepend_append();
    test_first_last();
    test_remove();
    test_insert();
    test_clear();
    test_edge_cases();
    test_bidirectional_traversal();
    printf("All DList tests passed successfully!\n");
    return 0;
}
