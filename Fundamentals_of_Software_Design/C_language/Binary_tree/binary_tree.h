#ifndef BINARY_TREE_H
#define BINARY_TREE_H

#include <stddef.h>

typedef void * Pointer;

typedef int (*CmpFunc)(Pointer data1, Pointer data2);

typedef struct tBSTNode {
    Pointer data;
    struct tBSTNode *left;
    struct tBSTNode *right;
} BSTNode;

typedef struct tBST {
    BSTNode *root;
    CmpFunc cmp_func;
    size_t size;
} BST;

BST * bst_create(CmpFunc cmp_func);

void bst_clear(BST *tree);

void bst_destroy(BST *tree);

size_t bst_size(BST *tree);

Pointer bst_find(BST *tree, Pointer data);

Pointer bst_insert(BST *tree, Pointer data);

Pointer bst_delete(BST *tree, Pointer data);

void bst_foreach(BST *tree, void (*foreach_func)(Pointer data, Pointer extra_data), Pointer extra_data);

#endif
