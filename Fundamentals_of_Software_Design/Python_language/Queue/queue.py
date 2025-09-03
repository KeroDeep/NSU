class QueueNode:
    __slots__ = ('data', 'next')
    def __init__(self, data):
        self.data = data
        self.next = None

class Queue:
    __slots__ = ('front', 'rear', 'size')
    def __init__(self):
        self.front = None
        self.rear = None
        self.size = 0

def queue_create():
    return Queue()

def queue_free(queue):
    queue_clear(queue)

def queue_enqueue(queue, data):
    new_node = QueueNode(data)
    if queue.rear:
        queue.rear.next = new_node
    else:
        queue.front = new_node
    queue.rear = new_node
    queue.size += 1

def queue_dequeue(queue):
    if not queue.front:
        return None
    temp = queue.front
    data = temp.data
    queue.front = queue.front.next
    if not queue.front:
        queue.rear = None
    queue.size -= 1
    return data

def queue_peek(queue):
    if not queue.front:
        return None
    return queue.front.data

def queue_empty(queue):
    return queue.size == 0

def queue_size(queue):
    return queue.size

def queue_clear(queue):
    current = queue.front
    while current:
        temp = current
        current = current.next
        temp.next = None
    queue.front = None
    queue.rear = None
    queue.size = 0
