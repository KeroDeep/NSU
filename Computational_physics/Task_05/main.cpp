#include <iostream>
#include <vector>
#include <cmath>
#include <functional>

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

double simpson_method(function<double(double)> function, double left_bound, double right_bound, int intervals_number) {
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

double bessel_function(double x, double m, function<double(function<double(double)>, double, double, int)> method, int intervals_number) {
    auto integrand = [m, x](double t) -> double {
        return cos(m * t - x * sin(t)) / M_PI;
    };
    
    return method(integrand, 0.0, M_PI, intervals_number);
}

double lagrange_basis(double x, const vector<double>& x_nodes, int i) {
    double result = 1.0;
    int n = x_nodes.size() - 1;

    for (int j = 0; j <= n; j++) {
        if (j != i) {
            result *= (x - x_nodes[j]) / (x_nodes[i] - x_nodes[j]);
        }
    }

    return result;
}

double lagrange_polynom(double x, const vector<double>& x_nodes, const vector<double>& y_nodes) {
    double result = 0.0;
    int n = x_nodes.size() - 1;

    for (int i = 0; i <= n; i++) {
        result += y_nodes[i] * lagrange_basis(x, x_nodes, i);
    }

    return result;
}

int main() {
    cout.precision(find_mantissa_bits_double());

    const int accuracy = 1000;
    const double x_minimum = 0.0;
    const double x_maximum = 10.0;
    vector<double> x_values = Linspace(x_minimum, x_maximum, 1000);
    const int node_counts[] = {2, 3, 4, 5, 6, 7, 8, 9, 10};

    for (int n : node_counts) {
        vector<double> x_nodes(n);
        vector<double> y_nodes(n);
        vector<double> y_lagrange(x_values.size());
        vector<double> y_bessel(x_values.size());
        vector<double> y_difference(x_values.size());

        double step = (x_maximum - x_minimum) / (n - 1);
        
        for (int i = 0; i < n; i++) {
            x_nodes[i] = x_minimum + i * step;
            y_nodes[i] = bessel_function(x_nodes[i], 0.0, simpson_method, 1000);
        }
        
        for (size_t i = 0; i < x_values.size(); i++) {
            y_lagrange[i] = lagrange_polynom(x_values[i], x_nodes, y_nodes);
            y_bessel[i] = bessel_function(x_values[i], 0.0, simpson_method, accuracy);
            y_difference[i] = fabs(y_lagrange[i] - y_bessel[i]);
        }

        Figure figure(800, 600);
        PlotStyle style_1 = figure.CreateStyle(Colors::BLUE, 1.0, LineStyle::SOLID);
        PlotStyle style_2 = figure.CreateStyle(Colors::RED, 1.0, LineStyle::SOLID);
        PlotStyle style_3 = figure.CreateStyle(Colors::GREEN, 1.0, LineStyle::SOLID);
        figure.Plot(x_values, y_lagrange, style_1);
        figure.Plot(x_values, y_bessel, style_2);
        figure.Plot(x_values, y_difference, style_3);
        figure.SetTitle("Lagrange polynomial for n = " + to_string(n));
        figure.SetXLabel("x");
        figure.SetYLabel("Value");
        figure.SetXLimit(x_minimum, x_maximum);
        figure.SetLegend({"P_n(x)", "J_0(x)", "|P_n(x) - J_0(x)|"});
        figure.Save("lagrange_polynomial_for_n=" + to_string(n) + ".svg");
    }

    return 0;
}
