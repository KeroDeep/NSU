import unittest
from stack import stack_create, stack_free, stack_push, stack_pop, stack_peek, stack_empty, stack_size, stack_clear

class TestStack(unittest.TestCase):
    def test_create_free(self):
        stack = stack_create()
        self.assertEqual(stack.size, 0)
        self.assertEqual(stack.capacity, 16)
        stack_free(stack)
    
    def test_push_pop(self):
        stack = stack_create()
        stack_push(stack, 10)
        stack_push(stack, 20)
        stack_push(stack, 30)
        
        self.assertEqual(stack_size(stack), 3)
        self.assertEqual(stack_pop(stack), 30)
        self.assertEqual(stack_pop(stack), 20)
        self.assertEqual(stack_pop(stack), 10)
        self.assertEqual(stack_empty(stack), True)
        
        stack_free(stack)
    
    def test_peek(self):
        stack = stack_create()
        stack_push(stack, "hello")
        stack_push(stack, "world")
        
        self.assertEqual(stack_peek(stack), "world")
        self.assertEqual(stack_size(stack), 2)
        self.assertEqual(stack_peek(stack), "world")
        
        stack_free(stack)
    
    def test_empty(self):
        stack = stack_create()
        self.assertEqual(stack_empty(stack), True)
        
        stack_push(stack, 42)
        self.assertEqual(stack_empty(stack), False)
        
        stack_pop(stack)
        self.assertEqual(stack_empty(stack), True)
        
        stack_free(stack)
    
    def test_clear(self):
        stack = stack_create()
        for i in range(10):
            stack_push(stack, i)
        
        self.assertEqual(stack_size(stack), 10)
        stack_clear(stack)
        self.assertEqual(stack_size(stack), 0)
        self.assertEqual(stack_empty(stack), True)
        
        stack_free(stack)
    
    def test_resize(self):
        stack = stack_create()
        for i in range(20):
            stack_push(stack, i)
        
        self.assertEqual(stack_size(stack), 20)
        self.assertEqual(stack.capacity, 32)
        
        for i in range(19, -1, -1):
            self.assertEqual(stack_pop(stack), i)
        
        self.assertEqual(stack_empty(stack), True)
        
        stack_free(stack)
    
    def test_none_values(self):
        stack = stack_create()
        stack_push(stack, None)
        stack_push(stack, 0)
        stack_push(stack, "")
        
        self.assertEqual(stack_pop(stack), "")
        self.assertEqual(stack_pop(stack), 0)
        self.assertEqual(stack_pop(stack), None)
        
        stack_free(stack)

if __name__ == "__main__":
    unittest.main()
