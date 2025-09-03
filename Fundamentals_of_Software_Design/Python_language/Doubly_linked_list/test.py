import unittest
from dlist import create, prepend, append, get, remove, length, is_empty, clear, foreach, foreach_reverse, find, remove_first, remove_all, copy

class TestDList(unittest.TestCase):
    def test_empty_list(self):
        dlist = create()
        self.assertEqual(length(dlist), 0)
        self.assertTrue(is_empty(dlist))
        self.assertIsNone(get(dlist, 0))
        self.assertIsNone(get(dlist, -1))
        self.assertIsNone(get(dlist, 1))
    
    def test_prepend(self):
        dlist = create()
        prepend(dlist, "bb")
        prepend(dlist, "aa")
        
        self.assertEqual(length(dlist), 2)
        self.assertEqual(get(dlist, 0), "aa")
        self.assertEqual(get(dlist, 1), "bb")
    
    def test_append(self):
        dlist = create()
        append(dlist, "yy")
        append(dlist, "zz")
        
        self.assertEqual(length(dlist), 2)
        self.assertEqual(get(dlist, 0), "yy")
        self.assertEqual(get(dlist, 1), "zz")
    
    def test_mixed_operations(self):
        dlist = create()
        prepend(dlist, "bb")
        prepend(dlist, "aa")
        append(dlist, "yy")
        append(dlist, "zz")
        
        self.assertEqual(length(dlist), 4)
        self.assertEqual(get(dlist, 0), "aa")
        self.assertEqual(get(dlist, 1), "bb")
        self.assertEqual(get(dlist, 2), "yy")
        self.assertEqual(get(dlist, 3), "zz")
    
    def test_remove(self):
        dlist = create()
        append(dlist, "aa")
        append(dlist, "bb")
        append(dlist, "cc")
        append(dlist, "dd")
        
        self.assertEqual(remove(dlist, 1), "bb")
        self.assertEqual(length(dlist), 3)
        self.assertEqual(get(dlist, 0), "aa")
        self.assertEqual(get(dlist, 1), "cc")
        self.assertEqual(get(dlist, 2), "dd")
        
        self.assertEqual(remove(dlist, 0), "aa")
        self.assertEqual(length(dlist), 2)
        self.assertEqual(get(dlist, 0), "cc")
        self.assertEqual(get(dlist, 1), "dd")
        
        self.assertEqual(remove(dlist, 1), "dd")
        self.assertEqual(length(dlist), 1)
        self.assertEqual(get(dlist, 0), "cc")
    
    def test_foreach(self):
        dlist = create()
        append(dlist, "a")
        append(dlist, "b")
        append(dlist, "c")
        
        result = []
        foreach(dlist, lambda x: result.append(x))
        self.assertEqual(result, ["a", "b", "c"])
        
        result_reverse = []
        foreach_reverse(dlist, lambda x: result_reverse.append(x))
        self.assertEqual(result_reverse, ["c", "b", "a"])
    
    def test_find(self):
        dlist = create()
        append(dlist, "apple")
        append(dlist, "banana")
        append(dlist, "cherry")
        append(dlist, "banana")
        
        self.assertEqual(find(dlist, "banana"), 1)
        self.assertEqual(find(dlist, "cherry"), 2)
        self.assertEqual(find(dlist, "orange"), -1)
    
    def test_remove_first(self):
        dlist = create()
        append(dlist, "apple")
        append(dlist, "banana")
        append(dlist, "cherry")
        append(dlist, "banana")
        
        self.assertEqual(remove_first(dlist, "banana"), 1)
        self.assertEqual(length(dlist), 3)
        self.assertEqual(get(dlist, 0), "apple")
        self.assertEqual(get(dlist, 1), "cherry")
        self.assertEqual(get(dlist, 2), "banana")
        
        self.assertEqual(remove_first(dlist, "nonexistent"), -1)
    
    def test_remove_all(self):
        dlist = create()
        append(dlist, "apple")
        append(dlist, "banana")
        append(dlist, "cherry")
        append(dlist, "banana")
        append(dlist, "banana")
        
        self.assertEqual(remove_all(dlist, "banana"), 3)
        self.assertEqual(length(dlist), 2)
        self.assertEqual(get(dlist, 0), "apple")
        self.assertEqual(get(dlist, 1), "cherry")
        
        self.assertEqual(remove_all(dlist, "nonexistent"), 0)
    
    def test_copy(self):
        dlist = create()
        append(dlist, "a")
        append(dlist, "b")
        append(dlist, "c")
        
        copied = copy(dlist)
        self.assertEqual(length(copied), 3)
        self.assertEqual(get(copied, 0), "a")
        self.assertEqual(get(copied, 1), "b")
        self.assertEqual(get(copied, 2), "c")
        
        append(dlist, "d")
        self.assertEqual(length(dlist), 4)
        self.assertEqual(length(copied), 3)
    
    def test_clear(self):
        dlist = create()
        append(dlist, "a")
        append(dlist, "b")
        append(dlist, "c")
        
        clear(dlist)
        self.assertEqual(length(dlist), 0)
        self.assertTrue(is_empty(dlist))
        self.assertIsNone(dlist.head)
        self.assertIsNone(dlist.tail)
    
    def test_edge_cases(self):
        dlist = create()
        append(dlist, "single")
        self.assertEqual(get(dlist, 0), "single")
        self.assertEqual(remove(dlist, 0), "single")
        self.assertEqual(length(dlist), 0)
        
        prepend(dlist, "new_single")
        self.assertEqual(get(dlist, 0), "new_single")
        self.assertEqual(remove_first(dlist, "new_single"), 0)
        self.assertEqual(length(dlist), 0)

if __name__ == "__main__":
    unittest.main()
