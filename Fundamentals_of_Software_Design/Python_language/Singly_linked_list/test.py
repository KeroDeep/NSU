import unittest
from slist import SListNode, length, prepend, get, remove, append, get_last, find, remove_first, remove_all, copy, concat, foreach, find_custom

class TestSList(unittest.TestCase):
    def test_empty_list(self):
        self.assertEqual(length(None), 0)
        self.assertIsNone(get(None, 0))
        self.assertIsNone(get_last(None))
        self.assertEqual(find(None, 5), -1)
    
    def test_prepend(self):
        lst = None
        lst = prepend(lst, 3)
        lst = prepend(lst, 2)
        lst = prepend(lst, 1)
        
        self.assertEqual(length(lst), 3)
        self.assertEqual(get(lst, 0), 1)
        self.assertEqual(get(lst, 1), 2)
        self.assertEqual(get(lst, 2), 3)
    
    def test_append(self):
        lst = None
        lst = append(lst, 1)
        lst = append(lst, 2)
        lst = append(lst, 3)
        
        self.assertEqual(length(lst), 3)
        self.assertEqual(get(lst, 0), 1)
        self.assertEqual(get(lst, 1), 2)
        self.assertEqual(get(lst, 2), 3)
        self.assertEqual(get_last(lst), 3)
    
    def test_remove(self):
        lst = None
        lst = append(lst, 1)
        lst = append(lst, 2)
        lst = append(lst, 3)
        
        data, lst = remove(lst, 1)
        self.assertEqual(data, 2)
        self.assertEqual(length(lst), 2)
        self.assertEqual(get(lst, 0), 1)
        self.assertEqual(get(lst, 1), 3)
        
        data, lst = remove(lst, 0)
        self.assertEqual(data, 1)
        self.assertEqual(length(lst), 1)
        self.assertEqual(get(lst, 0), 3)
        
        data, lst = remove(lst, 0)
        self.assertEqual(data, 3)
        self.assertEqual(length(lst), 0)
        self.assertIsNone(lst)
    
    def test_find(self):
        lst = None
        lst = append(lst, 1)
        lst = append(lst, 2)
        lst = append(lst, 3)
        lst = append(lst, 2)
        
        self.assertEqual(find(lst, 2), 1)
        self.assertEqual(find(lst, 4), -1)
        self.assertEqual(find(lst, 3), 2)
    
    def test_remove_first(self):
        lst = None
        lst = append(lst, 1)
        lst = append(lst, 2)
        lst = append(lst, 3)
        lst = append(lst, 2)
        
        lst = remove_first(lst, 2)
        self.assertEqual(length(lst), 3)
        self.assertEqual(get(lst, 0), 1)
        self.assertEqual(get(lst, 1), 3)
        self.assertEqual(get(lst, 2), 2)
        
        lst = remove_first(lst, 1)
        self.assertEqual(length(lst), 2)
        self.assertEqual(get(lst, 0), 3)
        self.assertEqual(get(lst, 1), 2)
    
    def test_remove_all(self):
        lst = None
        lst = append(lst, 2)
        lst = append(lst, 1)
        lst = append(lst, 2)
        lst = append(lst, 3)
        lst = append(lst, 2)
        
        lst = remove_all(lst, 2)
        self.assertEqual(length(lst), 2)
        self.assertEqual(get(lst, 0), 1)
        self.assertEqual(get(lst, 1), 3)
        
        lst = remove_all(lst, 5)
        self.assertEqual(length(lst), 2)
    
    def test_copy(self):
        lst = None
        lst = append(lst, 1)
        lst = append(lst, 2)
        lst = append(lst, 3)
        
        copied = copy(lst)
        self.assertEqual(length(copied), 3)
        self.assertEqual(get(copied, 0), 1)
        self.assertEqual(get(copied, 1), 2)
        self.assertEqual(get(copied, 2), 3)
        
        copied = prepend(copied, 0)
        self.assertEqual(length(lst), 3)
        self.assertEqual(length(copied), 4)
    
    def test_concat(self):
        lst1 = None
        lst1 = append(lst1, 1)
        lst1 = append(lst1, 2)
        
        lst2 = None
        lst2 = append(lst2, 3)
        lst2 = append(lst2, 4)
        
        result = concat(lst1, lst2)
        self.assertEqual(length(result), 4)
        self.assertEqual(get(result, 0), 1)
        self.assertEqual(get(result, 1), 2)
        self.assertEqual(get(result, 2), 3)
        self.assertEqual(get(result, 3), 4)
    
    def test_foreach(self):
        lst = None
        lst = append(lst, 1)
        lst = append(lst, 2)
        lst = append(lst, 3)
        
        result = []
        foreach(lst, lambda x: result.append(x))
        self.assertEqual(result, [1, 2, 3])
    
    def test_find_custom(self):
        lst = None
        lst = append(lst, 1)
        lst = append(lst, 4)
        lst = append(lst, 9)
        lst = append(lst, 16)
        
        def is_even(x):
            return x % 2 == 0
        
        def is_square(x):
            return x in [1, 4, 9, 16, 25]
        
        value, index = find_custom(lst, is_even)
        self.assertEqual(value, 4)
        self.assertEqual(index, 1)
        
        value, index = find_custom(lst, is_square)
        self.assertEqual(value, 1)
        self.assertEqual(index, 0)
        
        value, index = find_custom(lst, lambda x: x > 20)
        self.assertIsNone(value)
        self.assertEqual(index, -1)
    
    def test_complex_operations(self):
        lst = None
        for i in range(5):
            lst = prepend(lst, i)
        
        self.assertEqual(length(lst), 5)
        self.assertEqual(get(lst, 0), 4)
        self.assertEqual(get_last(lst), 0)
        
        lst = remove_all(lst, 2)
        self.assertEqual(length(lst), 4)
        self.assertEqual(find(lst, 2), -1)
        
        copied = copy(lst)
        lst = prepend(lst, 10)
        self.assertEqual(get(copied, 0), 4)
        self.assertEqual(get(lst, 0), 10)

if __name__ == "__main__":
    unittest.main()
