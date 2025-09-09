#include <iostream>
#include <vector>
#include <cmath>
#include <iomanip>
#include <algorithm>

#include "../Plot_library/graphics.hpp"

using namespace std;
using namespace PlotLibrary;

double A[2][2] = {
    {998, 1998},
    {-999, -1999}
};

void right_part(double t, double y[2], double f[2], double A[2][2]) {
    f[0] = A[0][0] * y[0] + A[0][1] * y[1];
    f[1] = A[1][0] * y[0] + A[1][1] * y[1];
}

void solve_2x2_system(double coefficient, double right[2], double solution[2], double A[2][2]) {
    double left[2][2];

    left[0][0] = 1 - coefficient * A[0][0];
    left[0][1] = -coefficient * A[0][1];
    left[1][0] = -coefficient * A[1][0];
    left[1][1] = 1 - coefficient * A[1][1];

    double det = left[0][0] * left[1][1] - left[0][1] * left[1][0];
    if (abs(det) < 1e-10) {
        cerr << "Warning: Determinant is near zero, solution may be unstable." << endl;
        solution[0] = solution[1] = 0.0;
        return;
    }

    solution[0] = (left[1][1] * right[0] - left[0][1] * right[1]) / det;
    solution[1] = (left[0][0] * right[1] - left[1][0] * right[0]) / det;
}

void explicit_method(vector<double>& t_values, vector<vector<double>>& y_values, double t_start, double y_start[2], double step, double t_minimum, double t_maximum) {
    t_values.clear();
    y_values.clear();

    double t_current = max(t_start, t_minimum);
    double y_current[2] = {y_start[0], y_start[1]};

    if (t_current > t_start) {
        double dt = t_current - t_start;
        double f[2];
        right_part(t_start, y_start, f, A);
        y_current[0] = y_start[0] + dt * f[0];
        y_current[1] = y_start[1] + dt * f[1];
    }

    t_values.push_back(t_current);
    y_values.push_back({y_current[0], y_current[1]});

    while (t_current < t_maximum) {
        double t_next = t_current + step;

        if (t_next > t_maximum) {
            step = t_maximum - t_current;
            t_next = t_maximum;
        }

        double f[2];
        right_part(t_current, y_current, f, A);
        y_current[0] += step * f[0];
        y_current[1] += step * f[1];
        t_values.push_back(t_next);
        y_values.push_back({y_current[0], y_current[1]});
        t_current = t_next;
    }
}

void semi_implicit_method(vector<double>& t_values, vector<vector<double>>& y_values, double t_start, double y_start[2], double step, double t_minimum, double t_maximum) {
    t_values.clear();
    y_values.clear();

    double t_current = max(t_start, t_minimum);
    double y_current[2] = {y_start[0], y_start[1]};

    if (t_current > t_start) {
        double dt = t_current - t_start;
        double f[2];
        right_part(t_start, y_start, f, A);
        y_current[0] = y_start[0] + dt * f[0];
        y_current[1] = y_start[1] + dt * f[1];
    }

    t_values.push_back(t_current);
    y_values.push_back({y_current[0], y_current[1]});

    while (t_current < t_maximum) {
        double t_next = t_current + step;

        if (t_next > t_maximum) {
            step = t_maximum - t_current;
            t_next = t_maximum;
        }

        double y_pred[2] = {y_current[0], y_current[1]};
        double f_current[2], f_next[2];
        right_part(t_current, y_current, f_current, A);
        y_pred[0] += step * f_current[0];
        y_pred[1] += step * f_current[1];
        right_part(t_next, y_pred, f_next, A);
        double right[2];
        right[0] = y_current[0] + (step / 2.0) * (f_current[0] + f_next[0]);
        right[1] = y_current[1] + (step / 2.0) * (f_current[1] + f_next[1]);
        solve_2x2_system(step / 2.0, right, y_current, A);
        t_values.push_back(t_next);
        y_values.push_back({y_current[0], y_current[1]});
        t_current = t_next;
    }
}

void implicit_method(vector<double>& t_values, vector<vector<double>>& y_values, double t_start, double y_start[2], double step, double t_minimum, double t_maximum) {
    t_values.clear();
    y_values.clear();

    double t_current = max(t_start, t_minimum);
    double y_current[2] = {y_start[0], y_start[1]};

    if (t_current > t_start) {
        double dt = t_current - t_start;
        double f[2];
        right_part(t_start, y_start, f, A);
        y_current[0] = y_start[0] + dt * f[0];
        y_current[1] = y_start[1] + dt * f[1];
    }

    t_values.push_back(t_current);
    y_values.push_back({y_current[0], y_current[1]});

    while (t_current < t_maximum) {
        double t_next = t_current + step;

        if (t_next > t_maximum) {
            step = t_maximum - t_current;
            t_next = t_maximum;
        }

        double right[2] = {y_current[0], y_current[1]};
        solve_2x2_system(step, right, y_current, A);
        t_values.push_back(t_next);
        y_values.push_back({y_current[0], y_current[1]});
        t_current = t_next;
    }
}

void analytic_solution(vector<double>& t, vector<vector<double>>& y_analytic) {
    y_analytic.clear();

    for (double t_i : t) {
        y_analytic.push_back({2 * exp(-t_i) - exp(-1000 * t_i), -exp(-t_i) + exp(-1000 * t_i)});
    }
}

