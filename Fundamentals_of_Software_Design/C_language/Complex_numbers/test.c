#include <assert.h>
#include <stdio.h>
#include <math.h>

#include "complex.h"

#define EPSILON 1e-10

void assert_complex_equal(Complex a, Complex b) {
    assert(fabs(a.re - b.re) < EPSILON);
    assert(fabs(a.im - b.im) < EPSILON);
}

void test_create() {
    Complex z = complex_create(3.0, 4.0);
    assert(z.re == 3.0);
    assert(z.im == 4.0);
    printf("Test create passed\n");
}

void test_add() {
    Complex a = complex_create(1.0, 2.0);
    Complex b = complex_create(3.0, 4.0);
    Complex result = complex_add(a, b);
    assert_complex_equal(result, complex_create(4.0, 6.0));
    printf("Test add passed\n");
}

void test_sub() {
    Complex a = complex_create(5.0, 6.0);
    Complex b = complex_create(2.0, 3.0);
    Complex result = complex_sub(a, b);
    assert_complex_equal(result, complex_create(3.0, 3.0));
    printf("Test sub passed\n");
}

void test_mul() {
    Complex a = complex_create(1.0, 2.0);
    Complex b = complex_create(3.0, 4.0);
    Complex result = complex_mul(a, b);
    assert_complex_equal(result, complex_create(-5.0, 10.0));
    printf("Test mul passed\n");
}

void test_div() {
    Complex a = complex_create(1.0, 2.0);
    Complex b = complex_create(3.0, 4.0);
    Complex result = complex_div(a, b);
    assert_complex_equal(result, complex_create(0.44, 0.08));
    printf("Test div passed\n");
}

void test_conjugate() {
    Complex z = complex_create(3.0, 4.0);
    Complex conj = complex_conjugate(z);
    assert_complex_equal(conj, complex_create(3.0, -4.0));
    printf("Test conjugate passed\n");
}

void test_abs() {
    Complex z = complex_create(3.0, 4.0);
    double abs_val = complex_abs(z);
    assert(fabs(abs_val - 5.0) < EPSILON);
    printf("Test abs passed\n");
}

void test_polar_conversion() {
    Complex z = complex_create(1.0, 1.0);
    Polar p = complex_to_polar(z);
    assert(fabs(p.radius - sqrt(2.0)) < EPSILON);
    assert(fabs(p.angle - M_PI/4) < EPSILON);
    Complex z_back = polar_to_complex(p);
    assert_complex_equal(z_back, z);
    printf("Test polar conversion passed\n");
}

void test_pow_int() {
    Complex z = complex_create(1.0, 1.0);
    Complex z_squared = complex_pow_int(z, 2);
    assert_complex_equal(z_squared, complex_create(0.0, 2.0));
    Complex z_cubed = complex_pow_int(z, 3);
    assert_complex_equal(z_cubed, complex_create(-2.0, 2.0));
    Complex z_neg = complex_pow_int(z, -1);
    Complex expected_reciprocal = complex_div(complex_create(1.0, 0.0), z);
    assert_complex_equal(z_neg, expected_reciprocal);
    printf("Test pow_int passed\n");
}

void test_pow_complex() {
    Complex base = complex_create(2.0, 0.0);
    Complex exponent = complex_create(3.0, 0.0);
    Complex result = complex_pow(base, exponent);
    assert_complex_equal(result, complex_create(8.0, 0.0));
    base = complex_create(0.0, 1.0);
    exponent = complex_create(2.0, 0.0);
    result = complex_pow(base, exponent);
    assert_complex_equal(result, complex_create(-1.0, 0.0));
    base = complex_create(1.0, 1.0);
    exponent = complex_create(0.5, 0.0);
    result = complex_pow(base, exponent);
    Complex squared = complex_mul(result, result);
    assert_complex_equal(squared, base);
    printf("Test pow_complex passed\n");
}

void test_euler_formula() {
    Complex i = complex_create(0.0, 1.0);
    Complex pi_i = complex_mul(complex_create(M_PI, 0.0), i);
    Complex e_pi_i = complex_pow(complex_create(M_E, 0.0), pi_i);
    assert_complex_equal(e_pi_i, complex_create(-1.0, 0.0));
    printf("Test Euler formula passed\n");
}

int main() {
    test_create();
    test_add();
    test_sub();
    test_mul();
    test_div();
    test_conjugate();
    test_abs();
    test_polar_conversion();
    test_pow_int();
    test_pow_complex();
    test_euler_formula();
    printf("All Complex tests passed successfully!\n");
    return 0;
}
