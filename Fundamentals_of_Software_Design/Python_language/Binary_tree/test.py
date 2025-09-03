import unittest
from binary_tree import create, clear, size, find, insert, delete, foreach
from random import choice, randint
import string

class TestBinaryTree(unittest.TestCase):
    def test_empty_tree(self):
        tree = create(lambda x, y: x - y)
        self.assertEqual(size(tree), 0)
        self.assertIsNone(find(tree, 5))
    
    def test_insert_find(self):
        tree = create(lambda x, y: x - y)
        self.assertIsNone(insert(tree, 10))
        self.assertIsNone(insert(tree, 5))
        self.assertIsNone(insert(tree, 15))
        self.assertEqual(size(tree), 3)
        self.assertEqual(find(tree, 10), 10)
        self.assertEqual(find(tree, 5), 5)
        self.assertEqual(find(tree, 15), 15)
        self.assertIsNone(find(tree, 20))
    
    def test_insert_replace(self):
        tree = create(lambda x, y: x - y)
        insert(tree, 10)
        self.assertEqual(insert(tree, 10), 10)
        self.assertEqual(size(tree), 1)
        self.assertEqual(find(tree, 10), 10)
    
    def test_delete(self):
        tree = create(lambda x, y: x - y)
        insert(tree, 10)
        insert(tree, 5)
        insert(tree, 15)
        self.assertEqual(delete(tree, 5), 5)
        self.assertEqual(size(tree), 2)
        self.assertIsNone(find(tree, 5))
        self.assertIsNone(delete(tree, 999))
    
    def test_clear(self):
        tree = create(lambda x, y: x - y)
        insert(tree, 10)
        insert(tree, 5)
        clear(tree)
        self.assertEqual(size(tree), 0)
        self.assertIsNone(find(tree, 10))
    
    def test_foreach(self):
        tree = create(lambda x, y: x - y)
        insert(tree, 10)
        insert(tree, 5)
        insert(tree, 15)
        insert(tree, 3)
        insert(tree, 7)
        result = []
        foreach(tree, lambda x: result.append(x))
        self.assertEqual(result, [3, 5, 7, 10, 15])
    
    def test_string_keys(self):
        tree = create(lambda x, y: (x > y) - (x < y))
        insert(tree, "apple")
        insert(tree, "banana")
        insert(tree, "cherry")
        self.assertEqual(size(tree), 3)
        self.assertEqual(find(tree, "banana"), "banana")
        self.assertEqual(delete(tree, "apple"), "apple")
        self.assertEqual(size(tree), 2)
    
    def test_large_tree(self):
        tree = create(lambda x, y: x - y)
        for i in range(1000):
            insert(tree, i)
        self.assertEqual(size(tree), 1000)
        self.assertEqual(find(tree, 500), 500)
        self.assertEqual(delete(tree, 500), 500)
        self.assertEqual(size(tree), 999)
        self.assertIsNone(find(tree, 500))
    
    def test_random_operations(self):
        tree = create(lambda x, y: x - y)
        elements = set()
        
        for _ in range(1000):
            operation = randint(0, 2)
            if operation == 0:  # insert
                value = randint(0, 100)
                expected = value if value in elements else None
                result = insert(tree, value)
                elements.add(value)
                self.assertEqual(result, expected)
            elif operation == 1:  # find
                value = randint(0, 100)
                expected = value if value in elements else None
                result = find(tree, value)
                self.assertEqual(result, expected)
            elif operation == 2:  # delete
                value = randint(0, 100)
                expected = value if value in elements else None
                result = delete(tree, value)
                elements.discard(value)
                self.assertEqual(result, expected)
            
            self.assertEqual(size(tree), len(elements))
    
    def test_unbalanced_tree(self):
        tree = create(lambda x, y: x - y)
        for i in range(100):
            insert(tree, i)
        result = []
        foreach(tree, lambda x: result.append(x))
        self.assertEqual(result, list(range(100)))
        self.assertEqual(size(tree), 100)

if __name__ == "__main__":
    unittest.main()