int main() {
    double step = 0.001;
    double t_start = 0.0;
    double t_end = 0.1;
    double y_start[2] = {1.0, 0.0};

    vector<double> t_explicit;
    vector<vector<double>> y_explicit;
    explicit_method(t_explicit, y_explicit, t_start, y_start, step, t_start, t_end);

    vector<double> t_semi_implicit;
    vector<vector<double>> y_semi_implicit;
    semi_implicit_method(t_semi_implicit, y_semi_implicit, t_start, y_start, step, t_start, t_end);
    
    vector<double> t_implicit;
    vector<vector<double>> y_implicit;
    implicit_method(t_implicit, y_implicit, t_start, y_start, step, t_start, t_end);

    vector<double> t_analytic = t_explicit;
    vector<vector<double>> y_analytic;
    analytic_solution(t_analytic, y_analytic);

    vector<double> u_explicit, v_explicit, u_semi_implicit, v_semi_implicit, u_implicit, v_implicit, u_analytic, v_analytic;
    for (const auto& y : y_explicit) { u_explicit.push_back(y[0]); v_explicit.push_back(y[1]); }
    for (const auto& y : y_semi_implicit) { u_semi_implicit.push_back(y[0]); v_semi_implicit.push_back(y[1]); }
    for (const auto& y : y_implicit) { u_implicit.push_back(y[0]); v_implicit.push_back(y[1]); }
    for (const auto& y : y_analytic) { u_analytic.push_back(y[0]); v_analytic.push_back(y[1]); }

    double u_minimum = min({*min_element(u_explicit.begin(), u_explicit.end()),
                        *min_element(u_semi_implicit.begin(), u_semi_implicit.end()),
                        *min_element(u_implicit.begin(), u_implicit.end()),
                        *min_element(u_analytic.begin(), u_analytic.end())});
    double u_maximum = max({*max_element(u_explicit.begin(), u_explicit.end()),
                        *max_element(u_semi_implicit.begin(), u_semi_implicit.end()),
                        *max_element(u_implicit.begin(), u_implicit.end()),
                        *max_element(u_analytic.begin(), u_analytic.end())});
    double u_margin = 0.1 * (u_maximum - u_minimum);
    u_minimum = max(-1.0, u_minimum - u_margin);
    u_maximum += u_margin;
    
    double v_minimum = min({*min_element(v_explicit.begin(), v_explicit.end()),
                        *min_element(v_semi_implicit.begin(), v_semi_implicit.end()),
                        *min_element(v_implicit.begin(), v_implicit.end()),
                        *min_element(v_analytic.begin(), v_analytic.end())});
    double v_maximum = max({*max_element(v_explicit.begin(), v_explicit.end()),
                        *max_element(v_semi_implicit.begin(), v_semi_implicit.end()),
                        *max_element(v_implicit.begin(), v_implicit.end()),
                        *max_element(v_analytic.begin(), v_analytic.end())});
    double v_margin = 0.1 * (v_maximum - v_minimum);
    v_minimum = max(-1.0, v_minimum - v_margin);
    v_maximum += v_margin;

    Figure figure_u(800, 600);
    PlotStyle style_explicit = figure_u.CreateStyle(Colors::BLUE, 1.0, LineStyle::SOLID);
    PlotStyle style_semi_implicit = figure_u.CreateStyle(Colors::RED, 1.0, LineStyle::SOLID);
    PlotStyle style_implicit = figure_u.CreateStyle(Colors::GREEN, 1.0, LineStyle::SOLID);
    PlotStyle style_analytic = figure_u.CreateStyle(Colors::BLACK, 1.0, LineStyle::SOLID);

    figure_u.Plot(t_explicit, u_explicit, style_explicit);
    figure_u.Plot(t_semi_implicit, u_semi_implicit, style_semi_implicit);
    figure_u.Plot(t_implicit, u_implicit, style_implicit);
    figure_u.Plot(t_analytic, u_analytic, style_analytic);
    figure_u.SetTitle("Solution of u(t) for stiff system");
    figure_u.SetXLabel("t");
    figure_u.SetYLabel("u(t)");
    figure_u.SetLegend({"Explicit method", "Semi implicit method", "Implicit method", "Analytic solution"});
    figure_u.SetXLimit(t_start, t_end);
    figure_u.SetYLimit(u_minimum, u_maximum);
    figure_u.Save("solution_u.svg");

    Figure figure_v(800, 600);
    figure_v.Plot(t_explicit, v_explicit, style_explicit);
    figure_v.Plot(t_semi_implicit, v_semi_implicit, style_semi_implicit);
    figure_v.Plot(t_implicit, v_implicit, style_implicit);
    figure_v.Plot(t_analytic, v_analytic, style_analytic);
    figure_v.SetTitle("Solution of v(t) for stiff system");
    figure_v.SetXLabel("t");
    figure_v.SetYLabel("v(t)");
    figure_v.SetLegend({"Explicit method", "Semi implicit method", "Implicit method", "Analytic solution"});
    figure_v.SetXLimit(t_start, t_end);
    figure_v.SetYLimit(v_minimum, v_maximum);
    figure_v.Save("solution_v.svg");

    return 0;
}
