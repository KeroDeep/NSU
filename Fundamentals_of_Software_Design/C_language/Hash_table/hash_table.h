#ifndef HASH_TABLE_H
#define HASH_TABLE_H

typedef void *Pointer;

typedef unsigned (*HashFunction)(char *key);

typedef void (*Destructor)(Pointer);

typedef struct List{
    char *key;
    Pointer data;
    struct List *next;
} List;

typedef struct HashTable {
    size_t size;
    List **table;
    HashFunction hashfunc;
    Destructor dtor;
} HashTable;

void ht_init(HashTable *ht, size_t size, HashFunction hf, Destructor dtor);

void ht_destroy(HashTable *ht);

Pointer ht_set(HashTable *ht, char *key, Pointer data);

Pointer ht_get(HashTable *ht, char *key);

int ht_has(HashTable *ht, char *key);

void ht_delete(HashTable *ht, char *key);

void ht_traverse(HashTable *ht, void (*f)(char *key, Pointer data));

void ht_resize(HashTable *ht, size_t new_size);

#endif
