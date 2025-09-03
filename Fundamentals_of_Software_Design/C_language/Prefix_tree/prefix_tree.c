#include <stdlib.h>
#include <string.h>
#include <stdio.h>

#include "prefix_tree.h"

#define ALPHABET_SIZE 256

typedef struct _TrieNode {
    struct _TrieNode* children[ALPHABET_SIZE];
    Pointer data;
    int is_end;
    char character;
} TrieNode;

static TrieNode* create_node(char ch) {
    TrieNode* node = (TrieNode*)calloc(1, sizeof(TrieNode));
    if (!node) return NULL;
    node->is_end = 0;
    node->data = NULL;
    node->character = ch;
    return node;
}

static void free_node(TrieNode* node, Destructor dtor) {
    if (!node) return;
    for (int i = 0; i < ALPHABET_SIZE; i++) {
        if (node->children[i]) {
            free_node(node->children[i], dtor);
        }
    }
    if (node->is_end && dtor) {
        dtor(node->data);
    }
    free(node);
}

static TrieNode* find_node(const TrieNode* node, const char* key) {
    if (!node || !key) return NULL;
    TrieNode* current = (TrieNode*)node;
    for (size_t i = 0; key[i]; i++) {
        int ch = (unsigned char)key[i];
        if (!current->children[ch]) {
            return NULL;
        }
        current = current->children[ch];
    }
    return current->is_end ? current : NULL;
}

static void traverse_node(const TrieNode* node, char* buffer, size_t depth, Enumerator en, void* user) {
    if (!node) return;
    buffer[depth] = node->character;
    if (node->is_end) {
        buffer[depth + 1] = '\0';
        en(buffer, node->data, user);
    }
    for (int i = 0; i < ALPHABET_SIZE; i++) {
        if (node->children[i]) {
            traverse_node(node->children[i], buffer, depth + 1, en, user);
        }
    }
}

Trie* trie_create(Destructor dtor) {
    Trie* trie = (Trie*)malloc(sizeof(Trie));
    if (!trie) return NULL;
    trie->root = create_node('\0');
    if (!trie->root) {
        free(trie);
        return NULL;
    }
    trie->dtor = dtor;
    trie->size = 0;
    return trie;
}

void trie_free(Trie* tr) {
    if (!tr) return;
    free_node(tr->root, tr->dtor);
    free(tr);
}

int trie_has(const Trie* tr, const char* key) {
    if (!tr || !key) return 0;
    return find_node(tr->root, key) != NULL;
}

Pointer trie_get(const Trie* tr, const char* key) {
    if (!tr || !key) return NULL;
    TrieNode* node = find_node(tr->root, key);
    return node ? node->data : NULL;
}

int trie_set(Trie* tr, const char* key, Pointer data) {
    if (!tr || !key) return 0;
    TrieNode* current = tr->root;
    for (size_t i = 0; key[i]; i++) {
        int ch = (unsigned char)key[i];
        if (!current->children[ch]) {
            current->children[ch] = create_node(key[i]);
            if (!current->children[ch]) return 0;
        }
        current = current->children[ch];
    }
    if (current->is_end && tr->dtor) {
        tr->dtor(current->data);
    }
    else if (!current->is_end) {
        tr->size++;
    }
    current->is_end = 1;
    current->data = data;
    return 1;
}

int trie_update(Trie* tr, const char* key, Updater up, Pointer next) {
    if (!tr || !key || !up) return 0;
    TrieNode* current = tr->root;
    for (size_t i = 0; key[i]; i++) {
        int ch = (unsigned char)key[i];
        if (!current->children[ch]) {
            current->children[ch] = create_node(key[i]);
            if (!current->children[ch]) return 0;
        }
        current = current->children[ch];
    }
    Pointer old_data = current->is_end ? current->data : NULL;
    Pointer new_data = up(old_data, next);
    if (!current->is_end) {
        tr->size++;
    }
    current->is_end = 1;
    current->data = new_data;
    return 1;
}

static int can_prune(TrieNode* node) {
    if (!node) return 1;
    if (node->is_end) return 0;
    for (int i = 0; i < ALPHABET_SIZE; i++) {
        if (node->children[i]) {
            return 0;
        }
    }
    return 1;
}

static int delete_recursive(TrieNode* node, const char* key, size_t depth, Destructor dtor, int* deleted) {
    if (!node) return 0;
    if (key[depth] == '\0') {
        if (node->is_end) {
            if (dtor) {
                dtor(node->data);
            }
            node->is_end = 0;
            node->data = NULL;
            *deleted = 1;
            return can_prune(node);
        }
        return 0;
    }
    int ch = (unsigned char)key[depth];
    if (!node->children[ch]) {
        return 0;
    }
    int should_prune = delete_recursive(node->children[ch], key, depth + 1, dtor, deleted);
    if (should_prune) {
        free(node->children[ch]);
        node->children[ch] = NULL;
        return can_prune(node);
    }
    return 0;
}

int trie_delete(Trie* tr, const char* key) {
    if (!tr || !key) return 0;
    int deleted = 0;
    delete_recursive(tr->root, key, 0, tr->dtor, &deleted);
    if (deleted) {
        tr->size--;
    }
    return deleted;
}

void trie_traverse(const Trie* tr, Enumerator en, void* user) {
    if (!tr || !en) return;
    char buffer[1024];
    for (int i = 0; i < ALPHABET_SIZE; i++) {
        if (tr->root->children[i]) {
            traverse_node(tr->root->children[i], buffer, 0, en, user);
        }
    }
}

size_t trie_size(const Trie* tr) {
    return tr ? tr->size : 0;
}
