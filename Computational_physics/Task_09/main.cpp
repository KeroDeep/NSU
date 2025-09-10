#include <iostream>
#include <vector>
#include <cmath>
#include <algorithm>
#include <limits>

#include "../Plot_library/graphics.hpp"

using namespace std;
using namespace PlotLibrary;

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

vector<double> difference_solver(int N, const string& boundary_conditions_type, const vector<double>& boundary_conditions_values, vector<double>& x_coordinates) {
    double left = -M_PI / 2;
    double right = M_PI / 2;
    double step = (right - left) / N;
    vector<double> x(N + 1);

    for (int i = 0; i <= N; ++i) {
        x[i] = left + i * step;
    }

    x_coordinates = x;

    vector<double> a(N - 1, 1.0);
    vector<double> b(N - 1, -2.0);
    vector<double> c(N - 1, 1.0);
    vector<double> d(N - 1);
    
    double alpha = boundary_conditions_values[0];
    double beta = boundary_conditions_values[1];

    if (boundary_conditions_type == "neumann") {
        for (int i = 0; i < N - 1; ++i) {
            d[i] = 0.0;
        }
    } else {
        for (int i = 0; i < N - 1; ++i) {
            d[i] = step * step * cos(x[i + 1]);
        }
    }

    if (boundary_conditions_type == "dirichlet") {
        d[0] -= alpha;
        d[N - 2] -= beta;
    } 
    else if (boundary_conditions_type == "neumann") {
        b[0] = -2.0 + 1.0;
        d[0] = -step * alpha;
        
        b[N - 2] = -2.0 + 1.0;
        d[N - 2] = step * beta;
    } 
    else if (boundary_conditions_type == "mixed") {
        d[0] -= alpha;
        b[N - 2] = -2.0 + 1.0;
        d[N - 2] = step * step * cos(x[N - 1]) + step * beta;
    } 
    else {
        return {};
    }

    vector<double> y_interior(N - 1);
    tridiagonal_solver(a, b, c, d, y_interior);

    vector<double> y(N + 1);
    
    if (boundary_conditions_type == "dirichlet") {
        y[0] = alpha;
        y[N] = beta;

        for (int i = 1; i < N; ++i) {
            y[i] = y_interior[i - 1];
        }
    } 
    else if (boundary_conditions_type == "neumann") {
        for (int i = 0; i < N - 1; ++i) {
            y[i + 1] = y_interior[i];
        }

        y[0] = y[1] - step * alpha;
        y[N] = y[N - 1] + step * beta;
    } 
    else if (boundary_conditions_type == "mixed") {
        y[0] = alpha;

        for (int i = 0; i < N - 1; ++i) {
            y[i + 1] = y_interior[i];
        }

        y[N] = y[N - 1] + step * beta;
    }

    return y;
}

double dirichlet_boundary_conditions(double x) {
    return -cos(x);
}

double neumann_boundary_conditions(double x) {
    return 0.0;
}

double mixed_boundary_conditions(double x) {
    return -cos(x) - x - M_PI/2;
}

int main() {
    int N = 200;
    vector<string> boundary_conditions_types = {"dirichlet", "neumann", "mixed"};

    vector<vector<double>> boundary_conditions_values = {
        {0, 0},
        {0, 0},
        {0, 0}
    };

    for (size_t i = 0; i < boundary_conditions_types.size(); ++i) {
        vector<double> x;
        vector<double> y = difference_solver(N, boundary_conditions_types[i], boundary_conditions_values[i], x);
        vector<double> y_analytic(x.size());
        
        for (size_t j = 0; j < x.size(); ++j) {
            if (boundary_conditions_types[i] == "dirichlet") {
                y_analytic[j] = dirichlet_boundary_conditions(x[j]);
            } else if (boundary_conditions_types[i] == "neumann") {
                y_analytic[j] = neumann_boundary_conditions(x[j]);
            } else if (boundary_conditions_types[i] == "mixed") {
                y_analytic[j] = mixed_boundary_conditions(x[j]);
            }
        }

        Figure figure(800, 600);
        PlotStyle style_numerical = figure.CreateStyle(Colors::BLUE, 2.0, LineStyle::SOLID);
        PlotStyle style_analytical = figure.CreateStyle(Colors::RED, 2.0, LineStyle::DASHED);
        figure.Plot(x, y, style_numerical);
        figure.Plot(x, y_analytic, style_analytical);
        string boundary_conditions_type;
        
        if (boundary_conditions_types[i] == "dirichlet") {
            boundary_conditions_type = "Dirichlet";
        } else if (boundary_conditions_types[i] == "neumann") {
            boundary_conditions_type = "Neumann";
        } else if (boundary_conditions_types[i] == "mixed") {
            boundary_conditions_type = "Mixed";
        }

        figure.SetTitle("Solution for " + boundary_conditions_type + " boundary conditions");
        figure.SetXLabel("x");
        figure.SetYLabel("y");
        figure.SetLegend({"Numerical solution", "Analytical solution"});
        figure.SetXLimit(-M_PI / 2, M_PI / 2);

        double y_minimum_numerical = *min_element(y.begin(), y.end());
        double y_maximum_numerical = *max_element(y.begin(), y.end());
        double y_minimum_analytical = *min_element(y_analytic.begin(), y_analytic.end());
        double y_maximum_analytical = *max_element(y_analytic.begin(), y_analytic.end());
        double y_minimum = min(y_minimum_numerical, y_minimum_analytical);
        double y_maximum = max(y_maximum_numerical, y_maximum_analytical);
        
        if (abs(y_maximum - y_minimum) < 1e-10) {
            y_minimum = -1.0;
            y_maximum = 1.0;
        }
        
        double margin = 0.1 * (y_maximum - y_minimum);
        
        figure.SetYLimit(y_minimum - margin, y_maximum + margin);
        figure.Grid(true);
        figure.Save("solution_" + boundary_conditions_types[i] + ".svg");
    }

    return 0;
}