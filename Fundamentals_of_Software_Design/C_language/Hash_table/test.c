#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "hash_table.h"

void test_destructor(Pointer data) {
    free(data);
}

void test_traverse_func(char *key, Pointer data) {
    printf("Key: %s, Value: %s\n", key, (char*)data);
}

void test_init_destroy() {
    HashTable ht;
    ht_init(&ht, 10, NULL, NULL);
    assert(ht.size == 10);
    assert(ht.table != NULL);
    ht_destroy(&ht);
    printf("Test init/destroy passed\n");
}

void test_set_get() {
    HashTable ht;
    ht_init(&ht, 10, NULL, NULL);
    assert(ht_set(&ht, "key1", "value1") == NULL);
    assert(ht_set(&ht, "key2", "value2") == NULL);
    assert(strcmp(ht_get(&ht, "key1"), "value1") == 0);
    assert(strcmp(ht_get(&ht, "key2"), "value2") == 0);
    assert(ht_get(&ht, "nonexistent") == NULL);
    ht_destroy(&ht);
    printf("Test set/get passed\n");
}

void test_overwrite() {
    HashTable ht;
    ht_init(&ht, 10, NULL, NULL);
    ht_set(&ht, "key", "old_value");
    Pointer old = ht_set(&ht, "key", "new_value");
    assert(strcmp(old, "old_value") == 0);
    assert(strcmp(ht_get(&ht, "key"), "new_value") == 0);
    ht_destroy(&ht);
    printf("Test overwrite passed\n");
}

void test_has_delete() {
    HashTable ht;
    ht_init(&ht, 10, NULL, NULL);
    ht_set(&ht, "key", "value");
    assert(ht_has(&ht, "key"));
    ht_delete(&ht, "key");
    assert(!ht_has(&ht, "key"));
    ht_delete(&ht, "nonexistent");
    ht_destroy(&ht);
    printf("Test has/delete passed\n");
}

void test_traverse() {
    HashTable ht;
    ht_init(&ht, 10, NULL, NULL);
    ht_set(&ht, "key1", "value1");
    ht_set(&ht, "key2", "value2");
    ht_set(&ht, "key3", "value3");
    printf("Traverse output:\n");
    ht_traverse(&ht, test_traverse_func);
    ht_destroy(&ht);
    printf("Test traverse passed\n");
}

void test_resize() {
    HashTable ht;
    ht_init(&ht, 2, NULL, NULL);
    ht_set(&ht, "key1", "value1");
    ht_set(&ht, "key2", "value2");
    ht_set(&ht, "key3", "value3");
    assert(ht_has(&ht, "key1"));
    assert(ht_has(&ht, "key2"));
    assert(ht_has(&ht, "key3"));
    ht_resize(&ht, 10);
    assert(ht_has(&ht, "key1"));
    assert(ht_has(&ht, "key2"));
    assert(ht_has(&ht, "key3"));
    assert(ht.size == 10);
    ht_destroy(&ht);
    printf("Test resize passed\n");
}

void test_custom_hash() {
    unsigned simple_hash(char *key) {
        return key[0];
    }
    HashTable ht;
    ht_init(&ht, 10, simple_hash, NULL);
    ht_set(&ht, "apple", "fruit");
    ht_set(&ht, "avocado", "vegetable");
    assert(ht_has(&ht, "apple"));
    assert(ht_has(&ht, "avocado"));
    ht_destroy(&ht);
    printf("Test custom hash passed\n");
}

void test_destructor_func() {
    HashTable ht;
    ht_init(&ht, 10, NULL, test_destructor);
    char *value1 = strdup("value1");
    char *value2 = strdup("value2");
    ht_set(&ht, "key1", value1);
    ht_set(&ht, "key2", value2);
    ht_set(&ht, "key1", strdup("new_value"));
    ht_delete(&ht, "key2");
    ht_destroy(&ht);
    printf("Test destructor passed\n");
}

void test_collisions() {
    HashTable ht;
    ht_init(&ht, 1, NULL, NULL);
    ht_set(&ht, "key1", "value1");
    ht_set(&ht, "key2", "value2");
    ht_set(&ht, "key3", "value3");
    assert(strcmp(ht_get(&ht, "key1"), "value1") == 0);
    assert(strcmp(ht_get(&ht, "key2"), "value2") == 0);
    assert(strcmp(ht_get(&ht, "key3"), "value3") == 0);
    ht_destroy(&ht);
    printf("Test collisions passed\n");
}

int main() {
    test_init_destroy();
    test_set_get();
    test_overwrite();
    test_has_delete();
    test_traverse();
    test_resize();
    test_custom_hash();
    test_destructor_func();
    test_collisions();
    printf("All HashTable tests passed successfully!\n");
    return 0;
}
