class SListNode:
    __slots__ = ('data', 'next')
    def __init__(self, data):
        self.data = data
        self.next = None

def length(lst):
    count = 0
    current = lst
    while current is not None:
        count += 1
        current = current.next
    return count

def prepend(lst, data):
    new_node = SListNode(data)
    new_node.next = lst
    return new_node

def get(lst, index):
    current = lst
    count = 0
    while current is not None:
        if count == index:
            return current.data
        count += 1
        current = current.next
    return None

def remove(lst, index):
    if lst is None or index < 0:
        return None, lst
    
    if index == 0:
        data = lst.data
        return data, lst.next
    
    current = lst
    prev = None
    count = 0
    
    while current is not None and count < index:
        prev = current
        current = current.next
        count += 1
    
    if current is None:
        return None, lst
    
    data = current.data
    prev.next = current.next
    return data, lst

def append(lst, data):
    new_node = SListNode(data)
    
    if lst is None:
        return new_node
    
    current = lst
    while current.next is not None:
        current = current.next
    
    current.next = new_node
    return lst

def get_last(lst):
    if lst is None:
        return None
    
    current = lst
    while current.next is not None:
        current = current.next
    
    return current.data

def find(lst, data):
    current = lst
    index = 0
    while current is not None:
        if current.data == data:
            return index
        index += 1
        current = current.next
    return -1

def remove_first(lst, data):
    if lst is None:
        return None
    
    if lst.data == data:
        return lst.next
    
    current = lst
    prev = None
    
    while current is not None:
        if current.data == data:
            prev.next = current.next
            return lst
        prev = current
        current = current.next
    
    return lst

def remove_all(lst, data):
    if lst is None:
        return None
    
    while lst is not None and lst.data == data:
        lst = lst.next
    
    if lst is None:
        return None
    
    current = lst
    while current.next is not None:
        if current.next.data == data:
            current.next = current.next.next
        else:
            current = current.next
    
    return lst

def copy(lst):
    if lst is None:
        return None
    
    new_head = SListNode(lst.data)
    current_old = lst.next
    current_new = new_head
    
    while current_old is not None:
        current_new.next = SListNode(current_old.data)
        current_new = current_new.next
        current_old = current_old.next
    
    return new_head

def concat(lst1, lst2):
    if lst1 is None:
        return lst2
    if lst2 is None:
        return lst1
    
    new_list = copy(lst1)
    current = new_list
    
    while current.next is not None:
        current = current.next
    
    current.next = copy(lst2)
    return new_list

def foreach(lst, func):
    current = lst
    while current is not None:
        func(current.data)
        current = current.next

def find_custom(lst, predicate):
    current = lst
    index = 0
    while current is not None:
        if predicate(current.data):
            return current.data, index
        index += 1
        current = current.next
    return None, -1
