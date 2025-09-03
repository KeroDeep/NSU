import unittest
from queue import queue_create, queue_free, queue_enqueue, queue_dequeue, queue_peek, queue_empty, queue_size, queue_clear

class TestQueue(unittest.TestCase):
    def test_create_free(self):
        queue = queue_create()
        self.assertEqual(queue.size, 0)
        self.assertIsNone(queue.front)
        self.assertIsNone(queue.rear)
        queue_free(queue)
    
    def test_enqueue_dequeue(self):
        queue = queue_create()
        queue_enqueue(queue, 10)
        queue_enqueue(queue, 20)
        queue_enqueue(queue, 30)
        
        self.assertEqual(queue_size(queue), 3)
        self.assertEqual(queue_dequeue(queue), 10)
        self.assertEqual(queue_dequeue(queue), 20)
        self.assertEqual(queue_dequeue(queue), 30)
        self.assertEqual(queue_empty(queue), True)
        
        queue_free(queue)
    
    def test_peek(self):
        queue = queue_create()
        queue_enqueue(queue, "hello")
        queue_enqueue(queue, "world")
        
        self.assertEqual(queue_peek(queue), "hello")
        self.assertEqual(queue_size(queue), 2)
        self.assertEqual(queue_peek(queue), "hello")
        
        queue_free(queue)
    
    def test_empty(self):
        queue = queue_create()
        self.assertEqual(queue_empty(queue), True)
        
        queue_enqueue(queue, 42)
        self.assertEqual(queue_empty(queue), False)
        
        queue_dequeue(queue)
        self.assertEqual(queue_empty(queue), True)
        
        queue_free(queue)
    
    def test_clear(self):
        queue = queue_create()
        for i in range(10):
            queue_enqueue(queue, i)
        
        self.assertEqual(queue_size(queue), 10)
        queue_clear(queue)
        self.assertEqual(queue_size(queue), 0)
        self.assertEqual(queue_empty(queue), True)
        self.assertIsNone(queue.front)
        self.assertIsNone(queue.rear)
        
        queue_free(queue)
    
    def test_fifo_order(self):
        queue = queue_create()
        items = ["first", "second", "third", "fourth"]
        
        for item in items:
            queue_enqueue(queue, item)
        
        for item in items:
            self.assertEqual(queue_dequeue(queue), item)
        
        self.assertEqual(queue_empty(queue), True)
        
        queue_free(queue)
    
    def test_none_values(self):
        queue = queue_create()
        queue_enqueue(queue, None)
        queue_enqueue(queue, 0)
        queue_enqueue(queue, "")
        
        self.assertEqual(queue_dequeue(queue), None)
        self.assertEqual(queue_dequeue(queue), 0)
        self.assertEqual(queue_dequeue(queue), "")
        
        queue_free(queue)
    
    def test_single_element(self):
        queue = queue_create()
        queue_enqueue(queue, "single")
        
        self.assertEqual(queue_peek(queue), "single")
        self.assertEqual(queue_dequeue(queue), "single")
        self.assertEqual(queue_empty(queue), True)
        self.assertIsNone(queue.front)
        self.assertIsNone(queue.rear)
        
        queue_free(queue)

if __name__ == "__main__":
    unittest.main()
