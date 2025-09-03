import unittest
from red_black_tree import create, clear, size, find, insert, delete, foreach, check
from random import choice, randint
import string

class TestRedBlackTree(unittest.TestCase):
    def setUp(self):
        self.tree = create(lambda x, y: x - y)
    
    def tearDown(self):
        self.assertTrue(check(self.tree))
    
    def test_empty_tree(self):
        self.assertEqual(size(self.tree), 0)
        self.assertIsNone(find(self.tree, 5))
        self.assertTrue(check(self.tree))
    
    def test_insert_find(self):
        self.assertIsNone(insert(self.tree, 10))
        self.assertIsNone(insert(self.tree, 5))
        self.assertIsNone(insert(self.tree, 15))
        self.assertEqual(size(self.tree), 3)
        self.assertEqual(find(self.tree, 10), 10)
        self.assertEqual(find(self.tree, 5), 5)
        self.assertEqual(find(self.tree, 15), 15)
        self.assertIsNone(find(self.tree, 20))
        self.assertTrue(check(self.tree))
    
    def test_insert_replace(self):
        insert(self.tree, 10)
        self.assertEqual(insert(self.tree, 10), 10)
        self.assertEqual(size(self.tree), 1)
        self.assertEqual(find(self.tree, 10), 10)
        self.assertTrue(check(self.tree))
    
    def test_delete(self):
        insert(self.tree, 10)
        insert(self.tree, 5)
        insert(self.tree, 15)
        self.assertEqual(delete(self.tree, 5), 5)
        self.assertEqual(size(self.tree), 2)
        self.assertIsNone(find(self.tree, 5))
        self.assertIsNone(delete(self.tree, 999))
        self.assertTrue(check(self.tree))
    
    def test_clear(self):
        insert(self.tree, 10)
        insert(self.tree, 5)
        clear(self.tree)
        self.assertEqual(size(self.tree), 0)
        self.assertIsNone(find(self.tree, 10))
        self.assertTrue(check(self.tree))
    
    def test_foreach(self):
        insert(self.tree, 10)
        insert(self.tree, 5)
        insert(self.tree, 15)
        insert(self.tree, 3)
        insert(self.tree, 7)
        result = []
        foreach(self.tree, lambda x: result.append(x))
        self.assertEqual(result, [3, 5, 7, 10, 15])
        self.assertTrue(check(self.tree))
    
    def test_string_keys(self):
        tree = create(lambda x, y: (x > y) - (x < y))
        insert(tree, "apple")
        insert(tree, "banana")
        insert(tree, "cherry")
        self.assertEqual(size(tree), 3)
        self.assertEqual(find(tree, "banana"), "banana")
        self.assertEqual(delete(tree, "apple"), "apple")
        self.assertEqual(size(tree), 2)
        self.assertTrue(check(tree))
    
    def test_large_tree(self):
        for i in range(1000):
            insert(self.tree, i)
        self.assertEqual(size(self.tree), 1000)
        self.assertEqual(find(self.tree, 500), 500)
        self.assertEqual(delete(self.tree, 500), 500)
        self.assertEqual(size(self.tree), 999)
        self.assertIsNone(find(self.tree, 500))
        self.assertTrue(check(self.tree))
    
    def test_random_operations(self):
        elements = set()
        
        for _ in range(1000):
            operation = randint(0, 2)
            if operation == 0:  # insert
                value = randint(0, 100)
                expected = value if value in elements else None
                result = insert(self.tree, value)
                elements.add(value)
                self.assertEqual(result, expected)
            elif operation == 1:  # find
                value = randint(0, 100)
                expected = value if value in elements else None
                result = find(self.tree, value)
                self.assertEqual(result, expected)
            elif operation == 2:  # delete
                value = randint(0, 100)
                expected = value if value in elements else None
                result = delete(self.tree, value)
                elements.discard(value)
                self.assertEqual(result, expected)
            
            self.assertEqual(size(self.tree), len(elements))
            self.assertTrue(check(self.tree))
    
    def test_rb_properties_after_operations(self):
        for i in range(100):
            insert(self.tree, i)
        self.assertEqual(size(self.tree), 100)
        self.assertTrue(check(self.tree))
        
        for i in range(0, 100, 2):
            delete(self.tree, i)
        self.assertEqual(size(self.tree), 50)
        self.assertTrue(check(self.tree))
        
        result = []
        foreach(self.tree, lambda x: result.append(x))
        self.assertEqual(result, list(range(1, 100, 2)))
        self.assertTrue(check(self.tree))

if __name__ == "__main__":
    unittest.main()
