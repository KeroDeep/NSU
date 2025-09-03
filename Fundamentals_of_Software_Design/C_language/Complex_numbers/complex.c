#include <math.h>

#include "complex.h"

Complex complex_create(double re, double im) {
    Complex z = {re, im};
    return z;
}

Complex complex_add(Complex a, Complex b) {
    Complex result = {a.re + b.re, a.im + b.im};
    return result;
}

Complex complex_sub(Complex a, Complex b) {
    Complex result = {a.re - b.re, a.im - b.im};
    return result;
}

Complex complex_mul(Complex a, Complex b) {
    Complex result = {
        a.re * b.re - a.im * b.im,
        a.re * b.im + a.im * b.re
    };
    return result;
}

Complex complex_div(Complex a, Complex b) {
    double denominator = b.re * b.re + b.im * b.im;
    Complex result = {
        (a.re * b.re + a.im * b.im) / denominator,
        (a.im * b.re - a.re * b.im) / denominator
    };
    return result;
}

Complex complex_conjugate(Complex z) {
    Complex result = {z.re, -z.im};
    return result;
}

double complex_abs(Complex z) {
    return sqrt(z.re * z.re + z.im * z.im);
}

Polar complex_to_polar(Complex z) {
    Polar p;
    p.radius = complex_abs(z);
    p.angle = atan2(z.im, z.re);
    return p;
}

Complex polar_to_complex(Polar p) {
    Complex z;
    z.re = p.radius * cos(p.angle);
    z.im = p.radius * sin(p.angle);
    return z;
}

Complex complex_pow_int(Complex base, int n) {
    if (n == 0) {
        Complex one = {1.0, 0.0};
        return one;
    }
    if (n < 0) {
        Complex reciprocal = complex_div(complex_create(1.0, 0.0), base);
        return complex_pow_int(reciprocal, -n);
    }
    Complex result = base;
    for (int i = 1; i < n; i++) {
        result = complex_mul(result, base);
    }
    return result;
}

Complex complex_pow(Complex base, Complex exponent) {
    if (exponent.im == 0.0) {
        if (exponent.re == (int)exponent.re) {
            return complex_pow_int(base, (int)exponent.re);
        }
    }
    Polar base_polar = complex_to_polar(base);
    double log_r = log(base_polar.radius);
    double theta = base_polar.angle;
    double real_exp = exponent.re;
    double imag_exp = exponent.im;
    double new_radius = exp(log_r * real_exp - theta * imag_exp);
    double new_angle = log_r * imag_exp + theta * real_exp;
    Polar result_polar = {new_radius, new_angle};
    return polar_to_complex(result_polar);
}
