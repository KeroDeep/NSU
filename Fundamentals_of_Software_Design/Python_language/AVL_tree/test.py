from avl_tree import create, clear, size, find, insert, delete, foreach

def run_tests():
    passed = 0
    total = 0

    def test_empty_tree():
        nonlocal passed, total
        total += 1
        tree = create(lambda x, y: x - y)
        assert size(tree) == 0
        assert find(tree, 5) is None
        print("Test empty tree: PASSED")
        return 1

    def test_insert_find():
        nonlocal passed, total
        total += 1
        tree = create(lambda x, y: x - y)
        assert insert(tree, 10) is None
        assert insert(tree, 5) is None
        assert insert(tree, 15) is None
        assert size(tree) == 3
        assert find(tree, 10) == 10
        assert find(tree, 5) == 5
        assert find(tree, 15) == 15
        assert find(tree, 20) is None
        print("Test insert/find: PASSED")
        return 1

    def test_insert_replace():
        nonlocal passed, total
        total += 1
        tree = create(lambda x, y: x - y)
        insert(tree, 10)
        assert insert(tree, 10) == 10
        assert size(tree) == 1
        assert find(tree, 10) == 10
        print("Test insert replace: PASSED")
        return 1

    def test_delete():
        nonlocal passed, total
        total += 1
        tree = create(lambda x, y: x - y)
        insert(tree, 10)
        insert(tree, 5)
        insert(tree, 15)
        assert delete(tree, 5) == 5
        assert size(tree) == 2
        assert find(tree, 5) is None
        assert delete(tree, 999) is None
        print("Test delete: PASSED")
        return 1

    def test_clear():
        nonlocal passed, total
        total += 1
        tree = create(lambda x, y: x - y)
        insert(tree, 10)
        insert(tree, 5)
        clear(tree)
        assert size(tree) == 0
        assert find(tree, 10) is None
        print("Test clear: PASSED")
        return 1

    def test_foreach():
        nonlocal passed, total
        total += 1
        tree = create(lambda x, y: x - y)
        insert(tree, 10)
        insert(tree, 5)
        insert(tree, 15)
        insert(tree, 3)
        insert(tree, 7)
        result = []
        foreach(tree, lambda x: result.append(x))
        assert result == [3, 5, 7, 10, 15]
        print("Test foreach: PASSED")
        return 1

    def test_avl_balance():
        nonlocal passed, total
        total += 1
        tree = create(lambda x, y: x - y)
        for i in range(10):
            insert(tree, i)
        assert size(tree) == 10
        result = []
        foreach(tree, lambda x: result.append(x))
        assert result == list(range(10))
        print("Test AVL balance: PASSED")
        return 1

    def test_string_keys():
        nonlocal passed, total
        total += 1
        tree = create(lambda x, y: (x > y) - (x < y))
        insert(tree, "apple")
        insert(tree, "banana")
        insert(tree, "cherry")
        assert size(tree) == 3
        assert find(tree, "banana") == "banana"
        assert delete(tree, "apple") == "apple"
        assert size(tree) == 2
        print("Test string keys: PASSED")
        return 1

    try:
        passed += test_empty_tree()
        passed += test_insert_find()
        passed += test_insert_replace()
        passed += test_delete()
        passed += test_clear()
        passed += test_foreach()
        passed += test_avl_balance()
        passed += test_string_keys()
        print(f"\nAll tests passed! {passed}/{total} tests successful.")
    except AssertionError as e:
        print(f"Test failed: {e}")
        print(f"Tests passed: {passed}/{total}")

if __name__ == "__main__":
    run_tests()
