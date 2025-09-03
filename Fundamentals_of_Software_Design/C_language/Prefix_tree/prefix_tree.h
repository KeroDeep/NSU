#ifndef PREFIX_TREE_H
#define PREFIX_TREE_H

typedef void* Pointer;

typedef void (*Destructor)(Pointer data);

typedef void (*Enumerator)(const char* key, Pointer data, void* user);

typedef Pointer (*Updater)(Pointer old, Pointer next);

typedef struct _Trie {
    struct _TrieNode* root;
    Destructor dtor;
    size_t size;
} Trie;

Trie* trie_create(Destructor dtor);

void trie_free(Trie* tr);

int trie_has(const Trie* tr, const char* key);

Pointer trie_get(const Trie* tr, const char* key);

int trie_set(Trie* tr, const char* key, Pointer data);

int trie_update(Trie* tr, const char* key, Updater up, Pointer next);

int trie_delete(Trie* tr, const char* key);

void trie_traverse(const Trie* tr, Enumerator en, void* user);

size_t trie_size(const Trie* tr);

#endif
