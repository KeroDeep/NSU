import unittest
from hashtable import HashTable, ht_init, ht_destroy, ht_set, ht_get, ht_has, ht_delete, ht_traverse, ht_resize

class TestHashTable(unittest.TestCase):
    def setUp(self):
        self.ht = HashTable(0)
        self.destroyed = []
        
        def dtor(data):
            self.destroyed.append(data)
        
        ht_init(self.ht, 10, None, dtor)
    
    def tearDown(self):
        ht_destroy(self.ht)
    
    def test_init_destroy(self):
        self.assertEqual(self.ht.size, 10)
        self.assertIsNotNone(self.ht.hashfunc)
        self.assertIsNotNone(self.ht.dtor)
    
    def test_set_get(self):
        ht_set(self.ht, "test1", 100)
        ht_set(self.ht, "test2", 200)
        
        self.assertEqual(ht_get(self.ht, "test1"), 100)
        self.assertEqual(ht_get(self.ht, "test2"), 200)
        self.assertEqual(ht_has(self.ht, "test1"), True)
        self.assertEqual(ht_has(self.ht, "test2"), True)
        self.assertEqual(ht_has(self.ht, "nonexistent"), False)
    
    def test_set_overwrite(self):
        ht_set(self.ht, "test", 100)
        ht_set(self.ht, "test", 200)
        
        self.assertEqual(ht_get(self.ht, "test"), 200)
        self.assertEqual(self.destroyed, [100])
    
    def test_delete(self):
        ht_set(self.ht, "test", 100)
        self.assertEqual(ht_has(self.ht, "test"), True)
        
        ht_delete(self.ht, "test")
        self.assertEqual(ht_has(self.ht, "test"), False)
        self.assertEqual(self.destroyed, [100])
    
    def test_traverse(self):
        test_data = {"key1": 100, "key2": 200, "key3": 300}
        for key, value in test_data.items():
            ht_set(self.ht, key, value)
        
        visited = {}
        def visitor(key, data):
            visited[key] = data
        
        ht_traverse(self.ht, visitor)
        self.assertEqual(visited, test_data)
    
    def test_resize(self):
        for i in range(20):
            ht_set(self.ht, f"key{i}", i)
        
        ht_resize(self.ht, 20)
        self.assertEqual(self.ht.size, 20)
        
        for i in range(20):
            self.assertEqual(ht_get(self.ht, f"key{i}"), i)
    
    def test_collision(self):
        ht_set(self.ht, "hello", 100)
        ht_set(self.ht, "test", 200)
        
        self.assertEqual(ht_get(self.ht, "hello"), 100)
        self.assertEqual(ht_get(self.ht, "test"), 200)
        
        ht_delete(self.ht, "hello")
        self.assertEqual(ht_has(self.ht, "hello"), False)
        self.assertEqual(ht_has(self.ht, "test"), True)

if __name__ == "__main__":
    unittest.main()
