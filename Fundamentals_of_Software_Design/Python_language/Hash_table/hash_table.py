class _Node:
    __slots__ = ('key', 'data', 'next')
    def __init__(self, key, data):
        self.key = key
        self.data = data
        self.next = None

class HashTable:
    __slots__ = ('size', 'table', 'hashfunc', 'dtor')
    def __init__(self, size, hashfunc=None, dtor=None):
        self.size = size
        self.table = [None] * size
        self.hashfunc = hashfunc or self._jenkins_hash
        self.dtor = dtor

def _jenkins_hash(key):
    hash_val = 0
    for char in key:
        hash_val += ord(char)
        hash_val += (hash_val << 10)
        hash_val ^= (hash_val >> 6)
    hash_val += (hash_val << 3)
    hash_val ^= (hash_val >> 11)
    hash_val += (hash_val << 15)
    return hash_val

def ht_init(ht, size, hf, dtor):
    ht.size = size
    ht.table = [None] * size
    ht.hashfunc = hf or _jenkins_hash
    ht.dtor = dtor

def ht_destroy(ht):
    for i in range(ht.size):
        current = ht.table[i]
        while current:
            next_node = current.next
            if ht.dtor:
                ht.dtor(current.data)
            current = next_node
    ht.table = None
    ht.size = 0

def ht_set(ht, key, data):
    hash_val = ht.hashfunc(key) % ht.size
    current = ht.table[hash_val]
    
    while current:
        if current.key == key:
            if ht.dtor:
                ht.dtor(current.data)
            current.data = data
            return data
        current = current.next
    
    new_node = _Node(key, data)
    new_node.next = ht.table[hash_val]
    ht.table[hash_val] = new_node
    return data

def ht_get(ht, key):
    hash_val = ht.hashfunc(key) % ht.size
    current = ht.table[hash_val]
    
    while current:
        if current.key == key:
            return current.data
        current = current.next
    return None

def ht_has(ht, key):
    return ht_get(ht, key) is not None

def ht_delete(ht, key):
    hash_val = ht.hashfunc(key) % ht.size
    current = ht.table[hash_val]
    prev = None
    
    while current:
        if current.key == key:
            if prev:
                prev.next = current.next
            else:
                ht.table[hash_val] = current.next
            
            if ht.dtor:
                ht.dtor(current.data)
            return
        
        prev = current
        current = current.next

def ht_traverse(ht, f):
    for i in range(ht.size):
        current = ht.table[i]
        while current:
            f(current.key, current.data)
            current = current.next

def ht_resize(ht, new_size):
    old_table = ht.table
    old_size = ht.size
    
    ht.size = new_size
    ht.table = [None] * new_size
    
    for i in range(old_size):
        current = old_table[i]
        while current:
            next_node = current.next
            hash_val = ht.hashfunc(current.key) % new_size
            current.next = ht.table[hash_val]
            ht.table[hash_val] = current
            current = next_node
