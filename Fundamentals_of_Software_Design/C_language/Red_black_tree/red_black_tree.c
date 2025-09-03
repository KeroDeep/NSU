#include <stdlib.h>
#include <stdio.h>
#include <assert.h>

#include "red_black_tree.h"

#define NIL NULL

static BSTNode* create_node(Pointer data) {
    BSTNode* node = (BSTNode*)malloc(sizeof(BSTNode));
    if (!node) return NULL;
    node->data = data;
    node->left = NIL;
    node->right = NIL;
    node->parent = NIL;
    node->color = RED;
    return node;
}

static void left_rotate(BST* tree, BSTNode* x) {
    BSTNode* y = x->right;
    x->right = y->left;
    if (y->left != NIL) {
        y->left->parent = x;
    }
    y->parent = x->parent;
    if (x->parent == NIL) {
        tree->root = y;
    }
    else if (x == x->parent->left) {
        x->parent->left = y;
    }
    else {
        x->parent->right = y;
    }
    y->left = x;
    x->parent = y;
}

static void right_rotate(BST* tree, BSTNode* y) {
    BSTNode* x = y->left;
    y->left = x->right;
    if (x->right != NIL) {
        x->right->parent = y;
    }
    x->parent = y->parent;
    if (y->parent == NIL) {
        tree->root = x;
    }
    else if (y == y->parent->right) {
        y->parent->right = x;
    }
    else {
        y->parent->left = x;
    }
    x->right = y;
    y->parent = x;
}

static void insert_fixup(BST* tree, BSTNode* z) {
    while (z->parent != NIL && z->parent->color == RED) {
        if (z->parent == z->parent->parent->left) {
            BSTNode* y = z->parent->parent->right;
            if (y != NIL && y->color == RED) {
                z->parent->color = BLACK;
                y->color = BLACK;
                z->parent->parent->color = RED;
                z = z->parent->parent;
            }
            else {
                if (z == z->parent->right) {
                    z = z->parent;
                    left_rotate(tree, z);
                }
                z->parent->color = BLACK;
                z->parent->parent->color = RED;
                right_rotate(tree, z->parent->parent);
            }
        }
        else {
            BSTNode* y = z->parent->parent->left;
            if (y != NIL && y->color == RED) {
                z->parent->color = BLACK;
                y->color = BLACK;
                z->parent->parent->color = RED;
                z = z->parent->parent;
            }
            else {
                if (z == z->parent->left) {
                    z = z->parent;
                    right_rotate(tree, z);
                }
                z->parent->color = BLACK;
                z->parent->parent->color = RED;
                left_rotate(tree, z->parent->parent);
            }
        }
    }
    tree->root->color = BLACK;
}

static BSTNode* find_min(BSTNode* node) {
    while (node != NIL && node->left != NIL) {
        node = node->left;
    }
    return node;
}

static void transplant(BST* tree, BSTNode* u, BSTNode* v) {
    if (u->parent == NIL) {
        tree->root = v;
    }
    else if (u == u->parent->left) {
        u->parent->left = v;
    }
    else {
        u->parent->right = v;
    }
    if (v != NIL) {
        v->parent = u->parent;
    }
}

static void delete_fixup(BST* tree, BSTNode* x) {
    while (x != tree->root && x->color == BLACK) {
        if (x == x->parent->left) {
            BSTNode* w = x->parent->right;
            if (w->color == RED) {
                w->color = BLACK;
                x->parent->color = RED;
                left_rotate(tree, x->parent);
                w = x->parent->right;
            }
            if (w->left->color == BLACK && w->right->color == BLACK) {
                w->color = RED;
                x = x->parent;
            }
            else {
                if (w->right->color == BLACK) {
                    w->left->color = BLACK;
                    w->color = RED;
                    right_rotate(tree, w);
                    w = x->parent->right;
                }
                w->color = x->parent->color;
                x->parent->color = BLACK;
                w->right->color = BLACK;
                left_rotate(tree, x->parent);
                x = tree->root;
            }
        }
        else {
            BSTNode* w = x->parent->left;
            if (w->color == RED) {
                w->color = BLACK;
                x->parent->color = RED;
                right_rotate(tree, x->parent);
                w = x->parent->left;
            }
            if (w->right->color == BLACK && w->left->color == BLACK) {
                w->color = RED;
                x = x->parent;
            }
            else {
                if (w->left->color == BLACK) {
                    w->right->color = BLACK;
                    w->color = RED;
                    left_rotate(tree, w);
                    w = x->parent->left;
                }
                w->color = x->parent->color;
                x->parent->color = BLACK;
                w->left->color = BLACK;
                right_rotate(tree, x->parent);
                x = tree->root;
            }
        }
    }
    x->color = BLACK;
}

