#include <iostream>
#include <vector>
#include <cmath>
#include <algorithm>
#include <fstream>
#include <iomanip>

#include "../Plot_library/graphics.hpp"

using namespace std;
using namespace PlotLibrary;

double analytical_solution(double x, double t, double x_maximum) {
    return sin(M_PI * x / x_maximum) * exp(-M_PI * M_PI * t / (x_maximum * x_maximum));
}

void tridiagonal_solver(const vector<double>& a, const vector<double>& b, const vector<double>& c, const vector<double>& d, vector<double>& y) {
    int n = d.size();
    vector<double> alpha(n), beta(n);

    if (abs(b[0]) < 1e-10) {
        return;
    }

    alpha[0] = -c[0] / b[0];
    beta[0] = d[0] / b[0];

    for (int i = 1; i < n; ++i) {
        double denominator = b[i] + a[i] * alpha[i - 1];

        if (abs(denominator) < 1e-10) {
            for (int j = 0; j < n; ++j) y[j] = 0.0;
            return;
        }

        alpha[i] = -c[i] / denominator;
        beta[i] = (d[i] - a[i] * beta[i - 1]) / denominator;
    }

    y[n - 1] = beta[n - 1];

    for (int i = n - 2; i >= 0; --i) {
        y[i] = alpha[i] * y[i + 1] + beta[i];
    }
}

vector<vector<double>> crank_nicolson_scheme(int t_intervals_number, int x_intervals_number, double t_minimum, double t_maximum, double x_minimum, double x_maximum) {
    double x_step = (x_maximum - x_minimum) / x_intervals_number;
    double t_step = (t_maximum - t_minimum) / t_intervals_number;
    double sigma = t_step / (x_step * x_step);
    
    vector<vector<double>> u_values(t_intervals_number + 1, vector<double>(x_intervals_number + 1, 0.0));
    
    for (int i = 0; i <= x_intervals_number; ++i) {
        double x = i * x_step;
        u_values[0][i] = sin(M_PI * x / x_maximum);
    }
    
    for (int n = 0; n <= t_intervals_number; ++n) {
        u_values[n][0] = 0.0;
        u_values[n][x_intervals_number] = 0.0;
    }
    
    for (int n = 0; n < t_intervals_number; ++n) {
        vector<double> a(x_intervals_number - 1, -sigma / 2.0);
        vector<double> b(x_intervals_number - 1, 1.0 + sigma);
        vector<double> c(x_intervals_number - 1, -sigma / 2.0);
        vector<double> d(x_intervals_number - 1);
        
        for (int i = 1; i < x_intervals_number; ++i) {
            d[i - 1] = (sigma / 2.0) * u_values[n][i - 1] + (1.0 - sigma) * u_values[n][i] + (sigma / 2.0) * u_values[n][i + 1];
        }
        
        vector<double> y(x_intervals_number - 1);
        tridiagonal_solver(a, b, c, d, y);
        
        for (int i = 1; i < x_intervals_number; ++i) {
            u_values[n + 1][i] = y[i - 1];
        }
    }
    
    return u_values;
}

void equation_solution_plot(const vector<vector<double>>& u_values, int t_intervals_number, int x_intervals_number, double t_minimum, double t_maximum, double x_minimum, double x_maximum) {
    double x_step = (x_maximum - x_minimum) / x_intervals_number;
    vector<double> x_points(x_intervals_number + 1);

    for (int i = 0; i <= x_intervals_number; ++i) {
        x_points[i] = i * x_step;
    }
    
    vector<int> time_steps = {0, t_intervals_number / 10, t_intervals_number / 5, t_intervals_number / 2, t_intervals_number};
    vector<double> times;

    for (int step : time_steps) {
        times.push_back(t_minimum + step * ((t_maximum - t_minimum) / t_intervals_number));
    }
    
    Figure figure(1000, 800);
    
    vector<Color> colors = {Colors::YELLOW, Colors::RED, Colors::LIGHT_GREEN, Colors::GREEN, Colors::LIGHT_BLUE, Colors::BLUE, Colors::PINK, Colors::PURPLE, Colors::BROWN, Colors::BLACK};
    
    for (size_t i = 0; i < time_steps.size(); ++i) {
        int n = time_steps[i];
        double t = times[i];
        
        vector<double> u_current(x_intervals_number + 1);

        for (int j = 0; j <= x_intervals_number; ++j) {
            u_current[j] = u_values[n][j];
        }
        
        vector<double> u_analytical(x_intervals_number + 1);

        for (int j = 0; j <= x_intervals_number; ++j) {
            u_analytical[j] = analytical_solution(x_points[j], t, x_maximum);
        }
        
        PlotStyle style_numerical = figure.CreateStyle(colors[2 * i], 2.0, LineStyle::SOLID);
        PlotStyle style_analytical = figure.CreateStyle(colors[2 * i + 1], 2.0, LineStyle::DASHED);
        
        figure.Plot(x_points, u_current, style_numerical);
        figure.Plot(x_points, u_analytical, style_analytical);
    }
    
    figure.SetTitle("Solution of the heat equation by Crank-Nicolson scheme");
    figure.SetXLabel("x");
    figure.SetYLabel("u(x,t)");
    figure.SetXLimit(x_minimum, x_maximum);
    figure.SetYLimit(-0.1, 1.1);
    figure.Grid(true);
    
    vector<string> legend_items;

    for (size_t i = 0; i < times.size(); ++i) {
        legend_items.push_back("t = " + to_string(times[i]) + " numerical");
        legend_items.push_back("t = " + to_string(times[i]) + " analytical");
    }

    figure.SetLegend(legend_items);
    figure.Save("equation_solution.svg");
}

