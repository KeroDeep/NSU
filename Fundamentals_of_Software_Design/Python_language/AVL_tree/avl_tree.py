class _Node:
    __slots__ = ('data', 'parent', 'left', 'right', 'height')
    def __init__(self, data):
        self.data = data
        self.parent = None
        self.left = None
        self.right = None
        self.height = 1

class Tree:
    __slots__ = ('root', 'cmp_func')
    def __init__(self, cmp_func):
        self.root = None
        self.cmp_func = cmp_func

def create(cmp_func):
    return Tree(cmp_func)

def clear(tree):
    tree.root = None

def size(tree):
    def _size(node):
        if node is None:
            return 0
        return 1 + _size(node.left) + _size(node.right)
    return _size(tree.root)

def _find_node(tree, data):
    node = tree.root
    while node is not None:
        cmp_result = tree.cmp_func(data, node.data)
        if cmp_result == 0:
            return node
        elif cmp_result < 0:
            node = node.left
        else:
            node = node.right
    return None

def find(tree, data):
    node = _find_node(tree, data)
    return node.data if node is not None else None

def _height(node):
    return node.height if node is not None else 0

def _update_height(node):
    if node is not None:
        node.height = 1 + max(_height(node.left), _height(node.right))

def _balance_factor(node):
    return _height(node.left) - _height(node.right) if node is not None else 0

def _rotate_left(tree, x):
    y = x.right
    x.right = y.left
    if y.left is not None:
        y.left.parent = x
    y.parent = x.parent
    if x.parent is None:
        tree.root = y
    elif x == x.parent.left:
        x.parent.left = y
    else:
        x.parent.right = y
    y.left = x
    x.parent = y
    _update_height(x)
    _update_height(y)
    return y

def _rotate_right(tree, y):
    x = y.left
    y.left = x.right
    if x.right is not None:
        x.right.parent = y
    x.parent = y.parent
    if y.parent is None:
        tree.root = x
    elif y == y.parent.right:
        y.parent.right = x
    else:
        y.parent.left = x
    x.right = y
    y.parent = x
    _update_height(y)
    _update_height(x)
    return x

def _rebalance(tree, node):
    while node is not None:
        _update_height(node)
        balance = _balance_factor(node)
        if balance > 1:
            if _balance_factor(node.left) < 0:
                node.left = _rotate_left(tree, node.left)
            node = _rotate_right(tree, node)
        elif balance < -1:
            if _balance_factor(node.right) > 0:
                node.right = _rotate_right(tree, node.right)
            node = _rotate_left(tree, node)
        node = node.parent

def insert(tree, data):
    new_node = _Node(data)
    if tree.root is None:
        tree.root = new_node
        return None
    
    current = tree.root
    parent = None
    while current is not None:
        parent = current
        cmp_result = tree.cmp_func(data, current.data)
        if cmp_result == 0:
            old_data = current.data
            current.data = data
            return old_data
        elif cmp_result < 0:
            current = current.left
        else:
            current = current.right
    
    cmp_result = tree.cmp_func(data, parent.data)
    if cmp_result < 0:
        parent.left = new_node
    else:
        parent.right = new_node
    new_node.parent = parent
    
    _rebalance(tree, parent)
    return None

def _min_node(node):
    current = node
    while current.left is not None:
        current = current.left
    return current

def delete(tree, data):
    node = _find_node(tree, data)
    if node is None:
        return None
    
    old_data = node.data
    
    if node.left is None and node.right is None:
        if node.parent is None:
            tree.root = None
        elif node == node.parent.left:
            node.parent.left = None
        else:
            node.parent.right = None
        _rebalance(tree, node.parent)
    
    elif node.left is None:
        if node.parent is None:
            tree.root = node.right
            node.right.parent = None
        elif node == node.parent.left:
            node.parent.left = node.right
            node.right.parent = node.parent
        else:
            node.parent.right = node.right
            node.right.parent = node.parent
        _rebalance(tree, node.parent)
    
    elif node.right is None:
        if node.parent is None:
            tree.root = node.left
            node.left.parent = None
        elif node == node.parent.left:
            node.parent.left = node.left
            node.left.parent = node.parent
        else:
            node.parent.right = node.left
            node.left.parent = node.parent
        _rebalance(tree, node.parent)
    
    else:
        successor = _min_node(node.right)
        node.data = successor.data
        if successor == successor.parent.left:
            successor.parent.left = successor.right
        else:
            successor.parent.right = successor.right
        if successor.right is not None:
            successor.right.parent = successor.parent
        _rebalance(tree, successor.parent)
    
    return old_data

def foreach(tree, func):
    def _inorder(node):
        if node is not None:
            _inorder(node.left)
            func(node.data)
            _inorder(node.right)
    _inorder(tree.root)
