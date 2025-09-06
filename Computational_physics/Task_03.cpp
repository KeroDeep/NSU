#include <iostream>
#include <cmath>

using namespace std;

int find_mantissa_bits_double() {
    double num = 1.0;
    int bits = 0;
    
    while (1.0 + num != 1.0) {
        num /= 2.0;
        bits++;
    }
    return bits;
}

double left_rectangle_method(double (*function)(double), double left_bound, double right_bound, int intervals_number) {
    double step = (right_bound - left_bound) / intervals_number;
    double square = 0.0;

    for (int i = 0; i < intervals_number; i++) {
        double x = left_bound + i * step;
        square += function(x) * step;
    }

    return square;
}

double right_rectangle_method(double (*function)(double), double left_bound, double right_bound, int intervals_number) {
    double step = (right_bound - left_bound) / intervals_number;
    double square = 0.0;

    for (int i = 1; i <= intervals_number; i++) {
        double x = left_bound + i * step;
        square += function(x) * step;
    }

    return square;
}

double medium_rectangle_method(double (*function)(double), double left_bound, double right_bound, int intervals_number) {
    double step = (right_bound - left_bound) / intervals_number;
    double square = 0.0;

    for (int i = 1; i <= intervals_number; i++) {
        double x = left_bound + (i + 0.5) * step;
        square += function(x) * step;
    }

    return square;
}

double trapezoidal_method(double (*function)(double), double left_bound, double right_bound, int intervals_number) {
    double step = (right_bound - left_bound) / intervals_number;
    double square = 0.0;

    for (int i = 0; i < intervals_number; i++) {
        double x_left = left_bound + i * step;
        double x_right = left_bound + (i + 1) * step;
        square += (function(x_left) + function(x_right)) * step / 2.0;
    }

    return square;
}

double simpson_method(double (*function)(double), double left_bound, double right_bound, int intervals_number) {
    if (intervals_number % 2 != 0) {
        intervals_number++;
    }
    
    double step = (right_bound - left_bound) / intervals_number;
    double square = function(left_bound) + function(right_bound);
    
    for (int i = 1; i < intervals_number; i++) {
        double x = left_bound + i * step;
        if (i % 2 == 0) {
            square += 2.0 * function(x);
        } else {
            square += 4.0 * function(x);
        }
    }
    
    return square * step / 3.0;
}

double main_integral(double x) {
    return 1.0 / (1.0 + pow(x, 2));
}

double error_integral(double t) {
    return 2.0 / sqrt(M_PI) * exp(-pow(t, 2));
}

double error_calculation(double exact, double approximate) {
    return fabs(exact - approximate);
}

int main() {
    cout.precision(find_mantissa_bits_double());

    const double a = -1.0;
    const double b = 1.0;
    const int accuracy = 100;
    const double exact_main = atan(1.0) - atan(-1.0);
    const double exact_error = 1.0;

    cout << endl;

    cout << "Main integral calculation:" << endl;
    
    cout << "Exact solution: " << exact_main << endl;
    cout << "Left rectangle method: " << left_rectangle_method(main_integral, a, b, accuracy) << endl;
    cout << "Right rectangle method: " << right_rectangle_method(main_integral, a, b, accuracy) << endl;
    cout << "Medium rectangle method: " << medium_rectangle_method(main_integral, a, b, accuracy) << endl;
    cout << "Trapezoidal method: " << trapezoidal_method(main_integral, a, b, accuracy) << endl;
    cout << "Simpson method: " << simpson_method(main_integral, a, b, accuracy) << endl;

    cout << endl;

    cout << "Error integral calculation:" << endl;
    
    cout << "Exact solution: " << exact_error << endl;
    cout << "Left rectangle method: " << left_rectangle_method(error_integral, a, b, accuracy) << endl;
    cout << "Right rectangle method: " << right_rectangle_method(error_integral, a, b, accuracy) << endl;
    cout << "Medium rectangle method: " << medium_rectangle_method(error_integral, a, b, accuracy) << endl;
    cout << "Trapezoidal method: " << trapezoidal_method(error_integral, a, b, accuracy) << endl;
    cout << "Simpson method: " << simpson_method(error_integral, a, b, accuracy) << endl;
    
    cout << endl;

    cout << "Methods accuracy for main integral:" << endl;

    cout << "Left rectangle method: " << error_calculation(exact_main, left_rectangle_method(main_integral, a, b, accuracy)) << endl;
    cout << "Right rectangle method: " << error_calculation(exact_main, right_rectangle_method(main_integral, a, b, accuracy)) << endl;
    cout << "Medium rectangle method: " << error_calculation(exact_main, medium_rectangle_method(main_integral, a, b, accuracy)) << endl;
    cout << "Trapezoidal method: " << error_calculation(exact_main, trapezoidal_method(main_integral, a, b, accuracy)) << endl;
    cout << "Simpson method: " << error_calculation(exact_main, simpson_method(main_integral, a, b, accuracy)) << endl;

    cout << endl;

    cout << "Methods accuracy for error integral:" << endl;

    cout << "Left rectangle method: " << error_calculation(exact_error, left_rectangle_method(error_integral, a, b, accuracy)) << endl;
    cout << "Right rectangle method: " << error_calculation(exact_error, right_rectangle_method(error_integral, a, b, accuracy)) << endl;
    cout << "Medium rectangle method: " << error_calculation(exact_error, medium_rectangle_method(error_integral, a, b, accuracy)) << endl;
    cout << "Trapezoidal method: " << error_calculation(exact_error, trapezoidal_method(error_integral, a, b, accuracy)) << endl;
    cout << "Simpson method: " << error_calculation(exact_error, simpson_method(error_integral, a, b, accuracy)) << endl;

    cout << endl;

    return 0;
}

