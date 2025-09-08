#include <iostream>
#include <cmath>
#include <vector>
#include <iomanip>
#include <sstream>
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

double trapezoidal_method(function<double(double)> function, double left_bound, double right_bound, int intervals_number) {
    double step = (right_bound - left_bound) / intervals_number;
    double square = 0.0;

    for (int i = 0; i < intervals_number; i++) {
        double x_left = left_bound + i * step;
        double x_right = left_bound + (i + 1) * step;
        square += (function(x_left) + function(x_right)) * step / 2.0;
    }

    return square;
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

double bessel_derivative(double x, double m, double delta, function<double(function<double(double)>, double, double, int)> method, int intervals_number) {
    return (bessel_function(x + delta, m, method, intervals_number) - bessel_function(x - delta, m, method, intervals_number)) / (2.0 * delta);
}

double test_integral(double x) {
    return 1.0 / (1.0 + pow(x, 2));
}

int main() {
    cout.precision(find_mantissa_bits_double());

    const int accuracy = 1000;
    const double delta = 1e-6;
    const double x_minimum = 0.0;
    const double x_maximum = 2.0 * M_PI;
    vector<double> x_values = Linspace(x_minimum, x_maximum, 500);

    Figure bessel_functions_simpson_figure(800, 600);
    vector<int> m_values = {0, 1, 2, 3, 4, 5, 6, 7};
    vector<PlotStyle> bessel_styles = {
        bessel_functions_simpson_figure.CreateStyle(Colors::BLUE, 1.0, LineStyle::SOLID),
        bessel_functions_simpson_figure.CreateStyle(Colors::RED, 1.0, LineStyle::SOLID),
        bessel_functions_simpson_figure.CreateStyle(Colors::GREEN, 1.0, LineStyle::SOLID),
        bessel_functions_simpson_figure.CreateStyle(Colors::PURPLE, 1.0, LineStyle::SOLID),
        bessel_functions_simpson_figure.CreateStyle(Colors::CYAN, 1.0, LineStyle::SOLID),
        bessel_functions_simpson_figure.CreateStyle(Colors::MAGENTA, 1.0, LineStyle::SOLID),
        bessel_functions_simpson_figure.CreateStyle(Colors::YELLOW, 1.0, LineStyle::SOLID),
        bessel_functions_simpson_figure.CreateStyle(Colors::ORANGE, 1.0, LineStyle::SOLID)
    };
    vector<string> bessel_functions_legends;
    
    for (size_t i = 0; i < m_values.size(); ++i) {
        double m = static_cast<double>(m_values[i]);
        vector<double> bessel_functions_values;

        for (auto px : x_values) {
            bessel_functions_values.push_back(bessel_function(px, m, simpson_method, accuracy));
        }
        
        bessel_functions_simpson_figure.Plot(x_values, bessel_functions_values, bessel_styles[i]);
        bessel_functions_legends.push_back("J_" + to_string(m_values[i]) + "(x)");
    }
    
    bessel_functions_simpson_figure.SetTitle("Bessel functions for different m (Simpson method)");
    bessel_functions_simpson_figure.SetXLabel("x");
    bessel_functions_simpson_figure.SetYLabel("J_m(x)");
    bessel_functions_simpson_figure.SetXLimit(x_minimum, x_maximum);
    bessel_functions_simpson_figure.SetLegend(bessel_functions_legends);
    bessel_functions_simpson_figure.Save("bessel_functions_simpson.svg");

    Figure bessel_functions_trapezoidal_figure(800, 600);
    vector<string> bessel_functions_trap_legends;
    
    for (size_t i = 0; i < m_values.size(); ++i) {
        double m = static_cast<double>(m_values[i]);
        vector<double> bessel_functions_values;

        for (auto px : x_values) {
            bessel_functions_values.push_back(bessel_function(px, m, trapezoidal_method, accuracy));
        }
        
        bessel_functions_trapezoidal_figure.Plot(x_values, bessel_functions_values, bessel_styles[i]);
        bessel_functions_trap_legends.push_back("J_" + to_string(m_values[i]) + "(x)");
    }

    bessel_functions_trapezoidal_figure.SetTitle("Bessel functions for different m (Trapezoidal method)");
    bessel_functions_trapezoidal_figure.SetXLabel("x");
    bessel_functions_trapezoidal_figure.SetYLabel("J_m(x)");
    bessel_functions_trapezoidal_figure.SetXLimit(x_minimum, x_maximum);
    bessel_functions_trapezoidal_figure.SetLegend(bessel_functions_trap_legends);
    bessel_functions_trapezoidal_figure.Save("bessel_functions_trapezoidal.svg");

    Figure bessel_derivatives_simpson_figure(800, 600);
    vector<string> bessel_derivatives_legends;
    
    for (size_t i = 0; i < m_values.size(); ++i) {
        double m = static_cast<double>(m_values[i]);
        vector<double> bessel_derivatives_values;

        for (auto px : x_values) {
            bessel_derivatives_values.push_back(bessel_derivative(px, m, delta, simpson_method, accuracy));
        }

        bessel_derivatives_simpson_figure.Plot(x_values, bessel_derivatives_values, bessel_styles[i]);
        bessel_derivatives_legends.push_back("J_" + to_string(m_values[i]) + "'(x)");
    }

    bessel_derivatives_simpson_figure.SetTitle("Derivatives of Bessel functions for different m (Simpson method)");
    bessel_derivatives_simpson_figure.SetXLabel("x");
    bessel_derivatives_simpson_figure.SetYLabel("J_m'(x)");
    bessel_derivatives_simpson_figure.SetXLimit(x_minimum, x_maximum);
    bessel_derivatives_simpson_figure.SetLegend(bessel_derivatives_legends);
    bessel_derivatives_simpson_figure.Save("bessel_derivatives_simpson.svg");

    Figure bessel_derivatives_trapezoidal_figure(800, 600);
    vector<string> bessel_derivatives_trap_legends;
    
    for (size_t i = 0; i < m_values.size(); ++i) {
        double m = static_cast<double>(m_values[i]);
        vector<double> bessel_derivatives_values;

        for (auto px : x_values) {
            bessel_derivatives_values.push_back(bessel_derivative(px, m, delta, trapezoidal_method, accuracy));
        }

        bessel_derivatives_trapezoidal_figure.Plot(x_values, bessel_derivatives_values, bessel_styles[i]);
        bessel_derivatives_trap_legends.push_back("J_" + to_string(m_values[i]) + "'(x)");
    }

    bessel_derivatives_trapezoidal_figure.SetTitle("Derivatives of Bessel functions for different m (Trapezoidal method)");
    bessel_derivatives_trapezoidal_figure.SetXLabel("x");
    bessel_derivatives_trapezoidal_figure.SetYLabel("J_m'(x)");
    bessel_derivatives_trapezoidal_figure.SetXLimit(x_minimum, x_maximum);
    bessel_derivatives_trapezoidal_figure.SetLegend(bessel_derivatives_trap_legends);
    bessel_derivatives_trapezoidal_figure.Save("bessel_derivatives_trapezoidal.svg");

    const int points = 1000;
    const double target_precision = 1e-10;
    
    vector<double> simpson_errors;
    vector<double> simpson_log_errors;
    
    vector<double> trapezoidal_errors;
    vector<double> trapezoidal_log_errors;
    
    double max_simpson_error = 0.0;
    double mean_simpson_error = 0.0;
    int valid_simpson_points = 0;

    double max_trapezoidal_error = 0.0;
    double mean_trapezoidal_error = 0.0;
    int valid_trapezoidal_points = 0;
    
    for (auto x : x_values) {
        double simpson_first_term = bessel_derivative(x, 0.0, delta, simpson_method, accuracy);
        double simpson_second_term = bessel_function(x, 1.0, simpson_method, accuracy);
        double simpson_error = fabs(simpson_first_term + simpson_second_term);
        simpson_errors.push_back(simpson_error);

        if (simpson_error > 0) {
            simpson_log_errors.push_back(log10(simpson_error));
            mean_simpson_error += simpson_error;
            valid_simpson_points++;
        } else {
            simpson_log_errors.push_back(NAN);
        }
        
        if (simpson_error > max_simpson_error) {
            max_simpson_error = simpson_error;
        }
        
        double trapezoidal_first_term = bessel_derivative(x, 0.0, delta, trapezoidal_method, accuracy);
        double trapezoidal_second_term = bessel_function(x, 1.0, trapezoidal_method, accuracy);
        double trapezoidal_error = fabs(trapezoidal_first_term + trapezoidal_second_term);
        trapezoidal_errors.push_back(trapezoidal_error);
        
        if (trapezoidal_error > 0) {
            trapezoidal_log_errors.push_back(log10(trapezoidal_error));
            mean_trapezoidal_error += trapezoidal_error;
            valid_trapezoidal_points++;
        } else {
            trapezoidal_log_errors.push_back(NAN);
        }
        
        if (trapezoidal_error > max_trapezoidal_error) {
            max_trapezoidal_error = trapezoidal_error;
        }
    }

    if (valid_simpson_points > 0) {
        mean_simpson_error /= valid_simpson_points;
    }

    if (valid_trapezoidal_points > 0) {
        mean_trapezoidal_error /= valid_trapezoidal_points;
    }

    cout << endl;
    
    cout << "Simpson method:" << endl;
    cout << "Max error: " << max_simpson_error << endl;
    cout << "Mean error: " << mean_simpson_error << endl;
    cout << "Valid points: " << valid_simpson_points << "/" << x_values.size() << endl;

    cout << endl;

    cout << "Trapezoidal method:" << endl;
    cout << "Max error: " << max_trapezoidal_error << endl;
    cout << "Mean error: " << mean_trapezoidal_error << endl;
    cout << "Valid points: " << valid_trapezoidal_points << "/" << x_values.size() << endl;

    cout << endl;
    
    Figure error_figure(800, 600);
    error_figure.SetYPrecision(2);
    
    PlotStyle simpson_style = error_figure.CreateStyle(Colors::RED, 1.5, LineStyle::SOLID);
    PlotStyle trapezoidal_style = error_figure.CreateStyle(Colors::BLUE, 1.5, LineStyle::SOLID);
    
    error_figure.Plot(x_values, simpson_log_errors, simpson_style);
    error_figure.Plot(x_values, trapezoidal_log_errors, trapezoidal_style);
    
    vector<double> target_precision_line(points, -10.0);
    PlotStyle target_style_line = error_figure.CreateStyle(Colors::GREEN, 1.0, LineStyle::DASHED);
    error_figure.Plot(x_values, target_precision_line, target_style_line);
    
    error_figure.SetTitle("Equality execution accuracy: J_0'(x) + J_1(x) = 0");
    error_figure.SetXLabel("x");
    error_figure.SetYLabel("log_10(error)");
    error_figure.SetXLimit(x_minimum, x_maximum);
    error_figure.SetLegend({"Simpson method", "Trapezoidal method", "Target precision"});
    error_figure.Save("equation_error.svg");
    
    if (mean_simpson_error < target_precision) {
        cout << "Simpson method achieves target precision" << endl;
    } else {
        cout << "Simpson method does not achieve target precision" << endl;
    }

    if (mean_trapezoidal_error < target_precision) {
        cout << "Trapezoidal method achieves target precision" << endl;
    } else {
        cout << "Trapezoidal method does not achieve target precision" << endl;
    }

    cout << endl;

    const double exact_value = M_PI / 4.0;
    const double left_bound = 0.0;
    const double right_bound = 1.0;

    vector<int> partitions;
    for (int n = 10; n <= 1000; n += (n < 100) ? 10 : (n < 500) ? 50 : 100) {
        partitions.push_back(n);
    }

    cout << "Error in simpson method for different partitions:" << endl;

    for (int n : partitions) {
        cout << "Error for (n = " << n << "): " << fabs(simpson_method(test_integral, left_bound, right_bound, n) - exact_value) << endl;
    }

    cout << endl;

    cout << "Error in trapezoidal method for different partitions:" << endl;

    for (int n : partitions) {
        cout << "Error for (n = " << n << "): " << fabs(trapezoidal_method(test_integral, left_bound, right_bound, n) - exact_value) << endl;
    }

    cout << endl;

    return 0;
}