static BSTNode* insert_node(BST* tree, Pointer data, CmpFunc cmp_func, Pointer* replaced_data) {
    BSTNode* z = create_node(data);
    if (!z) return NIL;
    BSTNode* y = NIL;
    BSTNode* x = tree->root;
    while (x != NIL) {
        y = x;
        int cmp_result = cmp_func(data, x->data);
        if (cmp_result < 0) {
            x = x->left;
        }
        else if (cmp_result > 0) {
            x = x->right;
        }
        else {
            *replaced_data = x->data;
            x->data = data;
            free(z);
            return x;
        }
    }
    z->parent = y;
    if (y == NIL) {
        tree->root = z;
    }
    else if (cmp_func(data, y->data) < 0) {
        y->left = z;
    }
    else {
        y->right = z;
    }
    insert_fixup(tree, z);
    return z;
}

static void delete_node(BST* tree, Pointer data, CmpFunc cmp_func, Pointer* deleted_data) {
    BSTNode* z = tree->root;
    while (z != NIL) {
        int cmp_result = cmp_func(data, z->data);
        if (cmp_result < 0) {
            z = z->left;
        }
        else if (cmp_result > 0) {
            z = z->right;
        }
        else {
            break;
        }
    }
    if (z == NIL) {
        *deleted_data = NULL;
        return;
    }
    *deleted_data = z->data;
    BSTNode* y = z;
    BSTNode* x;
    Color y_original_color = y->color;
    if (z->left == NIL) {
        x = z->right;
        transplant(tree, z, z->right);
    }
    else if (z->right == NIL) {
        x = z->left;
        transplant(tree, z, z->left);
    }
    else {
        y = find_min(z->right);
        y_original_color = y->color;
        x = y->right;
        if (y->parent == z) {
            if (x != NIL) x->parent = y;
        }
        else {
            transplant(tree, y, y->right);
            y->right = z->right;
            y->right->parent = y;
        }
        transplant(tree, z, y);
        y->left = z->left;
        y->left->parent = y;
        y->color = z->color;
    }
    free(z);
    if (y_original_color == BLACK) {
        delete_fixup(tree, x);
    }
}

static void clear_nodes(BSTNode* node) {
    if (node == NIL) return;
    clear_nodes(node->left);
    clear_nodes(node->right);
    free(node);
}

static void foreach_inorder(BSTNode* node, void (*foreach_func)(Pointer data, Pointer extra_data), Pointer extra_data) {
    if (node == NIL) return;
    foreach_inorder(node->left, foreach_func, extra_data);
    foreach_func(node->data, extra_data);
    foreach_inorder(node->right, foreach_func, extra_data);
}

static BSTNode* find_node(BSTNode* node, Pointer data, CmpFunc cmp_func) {
    if (node == NIL) return NIL;
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

static int check_node_properties(BSTNode* node, int* black_count, int current_black_count) {
    if (node == NIL) {
        if (*black_count == -1) {
            *black_count = current_black_count;
        }
        return (*black_count == current_black_count);
    }
    if (node->color == RED) {
        if ((node->left != NIL && node->left->color != BLACK) ||
            (node->right != NIL && node->right->color != BLACK)) {
            return 0;
        }
    }
    int new_black_count = current_black_count;
    if (node->color == BLACK) {
        new_black_count++;
    }
    return check_node_properties(node->left, black_count, new_black_count) && check_node_properties(node->right, black_count, new_black_count);
}

BST * bst_create(CmpFunc cmp_func) {
    BST* tree = (BST*)malloc(sizeof(BST));
    if (!tree) return NULL;
    tree->root = NIL;
    tree->cmp_func = cmp_func;
    tree->size = 0;
    return tree;
}

void bst_clear(BST *tree) {
    if (!tree) return;
    clear_nodes(tree->root);
    tree->root = NIL;
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
    return (node != NIL) ? node->data : NULL;
}

Pointer bst_insert(BST *tree, Pointer data) {
    if (!tree || !data) return NULL;
    Pointer replaced_data = NULL;
    insert_node(tree, data, tree->cmp_func, &replaced_data);
    if (!replaced_data) {
        tree->size++;
    }
    return replaced_data;
}

Pointer bst_delete(BST *tree, Pointer data) {
    if (!tree || !data) return NULL;
    Pointer deleted_data = NULL;
    delete_node(tree, data, tree->cmp_func, &deleted_data);
    if (deleted_data) {
        tree->size--;
    }
    return deleted_data;
}

void bst_foreach(BST *tree, void (*foreach_func)(Pointer data, Pointer extra_data), Pointer extra_data) {
    if (!tree || !foreach_func) return;
    foreach_inorder(tree->root, foreach_func, extra_data);
}

int bst_check(BST *tree) {
    if (!tree) return 0;
    if (tree->root == NIL) return 1;
    if (tree->root->color != BLACK) {
        return 0;
    }
    int black_count = -1;
    return check_node_properties(tree->root, &black_count, 0);
}
