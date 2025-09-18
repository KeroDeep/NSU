#include <iostream>
#include <vector>
#include <cmath>
#include <functional>
#include <complex>
#include <algorithm>
#include <sstream>
#include <iomanip>

#include "../Plot_library/graphics.hpp"

using namespace std;
using namespace PlotLibrary;

void right_part(const vector<double>& u, const vector<double>& v, vector<double>& du_dt, vector<double>& dv_dt, double gamma, double b_1, double b_2, double x_step) {
    int n = u.size();
    du_dt.resize(n);
    dv_dt.resize(n);

    du_dt[0] = 0.0;
    du_dt[n - 1] = 0.0;
    dv_dt[0] = 0.0;
    dv_dt[n - 1] = 0.0;

    for (int i = 1; i < n - 1; ++i) {
        double abs_A_sq = u[i] * u[i] + v[i] * v[i];
        double nonlinear_u = gamma * u[i] - abs_A_sq * u[i] - b_1 * abs_A_sq * v[i];
        double nonlinear_v = gamma * v[i] + b_1 * abs_A_sq * u[i] - abs_A_sq * v[i];

        double second_u = (u[i - 1] - 2 * u[i] + u[i + 1]) / (x_step * x_step);
        double second_v = (v[i - 1] - 2 * v[i] + v[i + 1]) / (x_step * x_step);

        du_dt[i] = nonlinear_u + second_u - b_2 * second_v;
        dv_dt[i] = nonlinear_v + b_2 * second_u + second_v;
    }
}

int main() {
    const double b_1 = 10.0;
    const double b_2 = 1.0;
    const int x_steps_number = 100;
    const double x_start = 0.0;
    const double x_end = 1.0;
    const double x_step = (x_end - x_start) / x_steps_number;
    const double t_step = 0.000001;
    const double t_start = 0.0;
    const double t_end = 0.001;

    vector<double> u(x_steps_number + 1, 0.0);
    vector<double> v(x_steps_number + 1, 0.0);

    for (int i = 0; i <= x_steps_number; ++i) {
        double x_i = i * x_step;
        u[i] = x_i * x_i * (1 - x_i);
        v[i] = 0.0;
    }

    vector<double> final_F_1_values;
    vector<double> final_F_2_values;

    vector<double> gammas = {
        0.0,
        0.5,
        1.0,
        2.0,
        5.0,
        10.0
    };

    vector<Color> colors = {
        Color(255, 0, 0),
        Color(0, 255, 0),
        Color(0, 0, 255),
        Color(255, 100, 100),
        Color(0, 100, 255),
        Color(100, 100, 100),
    };

    Figure figure(800, 600);

    double minimum_F_1 = 1e10, maximum_F_1 = -1e10;
    double minimum_F_2 = 1e10, maximum_F_2 = -1e10;

    cout << endl;

    for (size_t i = 0; i < gammas.size(); ++i) {
        double gamma_value = gammas[i];
        
        for (int j = 0; j <= x_steps_number; ++j) {
            double x_j = j * x_step;
            u[j] = x_j * x_j * (1 - x_j);
            v[j] = 0.0;
        }

        double t_current = t_start;
        bool stable = true;

        vector<double> F_1_history;
        vector<double> F_2_history;

        while (t_current < t_end && stable) {
            vector<double> du_dt, dv_dt;
            right_part(u, v, du_dt, dv_dt, gamma_value, b_1, b_2, x_step);

            for (int j = 1; j < x_steps_number; ++j) {
                u[j] += t_step * du_dt[j];
                v[j] += t_step * dv_dt[j];
                
                if (abs(u[j]) > 1e6 || abs(v[j]) > 1e6) {
                    stable = false;
                    break;
                }
            }

            if (!stable) {
                break;
            }

            complex<double> F_1 = 0.0, F_2 = 0.0;

            for (int j = 0; j <= x_steps_number; ++j) {
                double x_j = j * x_step;
                F_1 += complex<double>(u[j], v[j]) * sin(M_PI * x_j) * x_step;
                F_2 += complex<double>(u[j], v[j]) * sin(2 * M_PI * x_j) * x_step;
            }

            double module_F_1 = abs(F_1);
            double module_F_2 = abs(F_2);

            F_1_history.push_back(module_F_1);
            F_2_history.push_back(module_F_2);

            minimum_F_1 = min(minimum_F_1, module_F_1);
            maximum_F_1 = max(maximum_F_1, module_F_1);
            minimum_F_2 = min(minimum_F_2, module_F_2);
            maximum_F_2 = max(maximum_F_2, module_F_2);

            t_current += t_step;
        }

        if (stable && !F_1_history.empty()) {
            final_F_1_values.push_back(F_1_history.back());
            final_F_2_values.push_back(F_2_history.back());

            PlotStyle style = figure.CreateStyle(colors[i], 1.5, LineStyle::SOLID);
            figure.Plot(F_1_history, F_2_history, style);

            cout << "Gamma = " << gamma_value << ": |F_1| range = [";
            cout << *min_element(F_1_history.begin(), F_1_history.end()) << ", ";
            cout << *max_element(F_1_history.begin(), F_1_history.end()) << "], ";
            cout << "|F_2| range = [";
            cout << *min_element(F_2_history.begin(), F_2_history.end()) << ", ";
            cout << *max_element(F_2_history.begin(), F_2_history.end()) << "]";
            cout << endl;
        }
    }

    cout << endl;

    double margin_F_1 = (maximum_F_1 - minimum_F_1) * 0.1;
    double margin_F_2 = (maximum_F_2 - minimum_F_2) * 0.1;
    
    figure.SetXLimit(minimum_F_1 - margin_F_1, maximum_F_1 + margin_F_1 + 0.00015);
    figure.SetYLimit(minimum_F_2 - margin_F_2, maximum_F_2 + margin_F_2);
    
    cout << "Global limits: X = [" << minimum_F_1 - margin_F_1 << ", " << maximum_F_1 + margin_F_1 << "], Y = [" << minimum_F_2 - margin_F_2 << ", " << maximum_F_2 + margin_F_2 << "]" << endl;

    cout << endl;

    figure.SetTitle("Phase trajectory |F_1| from |F_2| for different gamma");
    
    vector<string> legend_labels;

    for (double gamma : gammas) {
        stringstream ss;
        ss << fixed << setprecision(1) << gamma;
        legend_labels.push_back("gamma = " + ss.str());
    }

    figure.SetXPrecision(7);
    figure.SetYPrecision(7);

    figure.SetLegend(legend_labels);
    figure.Grid(true);
    figure.Save("phase_curve.svg");

    return 0;
}
