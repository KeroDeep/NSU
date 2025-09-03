#include <stdlib.h>
#include <stdio.h>

#include "avl_tree.h"

#define MAX(a, b) ((a) > (b) ? (a) : (b))

static BSTNode* create_node(Pointer data) {
    BSTNode* node = (BSTNode*)malloc(sizeof(BSTNode));
    if (!node) return NULL;
    node->data = data;
    node->left = NULL;
    node->right = NULL;
    node->height = 1;
    return node;
}

static int height(BSTNode* node) {
    return node ? node->height : 0;
}

static int balance_factor(BSTNode* node) {
    return node ? height(node->left) - height(node->right) : 0;
}

static void update_height(BSTNode* node) {
    if (node) {
        node->height = 1 + MAX(height(node->left), height(node->right));
    }
}

static BSTNode* rotate_right(BSTNode* y) {
    BSTNode* x = y->left;
    BSTNode* T2 = x->right;
    x->right = y;
    y->left = T2;
    update_height(y);
    update_height(x);
    return x;
}

static BSTNode* rotate_left(BSTNode* x) {
    BSTNode* y = x->right;
    BSTNode* T2 = y->left;
    y->left = x;
    x->right = T2;
    update_height(x);
    update_height(y);
    return y;
}

static BSTNode* balance_node(BSTNode* node) {
    if (!node) return node;
    update_height(node);
    int balance = balance_factor(node);
    if (balance > 1 && balance_factor(node->left) >= 0) {
        return rotate_right(node);
    }
    if (balance < -1 && balance_factor(node->right) <= 0) {
        return rotate_left(node);
    }
    if (balance > 1 && balance_factor(node->left) < 0) {
        node->left = rotate_left(node->left);
        return rotate_right(node);
    }
    if (balance < -1 && balance_factor(node->right) > 0) {
        node->right = rotate_right(node->right);
        return rotate_left(node);
    }
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
        return node;
    }
    return balance_node(node);
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
        if (!node->left || !node->right) {
            BSTNode* temp = node->left ? node->left : node->right;
            if (!temp) {
                temp = node;
                node = NULL;
            }
            else {
                *node = *temp;
            }
            
            free(temp);
        }
        else {
            BSTNode* temp = find_min(node->right);
            node->data = temp->data;
            node->right = delete_node(node->right, temp->data, cmp_func, &temp->data);
        }
    }
    if (!node) {
        return node;
    }
    return balance_node(node);
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
