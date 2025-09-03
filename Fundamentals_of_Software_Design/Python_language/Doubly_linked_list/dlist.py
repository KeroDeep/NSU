class DListNode:
    __slots__ = ('data', 'prev', 'next')
    def __init__(self, data):
        self.data = data
        self.prev = None
        self.next = None

class DList:
    __slots__ = ('head', 'tail', 'size')
    def __init__(self):
        self.head = None
        self.tail = None
        self.size = 0

def create():
    return DList()

def prepend(dlist, data):
    new_node = DListNode(data)
    
    if dlist.head is None:
        dlist.head = new_node
        dlist.tail = new_node
    else:
        new_node.next = dlist.head
        dlist.head.prev = new_node
        dlist.head = new_node
    
    dlist.size += 1

def append(dlist, data):
    new_node = DListNode(data)
    
    if dlist.tail is None:
        dlist.head = new_node
        dlist.tail = new_node
    else:
        new_node.prev = dlist.tail
        dlist.tail.next = new_node
        dlist.tail = new_node
    
    dlist.size += 1

def get(dlist, index):
    if index < 0 or index >= dlist.size:
        return None
    
    if index < dlist.size // 2:
        current = dlist.head
        for i in range(index):
            current = current.next
    else:
        current = dlist.tail
        for i in range(dlist.size - index - 1):
            current = current.prev
    
    return current.data

def remove(dlist, index):
    if index < 0 or index >= dlist.size:
        return None
    
    if index < dlist.size // 2:
        current = dlist.head
        for i in range(index):
            current = current.next
    else:
        current = dlist.tail
        for i in range(dlist.size - index - 1):
            current = current.prev
    
    data = current.data
    
    if current.prev:
        current.prev.next = current.next
    else:
        dlist.head = current.next
    
    if current.next:
        current.next.prev = current.prev
    else:
        dlist.tail = current.prev
    
    dlist.size -= 1
    return data

def length(dlist):
    return dlist.size

def is_empty(dlist):
    return dlist.size == 0

def clear(dlist):
    dlist.head = None
    dlist.tail = None
    dlist.size = 0

def foreach(dlist, func):
    current = dlist.head
    while current:
        func(current.data)
        current = current.next

def foreach_reverse(dlist, func):
    current = dlist.tail
    while current:
        func(current.data)
        current = current.prev

def find(dlist, data):
    current = dlist.head
    index = 0
    while current:
        if current.data == data:
            return index
        current = current.next
        index += 1
    return -1

def remove_first(dlist, data):
    current = dlist.head
    index = 0
    while current:
        if current.data == data:
            if current.prev:
                current.prev.next = current.next
            else:
                dlist.head = current.next
            
            if current.next:
                current.next.prev = current.prev
            else:
                dlist.tail = current.prev
            
            dlist.size -= 1
            return index
        current = current.next
        index += 1
    return -1

def remove_all(dlist, data):
    count = 0
    current = dlist.head
    while current:
        next_node = current.next
        if current.data == data:
            if current.prev:
                current.prev.next = current.next
            else:
                dlist.head = current.next
            
            if current.next:
                current.next.prev = current.prev
            else:
                dlist.tail = current.prev
            
            dlist.size -= 1
            count += 1
        current = next_node
    return count

def copy(dlist):
    new_list = create()
    current = dlist.head
    while current:
        append(new_list, current.data)
        current = current.next
    return new_list
