#include <iostream>
#include <vector>
#include <cmath>
#include <functional>
#include <iomanip>
#include <sstream>

#include "../Plot_library/graphics.hpp"

using namespace std;
using namespace PlotLibrary;

double right_part(double t, double x) {
    return -x;
}

void euler_method_1(function<double(double, double)> function, vector<double>& t_values, vector<double>& x_values, double t_start, double x_start, double step, double t_minimum, double t_maximum) {
    t_values.clear();
    x_values.clear();

    double t_current = max(t_start, t_minimum);

    if (t_current > t_start) {
        double t_difference = t_current - t_start;
        double x_initialization = x_start + t_difference * function(t_start, x_start);
        t_values.push_back(t_current);
        x_values.push_back(x_initialization);
    } else {
        t_values.push_back(t_start);
        x_values.push_back(x_start);
    }

    while (t_current < t_maximum) {
        double t_next = t_current + step;

        if (t_next > t_maximum) {
            step = t_maximum - t_current;
            t_next = t_maximum;
        }

        double x_next = x_values.back() + step * function(t_current, x_values.back());
        t_values.push_back(t_next);
        x_values.push_back(x_next);
        t_current = t_next;
    }
}

void runge_kutta_method_2(function<double(double, double)> function, vector<double>& t_values, vector<double>& x_values, double t_start, double x_start, double step, double t_minimum, double t_maximum) {
    t_values.clear();
    x_values.clear();
    
    double t_current = max(t_start, t_minimum);

    if (t_current > t_start) {
        double t_difference = t_current - t_start;
        double x_initialization = x_start + t_difference * function(t_start, x_start);
        t_values.push_back(t_current);
        x_values.push_back(x_initialization);
    } else {
        t_values.push_back(t_start);
        x_values.push_back(x_start);
    }

    while (t_current < t_maximum) {
        double t_next = t_current + step;

        if (t_next > t_maximum) {
            step = t_maximum - t_current;
            t_next = t_maximum;
        }

        double t_n = t_values.back();
        double x_n = x_values.back();
        double k_1 = function(t_n, x_n);
        double k_2 = function(t_n + step / 2.0, x_n + (step / 2.0) * k_1);
        x_values.push_back(x_n + step * k_2);
        t_values.push_back(t_next);
        t_current = t_next;
    }
}

void runge_kutta_method_4(function<double(double, double)> function, vector<double>& t_values, vector<double>& x_values, double t_start, double x_start, double step, double t_minimum, double t_maximum) {
    t_values.clear();
    x_values.clear();

    double t_current = max(t_start, t_minimum);

    if (t_current > t_start) {
        double t_difference = t_current - t_start;
        double x_initialization = x_start + t_difference * function(t_start, x_start);
        t_values.push_back(t_current);
        x_values.push_back(x_initialization);
    } else {
        t_values.push_back(t_start);
        x_values.push_back(x_start);
    }

    while (t_current < t_maximum) {
        double t_next = t_current + step;

        if (t_next > t_maximum) {
            step = t_maximum - t_current;
            t_next = t_maximum;
        }

        double t_n = t_values.back();
        double x_n = x_values.back();
        double k_1 = function(t_n, x_n);
        double k_2 = function(t_n + step / 2.0, x_n + (step / 2.0) * k_1);
        double k_3 = function(t_n + step / 2.0, x_n + (step / 2.0) * k_2);
        double k_4 = function(t_n + step, x_n + step * k_3);
        x_values.push_back(x_n + (step / 6.0) * (k_1 + 2.0 * k_2 + 2.0 * k_3 + k_4));
        t_values.push_back(t_next);
        t_current = t_next;
    }
}

int main() {
    const double t_start = 0.0;
    const double x_start = 1.0;
    const double t_minimum = 0.0;
    const double t_maximum = 3.0;

    vector<double> steps = {0.5, 0.4, 0.3, 0.2, 0.1};

    for (double step : steps) {
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
        PlotStyle style_2 = figure.CreateStyle(Colors::RED, 1.0, LineStyle::DASHED);
        PlotStyle style_3 = figure.CreateStyle(Colors::GREEN, 1.0, LineStyle::DOTTED);
        PlotStyle style_4 = figure.CreateStyle(Colors::BLACK, 1.0, LineStyle::SOLID);

        figure.Plot(t_euler_method_1, x_euler_method_1, style_1);
        figure.Plot(t_runge_kutta_method_2, x_runge_kutta_method_2, style_2);
        figure.Plot(t_runge_kutta_method_4, x_runge_kutta_method_4, style_3);
        figure.Plot(t_exact_value, x_exact_value, style_4);

        ostringstream oss;
        oss << fixed << setprecision(1) << step;
        string step_str = oss.str();
        string filename = "cauchy_problem_solution_for_step=" + step_str + ".svg";
        figure.SetTitle("Cauchy problem solution for step = " + step_str);
        figure.SetXLabel("t");
        figure.SetYLabel("x(t)");
        figure.SetXLimit(t_minimum, t_maximum);
        figure.SetLegend({"Euler", "Runge-Kutta 2", "Runge-Kutta 4", "Exact Solution"});
        figure.Save(filename);
    }

    return 0;
}
