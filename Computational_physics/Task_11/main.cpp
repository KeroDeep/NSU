#include <iostream>
#include <vector>
#include <cmath>
#include <iomanip>

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

double U(double x) {
    return 0.5 * x * x;
}

void build_hamiltonian(vector<double>& a, vector<double>& b, vector<double>& c, const vector<double>& x, int intervals_number, double step) {
    a.resize(intervals_number - 1);
    b.resize(intervals_number);
    c.resize(intervals_number - 1);

    for (int i = 0; i < intervals_number - 1; ++i) {
        a[i] = -0.5 / (step * step);
        c[i] = -0.5 / (step * step);
    }

    for (int i = 0; i < intervals_number; ++i) {
        b[i] = 1.0 / (step * step) + U(x[i]);
    }

    b[0] = 1.0; a[0] = 0.0;
    c[intervals_number - 2] = 0.0; b[intervals_number - 1] = 1.0;
}

void tridiagonal_solve(const vector<double>& a, const vector<double>& b, const vector<double>& c, const vector<double>& u, vector<double>& v, double mu, int intervals_number) {
    int n = b.size();
    vector<double> a_new = a;
    vector<double> b_new(n);

    for (int i = 0; i < n; ++i) {
        b_new[i] = b[i] - mu;
    }

    vector<double> alpha(n - 1), beta(n);

    b_new[0] = 1.0;
    alpha[0] = -c[0] / b_new[0];
    beta[0] = u[0] / b_new[0];

    for (int i = 1; i < n - 1; ++i) {
        alpha[i] = -c[i] / (b_new[i] + a_new[i] * alpha[i - 1]);
        beta[i] = (u[i] - a_new[i] * beta[i - 1]) / (b_new[i] + a_new[i] * alpha[i - 1]);
    }

    b_new[n - 1] = 1.0;
    beta[n - 1] = u[n - 1] / b_new[n - 1];
    alpha[n - 2] = 0.0;

    v[n - 1] = beta[n - 1];

    for (int i = n - 2; i >= 0; --i) {
        v[i] = alpha[i] * v[i + 1] + beta[i];
    }
}

double normalize(vector<double>& v, double step) {
    double norm = 0.0;

    for (double val : v) {
        norm += val * val;
    }

    norm = sqrt(norm * step);

    if (norm > 0.0) {
        for (double& val : v) {
            val /= norm;
        }
    }

    return norm;
}

void inverse_iteration(const vector<double>& a, const vector<double>& b, const vector<double>& c, vector<double>& psi, double& energy_numerical, double mu, int max_iter, double tolerance, int intervals_number, double step) {
    int n = b.size();
    vector<double> u(n, 1.0);
    normalize(u, step);

    double E_old = 0.0;

    for (int iter = 0; iter < max_iter; ++iter) {
        vector<double> v(n);
        tridiagonal_solve(a, b, c, u, v, mu, intervals_number);

        normalize(v, step);
        
        energy_numerical = 0.0;

        for (int i = 0; i < n; ++i) {
            double hu_i = b[i] * v[i];

            if (i > 0) {
                hu_i += a[i - 1] * v[i - 1];
            }

            if (i < n - 1) {
                hu_i += c[i] * v[i + 1];
            }

            energy_numerical += v[i] * hu_i;
        }

        if (abs(energy_numerical - E_old) < tolerance) {
            break;
        }

        E_old = energy_numerical;
        u = v;
    }

    psi = u;
}

int main() {
    cout.precision(find_mantissa_bits_double());

    const double pit_boundary = 10.0;
    const int intervals_number = 100;
    const double step = 2 * pit_boundary / intervals_number;
    const double tolerance = 1e-6;
    const int max_iter = 100;
    vector<double> x(intervals_number);

    double energy_numerical;
    double energy_analytic = 0.5;

    for (int i = 0; i < intervals_number; ++i) {
        x[i] = -pit_boundary + i * step;
    }

    vector<double> a, b, c;
    build_hamiltonian(a, b, c, x, intervals_number, step);

    vector<double> psi(intervals_number);
    inverse_iteration(a, b, c, psi, energy_numerical, 0.4, max_iter, tolerance, intervals_number, step);

    double norm = 0.0;

    for (double val : psi) {
        norm += val * val;
    }

    norm = sqrt(norm * step);

    for (double& val : psi) {
        val /= norm;
    }

    vector<double> psi_anal(intervals_number);

    for (int i = 0; i < intervals_number; ++i) {
        psi_anal[i] = pow(1.0 / M_PI, 0.25) * exp(-0.5 * x[i] * x[i]);
    }

    Figure figure(800, 600);
    PlotStyle style_numerical = figure.CreateStyle(Colors::BLUE, 1.0, LineStyle::SOLID);
    PlotStyle style_analytic = figure.CreateStyle(Colors::RED, 1.0, LineStyle::DASHED);

    figure.Plot(x, psi, style_numerical);
    figure.Plot(x, psi_anal, style_analytic);

    figure.SetTitle("Wave function graphic");
    figure.SetXLabel("x");
    figure.SetYLabel("Psi_0(x)");
    figure.SetLegend({"Numerical solution", "Analytic solution"});
    figure.Save("wave_function.svg");

    cout << endl;

    cout << "Ground state energy for numerical solution: " << energy_numerical << endl;
    cout << "Ground state energy for analytic solution: " << energy_analytic << endl;

    cout << endl;

    return 0;
}
