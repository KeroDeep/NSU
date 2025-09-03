import unittest
import math
from complex_numbers import create, add, subtract, multiply, divide, to_polar, from_polar, power_int, modulus, conjugate, power_complex, to_string, equals

class TestComplexNumbers(unittest.TestCase):
    def test_create(self):
        z = create(3, 4)
        self.assertEqual(z.re, 3)
        self.assertEqual(z.im, 4)
    
    def test_add(self):
        a = create(2, 3)
        b = create(1, -1)
        result = add(a, b)
        self.assertTrue(equals(result, create(3, 2)))
    
    def test_subtract(self):
        a = create(5, 7)
        b = create(2, 3)
        result = subtract(a, b)
        self.assertTrue(equals(result, create(3, 4)))
    
    def test_multiply(self):
        a = create(2, 3)
        b = create(1, -1)
        result = multiply(a, b)
        self.assertTrue(equals(result, create(5, 1)))
    
    def test_divide(self):
        a = create(6, 8)
        b = create(2, 0)
        result = divide(a, b)
        self.assertTrue(equals(result, create(3, 4)))
        
        a = create(1, -1)
        b = create(1, 1)
        result = divide(a, b)
        self.assertTrue(equals(result, create(0, -1)))
    
    def test_to_polar(self):
        z = create(1, 1)
        r, theta = to_polar(z)
        self.assertAlmostEqual(r, math.sqrt(2))
        self.assertAlmostEqual(theta, math.pi/4)
        
        z = create(0, 1)
        r, theta = to_polar(z)
        self.assertAlmostEqual(r, 1)
        self.assertAlmostEqual(theta, math.pi/2)
    
    def test_from_polar(self):
        r = 2
        theta = math.pi/3
        z = from_polar(r, theta)
        self.assertAlmostEqual(z.re, 1)
        self.assertAlmostEqual(z.im, math.sqrt(3))
    
    def test_power_int(self):
        z = create(1, 1)
        result = power_int(z, 2)
        self.assertTrue(equals(result, create(0, 2)))
        
        z = create(2, 0)
        result = power_int(z, 3)
        self.assertTrue(equals(result, create(8, 0)))
        
        z = create(0, 1)
        result = power_int(z, 4)
        self.assertTrue(equals(result, create(1, 0)))
    
    def test_modulus(self):
        z = create(3, 4)
        self.assertAlmostEqual(modulus(z), 5)
        
        z = create(1, 1)
        self.assertAlmostEqual(modulus(z), math.sqrt(2))
    
    def test_conjugate(self):
        z = create(3, 4)
        conj = conjugate(z)
        self.assertTrue(equals(conj, create(3, -4)))
        
        z = create(0, -5)
        conj = conjugate(z)
        self.assertTrue(equals(conj, create(0, 5)))
    
    def test_power_complex(self):
        z = create(2, 0)
        w = create(3, 0)
        result = power_complex(z, w)
        self.assertTrue(equals(result, create(8, 0)))
        
        z = create(math.e, 0)
        w = create(0, math.pi)
        result = power_complex(z, w)
        self.assertTrue(equals(result, create(-1, 0), epsilon=1e-10))
        
        z = create(-1, 0)
        w = create(0.5, 0)
        result = power_complex(z, w)
        self.assertTrue(equals(result, create(0, 1)) or equals(result, create(0, -1)))
    
    def test_to_string(self):
        z = create(3, 4)
        self.assertEqual(to_string(z), "3 + 4i")
        
        z = create(2, -5)
        self.assertEqual(to_string(z), "2 - 5i")
        
        z = create(0, 1)
        self.assertEqual(to_string(z), "0 + 1i")
        
        z = create(1, 0)
        self.assertEqual(to_string(z), "1 + 0i")
    
    def test_edge_cases(self):
        z = create(0, 0)
        self.assertAlmostEqual(modulus(z), 0)
        r, theta = to_polar(z)
        self.assertAlmostEqual(r, 0)
        
        conj = conjugate(z)
        self.assertTrue(equals(conj, create(0, 0)))
        
        result = power_int(z, 5)
        self.assertTrue(equals(result, create(0, 0)))
        
        w = create(0, 0)
        result = power_complex(z, w)
        self.assertTrue(equals(result, create(1, 0)))
    
    def test_euler_identity(self):
        z = create(math.e, 0)
        w = create(0, math.pi)
        result = power_complex(z, w)
        sum_result = add(result, create(1, 0))
        self.assertTrue(equals(sum_result, create(0, 0), epsilon=1e-10))

if __name__ == "__main__":
    unittest.main()
