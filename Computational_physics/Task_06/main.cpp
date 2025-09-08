#include <iostream>
#include <vector>
#include <cmath>
#include <iomanip>
#include <functional>

#include "../Plot_library/graphics.hpp"

using namespace std;
using namespace PlotLibrary;

double right_part(double t, double x) {
    return -x;
}

void euler_method_1(function<double(double, double)> function, vector<double>& t_values, vector<double>& x_values, double t_start, double x_start, double step, double t_minimum, double t_maximum) {
    t_values.push_back(t_start);
    x_values.push_back(x_start);

    int n = (int)((t_maximum - t_minimum) / step);

    for (int i = 0; i < n; i++) {
        double t_current = t_values[i];
        double x_current = x_values[i];
        t_values.push_back(t_current + step);
        x_values.push_back(x_current + step * function(t_current, x_current));
    }
}

void runge_kutta_method_2(function<double(double, double)> function, vector<double>& t_values, vector<double>& x_values, double t_start, double x_start, double step, double t_minimum, double t_maximum) {
    t_values.push_back(t_start);
    x_values.push_back(x_start);

    int n = (int)((t_maximum - t_minimum) / step);

    for (int i = 0; i < n; i++) {
        double t_current = t_values[i];
        double x_current = x_values[i];
        double k1 = function(t_current, x_current);
        double k2 = function(t_current + step / 2.0, x_current + (step / 2.0) * k1);
        t_values.push_back(t_current + step);
        x_values.push_back(x_current + step * k2);
    }
}

void runge_kutta_method_4(function<double(double, double)> function, vector<double>& t_values, vector<double>& x_values, double t_start, double x_start, double step, double t_minimum, double t_maximum) {
    t_values.push_back(t_start);
    x_values.push_back(x_start);

    int n = (int)((t_maximum - t_minimum) / step);

    for (int i = 0; i < n; i++) {
        double t_current = t_values[i];
        double x_current = x_values[i];
        double k1 = function(t_current, x_current);
        double k2 = function(t_current + step / 2.0, x_current + (step / 2.0) * k1);
        double k3 = function(t_current + step / 2.0, x_current + (step / 2.0) * k2);
        double k4 = function(t_current + step, x_current + step * k3);
        t_values.push_back(t_current + step);
        x_values.push_back(x_current + (step / 6.0) * (k1 + 2.0 * k2 + 2.0 * k3 + k4));
    }
}

int main() {
    const double t_start = 0.0;
    const double x_start = 1.0;
    const double t_minimum = 0.0;
    const double t_maximum = 3.0;

    for (double step = 0.5; step >= 0.1; step -= 0.1) {
        vector<double> t_euler_method_1, x_euler_method_1;
        vector<double> t_runge_kutta_method_2, x_runge_kutta_method_2;
        vector<double> t_runge_kutta_method_4, x_runge_kutta_method_4;
        vector<double> t_exact_value, x_exact_value;

        euler_method_1(right_part, t_euler_method_1, x_euler_method_1, t_start, x_start, step, t_minimum, t_maximum);
        runge_kutta_method_2(right_part, t_runge_kutta_method_2, x_runge_kutta_method_2, t_start, x_start, step, t_minimum, t_maximum);
        runge_kutta_method_4(right_part, t_runge_kutta_method_4, x_runge_kutta_method_4, t_start, x_start, step, t_minimum, t_maximum);

        for (double t = t_minimum; t <= t_maximum; t += step) {
            t_exact_value.push_back(t);
            x_exact_value.push_back(exp(-t));
        }

        Figure figure(800, 600);
        PlotStyle style_1 = figure.CreateStyle(Colors::BLUE, 1.0, LineStyle::SOLID);
        PlotStyle style_2 = figure.CreateStyle(Colors::RED, 1.0, LineStyle::SOLID);
        PlotStyle style_3 = figure.CreateStyle(Colors::GREEN, 1.0, LineStyle::SOLID);
        PlotStyle style_4 = figure.CreateStyle(Colors::BLACK, 1.0, LineStyle::SOLID);

        figure.Plot(t_euler_method_1, x_euler_method_1, style_1);
        figure.Plot(t_runge_kutta_method_2, x_runge_kutta_method_2, style_2);
        figure.Plot(t_runge_kutta_method_4, x_runge_kutta_method_4, style_3);
        figure.Plot(t_exact_value, x_exact_value, style_4);

        figure.SetTitle("Cauchy problem solution for step = 0." + to_string((int)(step * 10)));
        figure.SetXLabel("t");
        figure.SetYLabel("x(t)");
        figure.SetXLimit(t_minimum, t_maximum);
        figure.SetLegend({"Euler", "Runge-Kutta 2", "Runge-Kutta 4", "Exact solution"});
        figure.Save("cauchy_problem_solution_for_step=0." + to_string((int)(step * 10)) + ".svg");
    }

    return 0;
}