void convergence_study_plot(double t_minimum, double t_maximum, double x_minimum, double x_maximum) {
    vector<int> t_intervals = {100, 200, 400, 800, 1600};
    vector<int> x_intervals = {10, 20, 40, 80, 160};
    
    vector<double> errors;
    vector<double> step_values;
    
    for (size_t i = 0; i < x_intervals.size(); ++i) {
        int x_intervals_number = x_intervals[i];
        int t_intervals_number = t_intervals[i];
        
        auto u_values = crank_nicolson_scheme(t_intervals_number, x_intervals_number, t_minimum, t_maximum, x_minimum, x_maximum);
        
        double maximum_error = 0.0;

        for (int j = 0; j <= x_intervals_number; ++j) {
            double x = j * (x_maximum / x_intervals_number);
            double error = abs(u_values.back()[j] - analytical_solution(x, t_maximum, x_maximum));
            maximum_error = max(maximum_error, error);
        }
        
        errors.push_back(maximum_error);
        step_values.push_back(x_maximum / x_intervals_number);
        
        cout << "Number of intervals along X: " << x_intervals_number << ", X step: " << (x_maximum/x_intervals_number) << ", Error: " << maximum_error << endl;
    }
    
    Figure figure(800, 600);
    PlotStyle error_style = figure.CreateStyle(Colors::BLUE, 3.0, LineStyle::SOLID);
    PlotStyle theory_style = figure.CreateStyle(Colors::RED, 2.0, LineStyle::DASHED);
    
    vector<double> theory_errors;
    double base_error = errors[0] / (step_values[0] * step_values[0]);

    for (double x_step : step_values) {
        theory_errors.push_back(base_error * x_step * x_step);
    }
    
    vector<double> log_step, log_errors, log_theory;

    for (size_t i = 0; i < step_values.size(); ++i) {
        log_step.push_back(log(step_values[i]));
        log_errors.push_back(log(errors[i]));
        log_theory.push_back(log(theory_errors[i]));
    }
    
    figure.Plot(log_step, log_errors, error_style);
    figure.Plot(log_step, log_theory, theory_style);
    
    figure.SetTitle("Convergence study of Crank-Nicolson scheme");
    figure.SetXLabel("ln(h)");
    figure.SetYLabel("ln(error)");
    figure.SetLegend({"Numerical error", "Theoretical error O(h^2)"});
    figure.Grid(true);
    
    figure.Save("convergence_study.svg");
}

int main() {
    const double t_minimum = 0.0;
    const double t_maximum = 0.1;
    const double x_minimum = 0.0;
    const double x_maximum = 1.0;
    const int x_intervals_number = 50;
    const int t_intervals_number = 500;
    
    auto u_numerical = crank_nicolson_scheme(t_intervals_number, x_intervals_number, t_minimum, t_maximum, x_minimum, x_maximum);

    equation_solution_plot(u_numerical, t_intervals_number, x_intervals_number, t_minimum, t_maximum, x_minimum, x_maximum);

    cout << endl;

    convergence_study_plot(t_minimum, t_maximum, x_minimum, x_maximum);
    
    cout << endl;

    cout << "Convergence study:" << endl;
    
    double final_error = 0.0;

    for (int i = 0; i <= x_intervals_number; ++i) {
        double x = i * (x_maximum / x_intervals_number);
        double error = abs(u_numerical.back()[i] - analytical_solution(x, t_maximum, x_maximum));
        final_error = max(final_error, error);
    }

    cout << "Maximum error at t = " << t_maximum << ": " << final_error << endl;

    cout << endl;
    
    return 0;
}
