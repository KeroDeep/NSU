import math

class Complex:
    __slots__ = ('re', 'im')
    def __init__(self, re, im):
        self.re = re
        self.im = im

def create(re, im):
    return Complex(re, im)

def add(a, b):
    return Complex(a.re + b.re, a.im + b.im)

def subtract(a, b):
    return Complex(a.re - b.re, a.im - b.im)

def multiply(a, b):
    re = a.re * b.re - a.im * b.im
    im = a.re * b.im + a.im * b.re
    return Complex(re, im)

def divide(a, b):
    denominator = b.re * b.re + b.im * b.im
    re = (a.re * b.re + a.im * b.im) / denominator
    im = (a.im * b.re - a.re * b.im) / denominator
    return Complex(re, im)

def to_polar(z):
    r = math.sqrt(z.re * z.re + z.im * z.im)
    theta = math.atan2(z.im, z.re)
    return r, theta

def from_polar(r, theta):
    re = r * math.cos(theta)
    im = r * math.sin(theta)
    return Complex(re, im)

def power_int(z, n):
    if n == 0:
        return Complex(1, 0)
    
    r, theta = to_polar(z)
    r_pow = r ** n
    theta_pow = theta * n
    return from_polar(r_pow, theta_pow)

def modulus(z):
    return math.sqrt(z.re * z.re + z.im * z.im)

def conjugate(z):
    return Complex(z.re, -z.im)

def power_complex(z, w):
    if z.re == 0 and z.im == 0:
        if w.re == 0 and w.im == 0:
            return Complex(1, 0)
        return Complex(0, 0)
    
    r, theta = to_polar(z)
    ln_r = math.log(r)
    
    re_exp = math.exp(ln_r * w.re - theta * w.im)
    im_exp = ln_r * w.im + theta * w.re
    
    re_result = re_exp * math.cos(im_exp)
    im_result = re_exp * math.sin(im_exp)
    
    return Complex(re_result, im_result)

def to_string(z):
    if z.im >= 0:
        return f"{z.re} + {z.im}i"
    else:
        return f"{z.re} - {-z.im}i"

def equals(a, b, epsilon=1e-10):
    return abs(a.re - b.re) < epsilon and abs(a.im - b.im) < epsilon
