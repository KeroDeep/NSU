#include <stdlib.h>
#include <stdio.h>

#include "binary_tree.h"

static BSTNode* create_node(Pointer data) {
    BSTNode* node = (BSTNode*)malloc(sizeof(BSTNode));
    if (!node) return NULL;
    node->data = data;
    node->left = NULL;
    node->right = NULL;
    return node;
}

static BSTNode* find_min(BSTNode* node) {
    while (node && node->left) {
        node = node->left;
    }
    return node;
}

static BSTNode* insert_node(BSTNode* node, Pointer data, CmpFunc cmp_func, Pointer* replaced_data) {
    if (!node) {
        return create_node(data);
    }
    int cmp_result = cmp_func(data, node->data);
    if (cmp_result < 0) {
        node->left = insert_node(node->left, data, cmp_func, replaced_data);
    }
    else if (cmp_result > 0) {
        node->right = insert_node(node->right, data, cmp_func, replaced_data);
    }
    else {
        *replaced_data = node->data;
        node->data = data;
    }
    return node;
}

static BSTNode* delete_node(BSTNode* node, Pointer data, CmpFunc cmp_func, Pointer* deleted_data) {
    if (!node) {
        return NULL;
    }
    int cmp_result = cmp_func(data, node->data);
    if (cmp_result < 0) {
        node->left = delete_node(node->left, data, cmp_func, deleted_data);
    }
    else if (cmp_result > 0) {
        node->right = delete_node(node->right, data, cmp_func, deleted_data);
    }
    else {
        *deleted_data = node->data;
        if (!node->left) {
            BSTNode* temp = node->right;
            free(node);
            return temp;
        }
        else if (!node->right) {
            BSTNode* temp = node->left;
            free(node);
            return temp;
        }
        else {
            BSTNode* temp = find_min(node->right);
            node->data = temp->data;
            node->right = delete_node(node->right, temp->data, cmp_func, &temp->data);
        }
    }
    return node;
}

static void clear_nodes(BSTNode* node) {
    if (!node) return;
    clear_nodes(node->left);
    clear_nodes(node->right);
    free(node);
}

static void foreach_inorder(BSTNode* node, void (*foreach_func)(Pointer data, Pointer extra_data), Pointer extra_data) {
    if (!node) return;
    foreach_inorder(node->left, foreach_func, extra_data);
    foreach_func(node->data, extra_data);
    foreach_inorder(node->right, foreach_func, extra_data);
}

static BSTNode* find_node(BSTNode* node, Pointer data, CmpFunc cmp_func) {
    if (!node) return NULL;
    int cmp_result = cmp_func(data, node->data);
    if (cmp_result < 0) {
        return find_node(node->left, data, cmp_func);
    }
    else if (cmp_result > 0) {
        return find_node(node->right, data, cmp_func);
    }
    else {
        return node;
    }
}

BST * bst_create(CmpFunc cmp_func) {
    BST* tree = (BST*)malloc(sizeof(BST));
    if (!tree) return NULL;
    tree->root = NULL;
    tree->cmp_func = cmp_func;
    tree->size = 0;
    return tree;
}

void bst_clear(BST *tree) {
    if (!tree) return;
    clear_nodes(tree->root);
    tree->root = NULL;
    tree->size = 0;
}

void bst_destroy(BST *tree) {
    if (!tree) return;
    bst_clear(tree);
    free(tree);
}

size_t bst_size(BST *tree) {
    return tree ? tree->size : 0;
}

Pointer bst_find(BST *tree, Pointer data) {
    if (!tree || !data) return NULL;
    BSTNode* node = find_node(tree->root, data, tree->cmp_func);
    return node ? node->data : NULL;
}

Pointer bst_insert(BST *tree, Pointer data) {
    if (!tree || !data) return NULL;
    Pointer replaced_data = NULL;
    tree->root = insert_node(tree->root, data, tree->cmp_func, &replaced_data);
    if (!replaced_data) {
        tree->size++;
    }
    return replaced_data;
}

Pointer bst_delete(BST *tree, Pointer data) {
    if (!tree || !data) return NULL;
    Pointer deleted_data = NULL;
    tree->root = delete_node(tree->root, data, tree->cmp_func, &deleted_data);
    if (deleted_data) {
        tree->size--;
    }
    return deleted_data;
}

void bst_foreach(BST *tree, void (*foreach_func)(Pointer data, Pointer extra_data), Pointer extra_data) {
    if (!tree || !foreach_func) return;
    foreach_inorder(tree->root, foreach_func, extra_data);
}
