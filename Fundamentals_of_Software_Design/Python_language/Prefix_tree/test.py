import unittest
from prefix_tree import trie_create, trie_free, trie_has, trie_get, trie_set, trie_update, trie_delete, trie_traverse

class TestTrie(unittest.TestCase):
    def test_empty_trie(self):
        tr = trie_create(None)
        self.assertEqual(trie_has(tr, "test"), 0)
        self.assertIsNone(trie_get(tr, "test"))
        trie_free(tr)
    
    def test_basic_operations(self):
        tr = trie_create(None)
        
        self.assertEqual(trie_set(tr, "hello", 42), 1)
        self.assertEqual(trie_has(tr, "hello"), 1)
        self.assertEqual(trie_get(tr, "hello"), 42)
        
        self.assertEqual(trie_set(tr, "hello", 100), 1)
        self.assertEqual(trie_get(tr, "hello"), 100)
        
        self.assertEqual(trie_has(tr, "hell"), 0)
        self.assertEqual(trie_has(tr, "helloworld"), 0)
        
        trie_free(tr)
    
    def test_multiple_keys(self):
        tr = trie_create(None)
        
        trie_set(tr, "apple", 1)
        trie_set(tr, "app", 2)
        trie_set(tr, "banana", 3)
        trie_set(tr, "band", 4)
        
        self.assertEqual(trie_get(tr, "apple"), 1)
        self.assertEqual(trie_get(tr, "app"), 2)
        self.assertEqual(trie_get(tr, "banana"), 3)
        self.assertEqual(trie_get(tr, "band"), 4)
        self.assertEqual(trie_has(tr, "ap"), 0)
        self.assertEqual(trie_has(tr, "ban"), 0)
        
        trie_free(tr)
    
    def test_delete(self):
        tr = trie_create(None)
        
        trie_set(tr, "apple", 1)
        trie_set(tr, "app", 2)
        
        self.assertEqual(trie_delete(tr, "apple"), 1)
        self.assertEqual(trie_has(tr, "apple"), 0)
        self.assertEqual(trie_has(tr, "app"), 1)
        
        self.assertEqual(trie_delete(tr, "nonexistent"), 0)
        
        trie_free(tr)
    
    def test_update(self):
        def sum_updater(old, next):
            return (old or 0) + next
        
        tr = trie_create(None)
        
        self.assertEqual(trie_update(tr, "count", sum_updater, 5), 1)
        self.assertEqual(trie_get(tr, "count"), 5)
        
        self.assertEqual(trie_update(tr, "count", sum_updater, 3), 1)
        self.assertEqual(trie_get(tr, "count"), 8)
        
        trie_free(tr)
    
    def test_traverse(self):
        tr = trie_create(None)
        
        test_data = {
            "apple": 1,
            "app": 2,
            "banana": 3,
            "band": 4,
            "cat": 5
        }
        
        for key, value in test_data.items():
            trie_set(tr, key, value)
        
        result = {}
        def collector(key, data, user):
            result[key] = data
        
        trie_traverse(tr, collector, None)
        self.assertEqual(result, test_data)
        
        trie_free(tr)
    
    def test_destructor(self):
        destroyed = []
        def dtor(data):
            destroyed.append(data)
        
        tr = trie_create(dtor)
        
        trie_set(tr, "test", 42)
        trie_set(tr, "test", 100)
        self.assertEqual(destroyed, [42])
        
        trie_set(tr, "other", 200)
        trie_delete(tr, "other")
        self.assertEqual(destroyed, [42, 200])
        
        trie_free(tr)
        self.assertEqual(destroyed, [42, 200, 100])
    
    def test_complex_scenario(self):
        tr = trie_create(None)
        
        words = ["hello", "hell", "he", "world", "word", "wor", "test", "testing"]
        for i, word in enumerate(words):
            trie_set(tr, word, i)
        
        for i, word in enumerate(words):
            self.assertEqual(trie_get(tr, word), i)
            self.assertEqual(trie_has(tr, word), 1)
        
        self.assertEqual(trie_has(tr, "hel"), 0)
        self.assertEqual(trie_has(tr, "worl"), 0)
        self.assertEqual(trie_has(tr, "testin"), 0)
        
        self.assertEqual(trie_delete(tr, "hell"), 1)
        self.assertEqual(trie_has(tr, "hell"), 0)
        self.assertEqual(trie_has(tr, "hello"), 1)
        
        all_data = {}
        def collect_all(key, data, user):
            all_data[key] = data
        
        trie_traverse(tr, collect_all, None)
        expected = {word: i for i, word in enumerate(words) if word != "hell"}
        self.assertEqual(all_data, expected)
        
        trie_free(tr)

if __name__ == "__main__":
    unittest.main()
