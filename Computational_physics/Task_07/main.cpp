#include <iostream>
#include <vector>
#include <cmath>
#include <algorithm>

#include "../Plot_library/graphics.hpp"

using namespace std;
using namespace PlotLibrary;

struct Params {
    double a;
    double b;
    double c;
    double d;

    Params(double prey_growth_rate, double prey_predator_interaction, double predator_growth_rate, double predator_death_rate) {
        a = prey_growth_rate;
        b = prey_predator_interaction;
        c = predator_growth_rate;
        d = predator_death_rate;
    }
};

double right_part_1(double t, double x, double y, const Params& parameters) {
    return parameters.a * x - parameters.b * x * y;
}

double right_part_2(double t, double x, double y, const Params& parameters) {
    return parameters.c * x * y - parameters.d * y;
}

void runge_kutta_method(vector<double>& t_values, vector<double>& x_values, vector<double>& y_values, double t_start, double x_start, double y_start, double step, double t_minimum, double t_maximum, const Params& parameters) {
    t_values.clear();
    x_values.clear();
    y_values.clear();

    double t_current = max(t_start, t_minimum);

    if (t_current > t_start) {
        double t_difference = t_current - t_start;
        double x_initialization = x_start + t_difference * right_part_1(t_start, x_start, y_start, parameters);
        double y_initialization = y_start + t_difference * right_part_2(t_start, x_start, y_start, parameters);
        t_values.push_back(t_current);
        x_values.push_back(x_initialization);
        y_values.push_back(y_initialization);
    } else {
        t_values.push_back(t_start);
        x_values.push_back(x_start);
        y_values.push_back(y_start);
    }

    while (t_current < t_maximum) {
        double t_next = t_current + step;

        if (t_next > t_maximum) {
            step = t_maximum - t_current;
            t_next = t_maximum;
        }

        double x_n = x_values.back();
        double y_n = y_values.back();
        double k_1_x, k_1_y, k_2_x, k_2_y;
        k_1_x = right_part_1(t_current, x_n, y_n, parameters);
        k_1_y = right_part_2(t_current, x_n, y_n, parameters);
        double x_mid = x_n + (step / 2.0) * k_1_x;
        double y_mid = y_n + (step / 2.0) * k_1_y;
        k_2_x = right_part_1(t_current + step / 2.0, x_mid, y_mid, parameters);
        k_2_y = right_part_2(t_current + step / 2.0, x_mid, y_mid, parameters);
        x_values.push_back(x_n + step * k_2_x);
        y_values.push_back(y_n + step * k_2_y);
        t_values.push_back(t_next);
        t_current = t_next;
    }
}

int main() {
    Params parameters(10.0, 2.0, 2.0, 10.0);

    const double t_start = 0.0;
    const double t_end = 10.0;
    const double x_start = 1.0;
    const double y_start = 1.0;
    const double t_minimum = 0.0;
    const double t_maximum = 10.0;
    const double step = 0.01;

    vector<double> t_values, x_values, y_values;

    runge_kutta_method(t_values, x_values, y_values, t_start, x_start, y_start, step, t_minimum, t_maximum, parameters);

    double x_minimum = *min_element(x_values.begin(), x_values.end());
    double x_maximum = *max_element(x_values.begin(), x_values.end());
    double y_minimum = *min_element(y_values.begin(), y_values.end());
    double y_maximum = *max_element(y_values.begin(), y_values.end());
    double margin = 0.1 * max({x_maximum - x_minimum, y_maximum - y_minimum});
    x_minimum = max(0.0, x_minimum - margin);
    x_maximum += margin;
    y_minimum = max(0.0, y_minimum - margin);
    y_maximum += margin;

    Figure figure(800, 600);
    PlotStyle style = figure.CreateStyle(Colors::BLUE, 1.0, LineStyle::SOLID);

    figure.Plot(x_values, y_values, style);

    figure.SetTitle("Phase trajectory of predator-prey system");
    figure.SetXLabel("x (prey)");
    figure.SetYLabel("y (predator)");
    figure.SetXLimit(x_minimum, x_maximum);
    figure.SetYLimit(y_minimum, y_maximum);
    figure.Save("system_phase_trajectory.svg");

    return 0;
}
