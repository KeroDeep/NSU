#include <iostream>
#include <cmath>

using namespace std;

float find_machine_epsilon_single() {
    float epsilon = 1.0f;
    while (1.0f + epsilon / 2.0f != 1.0f) {
        epsilon /= 2.0f;
    }
    return epsilon;
}

double find_machine_epsilon_double() {
    double epsilon = 1.0;
    while (1.0 + epsilon / 2.0 != 1.0) {
        epsilon /= 2.0;
    }
    return epsilon;
}

int find_mantissa_bits_single() {
    float num = 1.0f;
    int bits = 0;
    
    while (1.0f + num != 1.0f) {
        num /= 2.0f;
        bits++;
    }
    return bits;
}

int find_mantissa_bits_double() {
    double num = 1.0;
    int bits = 0;
    
    while (1.0 + num != 1.0) {
        num /= 2.0;
        bits++;
    }
    return bits;
}

int find_min_exponent_single() {
    float num = 1.0f;
    int exponent = 0;
    
    while (num / 2.0f != 0.0f) {
        num /= 2.0f;
        exponent--;
    }
    return exponent;
}

int find_max_exponent_single() {
    float num = 1.0f;
    int exponent = 0;
    
    while (num * 2.0f < INFINITY) {
        num *= 2.0f;
        exponent++;
    }
    return exponent;
}

int find_min_exponent_double() {
    double num = 1.0;
    int exponent = 0;
    
    while (num / 2.0 != 0.0) {
        num /= 2.0;
        exponent--;
    }
    return exponent;
}

int find_max_exponent_double() {
    double num = 1.0;
    int exponent = 0;
    
    while (num * 2.0 < INFINITY) {
        num *= 2.0;
        exponent++;
    }
    return exponent;
}

float summation_forward_large_to_small_single() {
    float summation = 0.0f;
    for (int n = 10000; n >= 1; --n) {
        summation += (n % 2 == 0 ? 1.0f : -1.0f) / (float)n;
    }
    return summation;
}

float summation_forward_small_to_large_single() {
    float summation = 0.0f;
    for (int n = 1; n <= 10000; ++n) {
        summation += (n % 2 == 0 ? 1.0f : -1.0f) / (float)n;
    }
    return summation;
}

float summation_separate_large_to_small_single() {
    float positive_summation = 0.0f;
    float negative_summation = 0.0f;
    
    for (int n = 10000; n >= 1; --n) {
        if (n % 2 == 0) {
            positive_summation += 1.0f / (float)n;
        } else {
            negative_summation += 1.0f / (float)n;
        }
    }
    return positive_summation - negative_summation;
}

float summation_separate_small_to_large_single() {
    float positive_summation = 0.0f;
    float negative_summation = 0.0f;
    
    for (int n = 1; n <= 10000; ++n) {
        if (n % 2 == 0) {
            positive_summation += 1.0f / (float)n;
        } else {
            negative_summation += 1.0f / (float)n;
        }
    }
    return positive_summation - negative_summation;
}

double summation_forward_large_to_small_double() {
    float summation = 0.0f;
    for (int n = 10000; n >= 1; --n) {
        summation += (n % 2 == 0 ? 1.0f : -1.0f) / (double)n;
    }
    return summation;
}

double summation_forward_small_to_large_double() {
    double summation = 0.0f;
    for (int n = 1; n <= 10000; ++n) {
        summation += (n % 2 == 0 ? 1.0f : -1.0f) / (double)n;
    }
    return summation;
}

double summation_separate_large_to_small_double() {
    double positive_summation = 0.0f;
    double negative_summation = 0.0f;
    
    for (int n = 10000; n >= 1; --n) {
        if (n % 2 == 0) {
            positive_summation += 1.0f / (double)n;
        } else {
            negative_summation += 1.0f / (double)n;
        }
    }
    return positive_summation - negative_summation;
}

double summation_separate_small_to_large_double() {
    double positive_summation = 0.0f;
    double negative_summation = 0.0f;
    
    for (int n = 1; n <= 10000; ++n) {
        if (n % 2 == 0) {
            positive_summation += 1.0f / (double)n;
        } else {
            negative_summation += 1.0f / (double)n;
        }
    }
    return positive_summation - negative_summation;
}

int main() {
    cout.precision(find_mantissa_bits_double());

    cout << endl;

    cout << "a) Machine precision characteristics:" << endl;
    
    cout << "Single epsilon: " << find_machine_epsilon_single() << endl;
    cout << "Double epsilon: " << find_machine_epsilon_double() << endl;
    cout << "Single mantissa bits: " << find_mantissa_bits_single() << endl;
    cout << "Double mantissa bits: " << find_mantissa_bits_double() << endl;
    cout << "Single min exponent: " << find_min_exponent_single() << endl;
    cout << "Single max exponent: " << find_max_exponent_single() << endl;
    cout << "Double min exponent: " << find_min_exponent_double() << endl;
    cout << "Double max exponent: " << find_max_exponent_double() << endl;
    
    cout << endl;

    cout << "b) Series summation results:" << endl;
    
    cout << "Forward large to small for single: " << summation_forward_large_to_small_single() << endl;
    cout << "Forward small to large for single: " << summation_forward_small_to_large_single() << endl;
    cout << "Separate large to small for single: " << summation_separate_large_to_small_single() << endl;
    cout << "Separate small to large for single: " << summation_separate_small_to_large_single() << endl;
    cout << "Forward large to small for double: " << summation_forward_large_to_small_double() << endl;
    cout << "Forward small to large for double: " << summation_forward_small_to_large_double() << endl;
    cout << "Separate large to small for double: " << summation_separate_large_to_small_double() << endl;
    cout << "Separate small to large for double: " << summation_separate_small_to_large_double() << endl;
    
    cout << endl;

    return 0;
}
