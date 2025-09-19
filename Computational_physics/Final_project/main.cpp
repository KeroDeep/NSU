#include <iostream>
#include <vector>
#include <cmath>
#include <functional>
#include <complex>
#include <algorithm>
#include <sstream>
#include <iomanip>
#include <fstream>

using namespace std;

void calculate_right_part(const vector<double>& u, const vector<double>& v, vector<double>& du_dt, vector<double>& dv_dt, double gamma, double b_1, double b_2, double x_step) {
    int n = u.size();
    du_dt.resize(n);
    dv_dt.resize(n);

    du_dt[0] = 0.0;
    du_dt[n - 1] = 0.0;
    dv_dt[0] = 0.0;
    dv_dt[n - 1] = 0.0;

    for (int i = 1; i < n - 1; ++i) {
        double square_modulus_A = u[i] * u[i] + v[i] * v[i];
        double nonlinear_u = gamma * u[i] - square_modulus_A * u[i] - b_1 * square_modulus_A * v[i];
        double nonlinear_v = gamma * v[i] + b_1 * square_modulus_A * u[i] - square_modulus_A * v[i];

        double second_u = (u[i - 1] - 2 * u[i] + u[i + 1]) / (x_step * x_step);
        double second_v = (v[i - 1] - 2 * v[i] + v[i + 1]) / (x_step * x_step);

        du_dt[i] = nonlinear_u + second_u - b_2 * second_v;
        dv_dt[i] = nonlinear_v + b_2 * second_u + second_v;
    }
}

void calculate_fourier_coefficients(const vector<double>& u, const vector<double>& v, double x_step, complex<double>& F_1, complex<double>& F_2) {
    F_1 = 0.0;
    F_2 = 0.0;
    
    for (int j = 0; j < u.size(); ++j) {
        double x_j = j * x_step;
        complex<double> A(u[j], v[j]);
        F_1 += A * sin(M_PI * x_j) * x_step;
        F_2 += A * sin(2 * M_PI * x_j) * x_step;
    }
}

void runge_kutta_step(vector<double>& u, vector<double>& v, double gamma, double b_1, double b_2, double x_step, double t_step) {
    int n = u.size();
    
    vector<double> k_1_u(n), k_1_v(n);
    vector<double> k_2_u(n), k_2_v(n);
    vector<double> temporary_u(n), temporary_v(n);
    
    calculate_right_part(u, v, k_1_u, k_1_v, gamma, b_1, b_2, x_step);
    
    for (int i = 1; i < n - 1; ++i) {
        temporary_u[i] = u[i] + 0.5 * t_step * k_1_u[i];
        temporary_v[i] = v[i] + 0.5 * t_step * k_1_v[i];
    }
    
    calculate_right_part(temporary_u, temporary_v, k_2_u, k_2_v, gamma, b_1, b_2, x_step);
    
    for (int i = 1; i < n - 1; ++i) {
        u[i] += t_step * k_2_u[i];
        v[i] += t_step * k_2_v[i];
    }
}

void save_gnuplot_data(const vector<double>& x_data, const vector<double>& y_data, const string& filename) {
    ofstream file(filename);

    for (size_t i = 0; i < x_data.size(); ++i) {
        file << x_data[i] << " " << y_data[i] << endl;
    }

    file.close();
}

void create_gnuplot_script(const vector<string>& data_files, const vector<string>& legend_labels, const string& output_file, double x_min, double x_max, double y_min, double y_max) {
    ofstream script("plot_script.gp");
    
    script << "set terminal pngcairo enhanced font 'Arial,12' size 800,600" << endl;
    script << "set output '" << output_file << ".png'" << endl;
    script << "set title 'Phase trajectory |F_1| from |F_2| for different gamma'" << endl;
    script << "set xlabel '|F_1|'" << endl;
    script << "set ylabel '|F_2|'" << endl;
    script << "set grid" << endl;
    script << "set key outside right top" << endl;
    script << "set xrange [" << x_min << ":" << x_max << "]" << endl;
    script << "set yrange [" << y_min << ":" << y_max << "]" << endl;
    script << "set format x '%.4f'" << endl;
    script << "set format y '%.4f'" << endl;
    script << endl;
    script << "plot \\" << endl;
    
    for (size_t i = 0; i < data_files.size(); ++i) {
        script << "    '" << data_files[i] << "' with lines title '" << legend_labels[i] << "' lw 2";
        if (i != data_files.size() - 1) script << ", \\";
        script << endl;
    }
    
    script.close();
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

    vector<double> gammas = {0.0, 0.5, 1.0, 2.0, 5.0, 10.0};

    double minimum_F_1 = 1e10, maximum_F_1 = -1e10;
    double minimum_F_2 = 1e10, maximum_F_2 = -1e10;

    vector<string> data_files;
    vector<string> legend_labels;

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
            runge_kutta_step(u, v, gamma_value, b_1, b_2, x_step, t_step);
            
            for (int j = 1; j < x_steps_number; ++j) {
                if (abs(u[j]) > 1e6 || abs(v[j]) > 1e6) {
                    stable = false;
                    break;
                }
            }

            if (!stable) {
                break;
            }

            complex<double> F_1, F_2;
            calculate_fourier_coefficients(u, v, x_step, F_1, F_2);

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
            stringstream filename;
            filename << "gamma_" << fixed << setprecision(1) << gamma_value << ".dat";
            string data_file = filename.str();
            
            save_gnuplot_data(F_1_history, F_2_history, data_file);
            data_files.push_back(data_file);
            
            stringstream legend;
            legend << "gamma = " << fixed << setprecision(1) << gamma_value;
            legend_labels.push_back(legend.str());
        }
    }

    double margin_F_1 = (maximum_F_1 - minimum_F_1) * 0.1;
    double margin_F_2 = (maximum_F_2 - minimum_F_2) * 0.1;
    
    double x_min = minimum_F_1 - margin_F_1;
    double x_max = maximum_F_1 + margin_F_1 + 0.00015;
    double y_min = minimum_F_2 - margin_F_2;
    double y_max = maximum_F_2 + margin_F_2;

    create_gnuplot_script(data_files, legend_labels, "phase_curve", x_min, x_max, y_min, y_max);

    return 0;
}
