class _Node:
    __slots__ = ('data', 'parent', 'left', 'right')
    def __init__(self, data):
        self.data = data
        self.parent = None
        self.left = None
        self.right = None

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
    
    else:
        successor = _min_node(node.right)
        node.data = successor.data
        if successor == successor.parent.left:
            successor.parent.left = successor.right
        else:
            successor.parent.right = successor.right
        if successor.right is not None:
            successor.right.parent = successor.parent
    
    return old_data

def foreach(tree, func):
    def _inorder(node):
        if node is not None:
            _inorder(node.left)
            func(node.data)
            _inorder(node.right)
    _inorder(tree.root)
