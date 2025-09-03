#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "prefix_tree.h"

void test_destructor(Pointer data) {
    free(data);
}

void test_enumerator(const char* key, Pointer data, void* user) {
    int* count = (int*)user;
    (*count)++;
    printf("Key: %s, Value: %s\n", key, (char*)data);
}

Pointer test_updater(Pointer old, Pointer next) {
    if (old) {
        free(old);
    }
    char* new_str = malloc(strlen((char*)next) + 1);
    strcpy(new_str, (char*)next);
    return new_str;
}

void test_create_free() {
    Trie* trie = trie_create(test_destructor);
    assert(trie != NULL);
    trie_free(trie);
    printf("Test create/free passed\n");
}

void test_set_get() {
    Trie* trie = trie_create(NULL);
    assert(trie_set(trie, "hello", "world"));
    assert(trie_has(trie, "hello"));
    assert(strcmp(trie_get(trie, "hello"), "world") == 0);
    assert(trie_set(trie, "test", "value"));
    assert(strcmp(trie_get(trie, "test"), "value") == 0);
    assert(!trie_has(trie, "nonexistent"));
    assert(trie_get(trie, "nonexistent") == NULL);
    trie_free(trie);
    printf("Test set/get passed\n");
}

void test_update() {
    Trie* trie = trie_create(test_destructor);
    char* value1 = malloc(6);
    strcpy(value1, "hello");
    char* value2 = malloc(6);
    strcpy(value2, "world");
    assert(trie_update(trie, "key", test_updater, value1));
    assert(trie_has(trie, "key"));
    assert(strcmp(trie_get(trie, "key"), "hello") == 0);
    assert(trie_update(trie, "key", test_updater, value2));
    assert(strcmp(trie_get(trie, "key"), "world") == 0);
    free(value1);
    free(value2);
    trie_free(trie);
    printf("Test update passed\n");
}

void test_delete() {
    Trie* trie = trie_create(test_destructor);
    char* value = malloc(6);
    strcpy(value, "test");
    assert(trie_set(trie, "key", value));
    assert(trie_has(trie, "key"));
    assert(trie_delete(trie, "key"));
    assert(!trie_has(trie, "key"));
    assert(!trie_delete(trie, "nonexistent"));
    trie_free(trie);
    printf("Test delete passed\n");
}

void test_traverse() {
    Trie* trie = trie_create(NULL);
    assert(trie_set(trie, "apple", "fruit"));
    assert(trie_set(trie, "banana", "fruit"));
    assert(trie_set(trie, "carrot", "vegetable"));
    int count = 0;
    trie_traverse(trie, test_enumerator, &count);
    assert(count == 3);
    trie_free(trie);
    printf("Test traverse passed\n");
}

void test_prefixes() {
    Trie* trie = trie_create(NULL);
    assert(trie_set(trie, "app", "short"));
    assert(trie_set(trie, "apple", "long"));
    assert(trie_has(trie, "app"));
    assert(trie_has(trie, "apple"));
    assert(!trie_has(trie, "appl"));
    trie_free(trie);
    printf("Test prefixes passed\n");
}

void test_size() {
    Trie* trie = trie_create(NULL);
    assert(trie_size(trie) == 0);
    assert(trie_set(trie, "key1", "value1"));
    assert(trie_size(trie) == 1);
    assert(trie_set(trie, "key2", "value2"));
    assert(trie_size(trie) == 2);
    assert(trie_delete(trie, "key1"));
    assert(trie_size(trie) == 1);
    trie_free(trie);
    printf("Test size passed\n");
}

void test_unicode() {
    Trie* trie = trie_create(NULL);
    assert(trie_set(trie, "привет", "hello"));
    assert(trie_set(trie, "мир", "world"));
    
    assert(trie_has(trie, "привет"));
    assert(trie_has(trie, "мир"));
    assert(strcmp(trie_get(trie, "привет"), "hello") == 0);
    trie_free(trie);
    printf("Test unicode passed\n");
}

int main() {
    test_create_free();
    test_set_get();
    test_update();
    test_delete();
    test_traverse();
    test_prefixes();
    test_size();
    test_unicode();
    printf("All Trie tests passed successfully!\n");
    return 0;
}
