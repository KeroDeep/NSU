#include <stdlib.h>
#include <string.h>
#include <stdio.h>

#include "hash_table.h"

unsigned jenkins_one_at_a_time_hash(char *key) {
    unsigned hash = 0;
    for (; *key; ++key) {
        hash += *key;
        hash += (hash << 10);
        hash ^= (hash >> 6);
    }
    hash += (hash << 3);
    hash ^= (hash >> 11);
    hash += (hash << 15);
    return hash;
}

void ht_init(HashTable *ht, size_t size, HashFunction hf, Destructor dtor) {
    ht->size = size;
    ht->table = calloc(size, sizeof(List*));
    ht->hashfunc = hf ? hf : jenkins_one_at_a_time_hash;
    ht->dtor = dtor;
}

void ht_destroy(HashTable *ht) {
    for (size_t i = 0; i < ht->size; i++) {
        List *current = ht->table[i];
        while (current) {
            List *next = current->next;
            if (ht->dtor) {
                ht->dtor(current->data);
            }
            free(current->key);
            free(current);
            current = next;
        }
    }
    free(ht->table);
    ht->table = NULL;
    ht->size = 0;
}

Pointer ht_set(HashTable *ht, char *key, Pointer data) {
    unsigned hash = ht->hashfunc(key) % ht->size;
    List *current = ht->table[hash];
    while (current) {
        if (strcmp(current->key, key) == 0) {
            Pointer old_data = current->data;
            if (ht->dtor) {
                ht->dtor(old_data);
            }
            current->data = data;
            return old_data;
        }
        current = current->next;
    }
    List *new_node = malloc(sizeof(List));
    new_node->key = strdup(key);
    new_node->data = data;
    new_node->next = ht->table[hash];
    ht->table[hash] = new_node;
    return NULL;
}

Pointer ht_get(HashTable *ht, char *key) {
    unsigned hash = ht->hashfunc(key) % ht->size;
    List *current = ht->table[hash];
    while (current) {
        if (strcmp(current->key, key) == 0) {
            return current->data;
        }
        current = current->next;
    }
    return NULL;
}

int ht_has(HashTable *ht, char *key) {
    return ht_get(ht, key) != NULL;
}

void ht_delete(HashTable *ht, char *key) {
    unsigned hash = ht->hashfunc(key) % ht->size;
    List *current = ht->table[hash];
    List *prev = NULL;
    while (current) {
        if (strcmp(current->key, key) == 0) {
            if (prev) {
                prev->next = current->next;
            }
            else {
                ht->table[hash] = current->next;
            }
            
            if (ht->dtor) {
                ht->dtor(current->data);
            }
            free(current->key);
            free(current);
            return;
        }
        prev = current;
        current = current->next;
    }
}

void ht_traverse(HashTable *ht, void (*f)(char *key, Pointer data)) {
    for (size_t i = 0; i < ht->size; i++) {
        List *current = ht->table[i];
        while (current) {
            f(current->key, current->data);
            current = current->next;
        }
    }
}

void ht_resize(HashTable *ht, size_t new_size) {
    List **new_table = calloc(new_size, sizeof(List*));
    for (size_t i = 0; i < ht->size; i++) {
        List *current = ht->table[i];
        while (current) {
            List *next = current->next;
            unsigned new_hash = ht->hashfunc(current->key) % new_size;
            current->next = new_table[new_hash];
            new_table[new_hash] = current;
            
            current = next;
        }
    }
    free(ht->table);
    ht->table = new_table;
    ht->size = new_size;
}
