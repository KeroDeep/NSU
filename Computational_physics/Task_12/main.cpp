#include <iostream>
#include <vector>
#include <cmath>
#include <complex>
#include <valarray>

#include "../Plot_library/graphics.hpp"

using namespace std;
using namespace PlotLibrary;

double signal_modulation(double t, double coefficient_0, double coefficient_1, double frequency_0, double frequency_1) {
    return coefficient_0 * sin(frequency_0 * t) + coefficient_1 * sin(frequency_1 * t);
}

vector<double> apply_hann_window(const vector<double>& data) {
    int intervals_number = data.size();
    vector<double> windowed_data(intervals_number);

    for (int i = 0; i < intervals_number; ++i) {
        double hann = 0.5 * (1 - cos(2 * M_PI * i / (intervals_number - 1)));
        windowed_data[i] = data[i] * hann;
    }

    return windowed_data;
}

void fft_recursive(valarray<complex<double>>& x) {
    const size_t N = x.size();
    if (N <= 1) return;

    valarray<complex<double>> even = x[slice(0, N/2, 2)];
    valarray<complex<double>> odd = x[slice(1, N/2, 2)];

    fft_recursive(even);
    fft_recursive(odd);

    for (size_t k = 0; k < N/2; ++k) {
        complex<double> t = polar(1.0, -2 * M_PI * k / N) * odd[k];
        x[k] = even[k] + t;
        x[k + N/2] = even[k] - t;
    }
}

vector<double> fft_magnitude(const vector<double>& data) {
    int intervals_number = data.size();
    valarray<complex<double>> fft_data(intervals_number);

    for (int i = 0; i < intervals_number; ++i) {
        fft_data[i] = data[i];
    }

    fft_recursive(fft_data);

    vector<double> magnitude(intervals_number / 2 + 1);
    for (int i = 0; i <= intervals_number / 2; ++i) {
        magnitude[i] = norm(fft_data[i]) / intervals_number;
    }

    return magnitude;
}

int main() {
    double t_minimum = 0.0;
    double t_maximum = 2 * M_PI;
    const int intervals_number = 1024;
    double t_step = (t_maximum - t_minimum) / (intervals_number - 1);
    vector<double> t(intervals_number), signal(intervals_number);
    double coefficient_0 = 1.0, coefficient_1 = 0.002;
    double frequency_0 = 5.1;
    double frequency_1 = 25.5;

    for (int i = 0; i < intervals_number; ++i) {
        t[i] = t_minimum + i * t_step;
        signal[i] = signal_modulation(t[i], coefficient_0, coefficient_1, frequency_0, frequency_1);
    }

    vector<double> signal_hann = apply_hann_window(signal);

    vector<double> power_spectrum = fft_magnitude(signal);
    vector<double> power_spectrum_hann = fft_magnitude(signal_hann);

    vector<double> frequency(intervals_number / 2 + 1);
    double sampling_rate = (intervals_number - 1) / (t_maximum - t_minimum);
    double frequency_step = sampling_rate / intervals_number;

    for (int i = 0; i <= intervals_number / 2; ++i) {
        frequency[i] = i * frequency_step;
    }

    Figure Figure(800, 600);
    PlotStyle style_rect = Figure.CreateStyle(Colors::BLUE, 1.0, LineStyle::SOLID);
    PlotStyle style_hann = Figure.CreateStyle(Colors::RED, 1.0, LineStyle::DASHED);

    Figure.Plot(frequency, power_spectrum, style_rect);
    Figure.Plot(frequency, power_spectrum_hann, style_hann);

    double power_maximum = 0.0;

    for (double power : power_spectrum) {
        if (power > power_maximum) {
            power_maximum = power;
        }
    }

    for (double power : power_spectrum_hann) {
        if (power > power_maximum) {
            power_maximum = power;
        }
    }

    Figure.SetYLimit(0, power_maximum * 1.1);
    Figure.SetXLimit(0, frequency[intervals_number / 25]);

    Figure.SetTitle("Power Spectrum");
    Figure.SetXLabel("Frequency (Hz)");
    Figure.SetYLabel("Power");
    Figure.SetLegend({"Rectangular window", "Hann window"});
    Figure.Save("power_spectrum.svg");

    return 0;
}
