class TrieNode:
    __slots__ = ('children', 'data', 'has_data')
    def __init__(self):
        self.children = {}
        self.data = None
        self.has_data = False

class Trie:
    __slots__ = ('root', 'dtor')
    def __init__(self, dtor):
        self.root = TrieNode()
        self.dtor = dtor

def trie_create(dtor):
    return Trie(dtor)

def trie_free(tr):
    def _free_node(node):
        if node:
            for child in node.children.values():
                _free_node(child)
            if tr.dtor and node.has_data:
                tr.dtor(node.data)
    _free_node(tr.root)
    tr.root = None

def trie_has(tr, key):
    node = tr.root
    for char in key:
        if char not in node.children:
            return 0
        node = node.children[char]
    return 1 if node.has_data else 0

def trie_get(tr, key):
    node = tr.root
    for char in key:
        if char not in node.children:
            return None
        node = node.children[char]
    return node.data if node.has_data else None

def trie_set(tr, key, data):
    node = tr.root
    for char in key:
        if char not in node.children:
            node.children[char] = TrieNode()
        node = node.children[char]
    
    if tr.dtor and node.has_data:
        tr.dtor(node.data)
    
    node.data = data
    node.has_data = True
    return 1

def trie_update(tr, key, up, next):
    node = tr.root
    for char in key:
        if char not in node.children:
            node.children[char] = TrieNode()
        node = node.children[char]
    
    old_data = node.data if node.has_data else None
    node.data = up(old_data, next)
    node.has_data = True
    return 1

def _should_delete(node):
    return not node.has_data and not node.children

def trie_delete(tr, key):
    def _delete_recursive(node, key, index):
        if index == len(key):
            if not node.has_data:
                return False, False
            
            if tr.dtor:
                tr.dtor(node.data)
            node.data = None
            node.has_data = False
            return True, _should_delete(node)
        
        char = key[index]
        if char not in node.children:
            return False, False
        
        child = node.children[char]
        deleted, should_delete_child = _delete_recursive(child, key, index + 1)
        
        if should_delete_child:
            del node.children[char]
        
        return deleted, _should_delete(node)
    
    deleted, _ = _delete_recursive(tr.root, key, 0)
    return 1 if deleted else 0

def trie_traverse(tr, en, user):
    def _traverse_node(node, current_key):
        if node.has_data:
            en(current_key, node.data, user)
        
        for char, child in node.children.items():
            _traverse_node(child, current_key + char)
    
    _traverse_node(tr.root, "")
