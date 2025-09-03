class _Node:
    __slots__ = ('data', 'parent', 'left', 'right', 'color')
    def __init__(self, data):
        self.data = data
        self.parent = None
        self.left = None
        self.right = None
        self.color = 1

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

def _rotate_right(tree, x):
    y = x.left
    x.left = y.right
    if y.right is not None:
        y.right.parent = x
    y.parent = x.parent
    if x.parent is None:
        tree.root = y
    elif x == x.parent.right:
        x.parent.right = y
    else:
        x.parent.left = y
    y.right = x
    x.parent = y

def _fix_insert(tree, node):
    while node != tree.root and node.parent.color == 1:
        if node.parent == node.parent.parent.left:
            uncle = node.parent.parent.right
            if uncle is not None and uncle.color == 1:
                node.parent.color = 0
                uncle.color = 0
                node.parent.parent.color = 1
                node = node.parent.parent
            else:
                if node == node.parent.right:
                    node = node.parent
                    _rotate_left(tree, node)
                node.parent.color = 0
                node.parent.parent.color = 1
                _rotate_right(tree, node.parent.parent)
        else:
            uncle = node.parent.parent.left
            if uncle is not None and uncle.color == 1:
                node.parent.color = 0
                uncle.color = 0
                node.parent.parent.color = 1
                node = node.parent.parent
            else:
                if node == node.parent.left:
                    node = node.parent
                    _rotate_right(tree, node)
                node.parent.color = 0
                node.parent.parent.color = 1
                _rotate_left(tree, node.parent.parent)
    tree.root.color = 0

def insert(tree, data):
    new_node = _Node(data)
    if tree.root is None:
        tree.root = new_node
        new_node.color = 0
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
    
    _fix_insert(tree, new_node)
    return None

def _min_node(node):
    current = node
    while current.left is not None:
        current = current.left
    return current

def _fix_delete(tree, node):
    while node != tree.root and (node is None or node.color == 0):
        if node == node.parent.left:
            sibling = node.parent.right
            if sibling.color == 1:
                sibling.color = 0
                node.parent.color = 1
                _rotate_left(tree, node.parent)
                sibling = node.parent.right
            
            if (sibling.left is None or sibling.left.color == 0) and (sibling.right is None or sibling.right.color == 0):
                sibling.color = 1
                node = node.parent
            else:
                if sibling.right is None or sibling.right.color == 0:
                    sibling.left.color = 0
                    sibling.color = 1
                    _rotate_right(tree, sibling)
                    sibling = node.parent.right
                
                sibling.color = node.parent.color
                node.parent.color = 0
                sibling.right.color = 0
                _rotate_left(tree, node.parent)
                node = tree.root
        else:
            sibling = node.parent.left
            if sibling.color == 1:
                sibling.color = 0
                node.parent.color = 1
                _rotate_right(tree, node.parent)
                sibling = node.parent.left
            
            if (sibling.right is None or sibling.right.color == 0) and (sibling.left is None or sibling.left.color == 0):
                sibling.color = 1
                node = node.parent
            else:
                if sibling.left is None or sibling.left.color == 0:
                    sibling.right.color = 0
                    sibling.color = 1
                    _rotate_left(tree, sibling)
                    sibling = node.parent.left
                
                sibling.color = node.parent.color
                node.parent.color = 0
                sibling.left.color = 0
                _rotate_right(tree, node.parent)
                node = tree.root
    
    if node is not None:
        node.color = 0

def delete(tree, data):
    node = _find_node(tree, data)
    if node is None:
        return None
    
    old_data = node.data
    y = node
    y_original_color = y.color
    x = None
    
    if node.left is None:
        x = node.right
        _transplant(tree, node, node.right)
    elif node.right is None:
        x = node.left
        _transplant(tree, node, node.left)
    else:
        y = _min_node(node.right)
        y_original_color = y.color
        x = y.right
        
        if y.parent == node:
            if x is not None:
                x.parent = y
        else:
            _transplant(tree, y, y.right)
            y.right = node.right
            y.right.parent = y
        
        _transplant(tree, node, y)
        y.left = node.left
        y.left.parent = y
        y.color = node.color
    
    if y_original_color == 0:
        _fix_delete(tree, x)
    
    return old_data

def _transplant(tree, u, v):
    if u.parent is None:
        tree.root = v
    elif u == u.parent.left:
        u.parent.left = v
    else:
        u.parent.right = v
    if v is not None:
        v.parent = u.parent

def foreach(tree, func):
    def _inorder(node):
        if node is not None:
            _inorder(node.left)
            func(node.data)
            _inorder(node.right)
    _inorder(tree.root)

def check(tree):
    def _check_rb_properties(node):
        if node is None:
            return 0, True
        
        left_black_height, left_valid = _check_rb_properties(node.left)
        right_black_height, right_valid = _check_rb_properties(node.right)
        
        if not left_valid or not right_valid:
            return 0, False
        
        if left_black_height != right_black_height:
            return 0, False
        
        black_height = left_black_height
        if node.color == 0:
            black_height += 1
        
        if node.color == 1:
            if (node.left is not None and node.left.color == 1) or (node.right is not None and node.right.color == 1):
                return black_height, False
        
        return black_height, True
    
    if tree.root is None:
        return True
    
    if tree.root.color != 0:
        return False
    
    _, is_valid = _check_rb_properties(tree.root)
    return is_valid
