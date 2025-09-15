#include <iostream>
#include <cmath>
#include <vector>
#include <algorithm>
#include <limits>

#include "../Plot_library/graphics.hpp"

using namespace std;
using namespace PlotLibrary;

int find_mantissa_bits_double() {
    double number = 1.0;
    int bits = 0;
    
    while (1.0 + number != 1.0) {
        number /= 2.0;
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

    for (int i = 0; i < intervals_number; i++) {
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

    const double x_minimum = -1.0;
    const double x_maximum = 1.0;
    const int accuracy = 100;
    const double exact_main = atan(1.0) - atan(-1.0);
    const double exact_error = 1.0;
    const int maximum_intervals = 1000;

    cout << endl;

    cout << "Main integral calculation:" << endl;
    cout << "Exact solution: " << exact_main << endl;
    cout << "Left rectangle method: " << left_rectangle_method(main_integral, x_minimum, x_maximum, accuracy) << endl;
    cout << "Right rectangle method: " << right_rectangle_method(main_integral, x_minimum, x_maximum, accuracy) << endl;
    cout << "Medium rectangle method: " << medium_rectangle_method(main_integral, x_minimum, x_maximum, accuracy) << endl;
    cout << "Trapezoidal method: " << trapezoidal_method(main_integral, x_minimum, x_maximum, accuracy) << endl;
    cout << "Simpson method: " << simpson_method(main_integral, x_minimum, x_maximum, accuracy) << endl;

    cout << endl;

    cout << "Error integral calculation:" << endl;
    cout << "Exact solution: " << exact_error << endl;
    cout << "Left rectangle method: " << left_rectangle_method(error_integral, x_minimum, x_maximum, accuracy) << endl;
    cout << "Right rectangle method: " << right_rectangle_method(error_integral, x_minimum, x_maximum, accuracy) << endl;
    cout << "Medium rectangle method: " << medium_rectangle_method(error_integral, x_minimum, x_maximum, accuracy) << endl;
    cout << "Trapezoidal method: " << trapezoidal_method(error_integral, x_minimum, x_maximum, accuracy) << endl;
    cout << "Simpson method: " << simpson_method(error_integral, x_minimum, x_maximum, accuracy) << endl;
    
    cout << endl;

    cout << "Methods accuracy for main integral:" << endl;
    cout << "Left rectangle method: " << error_calculation(exact_main, left_rectangle_method(main_integral, x_minimum, x_maximum, accuracy)) << endl;
    cout << "Right rectangle method: " << error_calculation(exact_main, right_rectangle_method(main_integral, x_minimum, x_maximum, accuracy)) << endl;
    cout << "Medium rectangle method: " << error_calculation(exact_main, medium_rectangle_method(main_integral, x_minimum, x_maximum, accuracy)) << endl;
    cout << "Trapezoidal method: " << error_calculation(exact_main, trapezoidal_method(main_integral, x_minimum, x_maximum, accuracy)) << endl;
    cout << "Simpson method: " << error_calculation(exact_main, simpson_method(main_integral, x_minimum, x_maximum, accuracy)) << endl;

    cout << endl;

    cout << "Methods accuracy for error integral:" << endl;
    cout << "Left rectangle method: " << error_calculation(exact_error, left_rectangle_method(error_integral, x_minimum, x_maximum, accuracy)) << endl;
    cout << "Right rectangle method: " << error_calculation(exact_error, right_rectangle_method(error_integral, x_minimum, x_maximum, accuracy)) << endl;
    cout << "Medium rectangle method: " << error_calculation(exact_error, medium_rectangle_method(error_integral, x_minimum, x_maximum, accuracy)) << endl;
    cout << "Trapezoidal method: " << error_calculation(exact_error, trapezoidal_method(error_integral, x_minimum, x_maximum, accuracy)) << endl;
    cout << "Simpson method: " << error_calculation(exact_error, simpson_method(error_integral, x_minimum, x_maximum, accuracy)) << endl;

    cout << endl;

    vector<int> intervals;
    vector<double> intervals_double;

    for (int n = 10; n <= maximum_intervals; n += 10) {
        intervals.push_back(n);
        intervals_double.push_back((double)n);
    }

    auto generate_errors = [&](double (*function)(double), double exact, double (*method)(double (*)(double), double, double, int)) {
        vector<double> errors;

        for (int n : intervals) {
            double approximation = method(function, x_minimum, x_maximum, n);
            errors.push_back(fabs(exact - approximation));
        }

        return errors;
    };

    vector<double> errors_main_left = generate_errors(main_integral, exact_main, left_rectangle_method);
    vector<double> errors_main_right = generate_errors(main_integral, exact_main, right_rectangle_method);
    vector<double> errors_main_medium = generate_errors(main_integral, exact_main, medium_rectangle_method);
    vector<double> errors_main_trapezoidal = generate_errors(main_integral, exact_main, trapezoidal_method);
    vector<double> errors_main_simpson = generate_errors(main_integral, exact_main, simpson_method);

    vector<double> errors_error_left = generate_errors(error_integral, exact_error, left_rectangle_method);
    vector<double> errors_error_right = generate_errors(error_integral, exact_error, right_rectangle_method);
    vector<double> errors_error_medium = generate_errors(error_integral, exact_error, medium_rectangle_method);
    vector<double> errors_error_trapezoidal = generate_errors(error_integral, exact_error, trapezoidal_method);
    vector<double> errors_error_simpson = generate_errors(error_integral, exact_error, simpson_method);

    Figure figure_main(800, 600);
    PlotStyle style_left = figure_main.CreateStyle(Colors::RED, 2.0, LineStyle::SOLID, MarkerStyle::TRIANGLE_UP);
    PlotStyle style_right = figure_main.CreateStyle(Colors::GREEN, 2.0, LineStyle::SOLID, MarkerStyle::TRIANGLE_DOWN);
    PlotStyle style_medium = figure_main.CreateStyle(Colors::BLUE, 2.0, LineStyle::SOLID);
    PlotStyle style_trapezoidal = figure_main.CreateStyle(Colors::ORANGE, 2.0, LineStyle::SOLID);
    PlotStyle style_simpson = figure_main.CreateStyle(Colors::PURPLE, 2.0, LineStyle::SOLID);

    figure_main.Plot(intervals_double, errors_main_left, style_left);
    figure_main.Plot(intervals_double, errors_main_right, style_right);
    figure_main.Plot(intervals_double, errors_main_medium, style_medium);
    figure_main.Plot(intervals_double, errors_main_trapezoidal, style_trapezoidal);
    figure_main.Plot(intervals_double, errors_main_simpson, style_simpson);

    figure_main.SetTitle("Integration methods accuracy (main integral)");
    figure_main.SetXLabel("number of intervals");
    figure_main.SetYLabel("absolute error");
    figure_main.SetLegend({"Left rectangle", "Right rectangle", "Medium rectangle", "Trapezoidal", "Simpson"});
    figure_main.Grid(true);
    figure_main.Save("integration_methods_main.svg");

    Figure figure_error(800, 600);
    figure_error.Plot(intervals_double, errors_error_left, style_left);
    figure_error.Plot(intervals_double, errors_error_right, style_right);
    figure_error.Plot(intervals_double, errors_error_medium, style_medium);
    figure_error.Plot(intervals_double, errors_error_trapezoidal, style_trapezoidal);
    figure_error.Plot(intervals_double, errors_error_simpson, style_simpson);

    figure_error.SetTitle("Integration methods accuracy (error integral)");
    figure_error.SetXLabel("number of intervals");
    figure_error.SetYLabel("absolute error");
    figure_error.SetLegend({"Left rectangle", "Right rectangle", "Medium rectangle", "Trapezoidal", "Simpson"});
    figure_error.Grid(true);
    figure_error.Save("integration_methods_error.svg");

    return 0;
}
