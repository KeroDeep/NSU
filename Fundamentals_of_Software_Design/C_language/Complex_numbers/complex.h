#ifndef COMPLEX_H
#define COMPLEX_H

#include <math.h>

typedef struct _Complex {
    double re, im;
} Complex;

typedef struct _Polar {
    double radius, angle;
} Polar;

Complex complex_create(double re, double im);

Complex complex_add(Complex a, Complex b);

Complex complex_sub(Complex a, Complex b);

Complex complex_mul(Complex a, Complex b);

Complex complex_div(Complex a, Complex b);

Complex complex_conjugate(Complex z);

Complex complex_pow(Complex base, Complex exponent);

Complex complex_pow_int(Complex base, int n);

double complex_abs(Complex z);

Polar complex_to_polar(Complex z);

Complex polar_to_complex(Polar p);

#endif
