#include <vector>
#include <cmath>
#include <cstdlib>
#include <iostream>
#include <fstream>
#include <algorithm>
#include <string>
#include <limits>
#include <cstdio>

#include "graphics.hpp"

#ifdef _WIN32
#include <windows.h>
#include <shellapi.h>
#endif

namespace PlotLibrary {
    Color::Color(double red, double green, double blue, double alpha) : r(red), g(green), b(blue), a(alpha) {}

    const Color Colors::RED = Color(1.0, 0.0, 0.0);
    const Color Colors::BLUE = Color(0.0, 0.0, 1.0);
    const Color Colors::GREEN = Color(0.0, 0.5, 0.0);
    const Color Colors::BLACK = Color(0.0, 0.0, 0.0);
    const Color Colors::WHITE = Color(1.0, 1.0, 1.0);
    const Color Colors::YELLOW = Color(1.0, 1.0, 0.0);
    const Color Colors::CYAN = Color(0.0, 1.0, 1.0);
    const Color Colors::MAGENTA = Color(1.0, 0.0, 1.0);
    const Color Colors::ORANGE = Color(1.0, 0.65, 0.0);
    const Color Colors::PURPLE = Color(0.5, 0.0, 0.5);
    const Color Colors::PINK = Color(1.0, 0.75, 0.8);
    const Color Colors::BROWN = Color(0.65, 0.16, 0.16);
    const Color Colors::GRAY = Color(0.5, 0.5, 0.5);
    const Color Colors::LIGHT_BLUE = Color(0.68, 0.85, 0.9);
    const Color Colors::LIGHT_GREEN = Color(0.56, 0.93, 0.56);
    const Color Colors::LIGHT_RED = Color(1.0, 0.71, 0.71);

    std::string Color::ToHex() const {
        char buffer[8];
        std::snprintf(buffer, sizeof(buffer), "#%02X%02X%02X", int(r * 255), int(g * 255), int(b * 255));
        return std::string(buffer);
    }

    std::string Color::ToRgb() const {
        char buffer[64];
        std::snprintf(buffer, sizeof(buffer), "rgb(%d,%d,%d)", int(r * 255), int(g * 255), int(b * 255));
        return std::string(buffer);
    }

    PlotStyle::PlotStyle() {
        color = Colors::BLUE;
        line_width = 1.0;
        line_style = LineStyle::SOLID;
        marker_style = MarkerStyle::NONE;
        marker_size = 6.0;
        marker_edge_width = 1.0;
        marker_edge_color = Colors::BLACK;
        marker_face_color = Colors::BLUE;
        alpha = 1.0;
        fill = false;
        fill_color = Colors::BLUE;
        plot_type = PlotType::LINE;
    }

    AxisLimits::AxisLimits() {
        x_min = 0;
        x_max = 1;
        y_min = 0;
        y_max = 1;
        auto_x = true;
        auto_y = true;
    }

    TextProperties::TextProperties() {
        font_family = "Arial";
        font_size = 12;
        color = Colors::BLACK;
        horizontal_alignment = "center";
        vertical_alignment = "center";
        rotation = 0;
        bold = false;
        italic = false;
    }

    LegendProperties::LegendProperties() {
        location = "upper right";
        columns = 1;
        frame_visible = true;
        frame_color = Colors::WHITE;
        frame_alpha = 0.8;
    }

    Figure::Figure(double width, double height, double dpi_value) {
        figure_width = width;
        figure_height = height;
        dpi = dpi_value;
        grid_visible = true;
        grid_color = Colors::GRAY;
        grid_alpha = 0.3;
        background_color = Colors::WHITE;
        axis_color = Colors::BLACK;
        
        title_properties.font_size = 16;
        title_properties.bold = true;
        label_properties.font_size = 14;
    }

    void Figure::Plot(const std::vector<double>& x, const std::vector<double>& y, const PlotStyle& style) {
        data_x.push_back(x);
        data_y.push_back(y);
        
        PlotStyle new_style = style;
        new_style.plot_type = PlotType::LINE;
        styles.push_back(new_style);
        
        if (limits.auto_x || limits.auto_y) {
            CalculateAutoLimits();
        }
    }

    void Figure::Scatter(const std::vector<double>& x, const std::vector<double>& y, const PlotStyle& style) {
        data_x.push_back(x);
        data_y.push_back(y);
        
        PlotStyle new_style = style;
        new_style.plot_type = PlotType::SCATTER;
        
        if (new_style.marker_style == MarkerStyle::NONE) {
            new_style.marker_style = MarkerStyle::CIRCLE;
        }

        styles.push_back(new_style);
        
        if (limits.auto_x || limits.auto_y) {
            CalculateAutoLimits();
        }
    }

    void Figure::Bar(const std::vector<double>& x, const std::vector<double>& heights, const PlotStyle& style) {
        data_x.push_back(x);
        data_y.push_back(heights);
        
        PlotStyle new_style = style;
        new_style.plot_type = PlotType::BAR;
        new_style.fill = true;
        styles.push_back(new_style);
        
        if (limits.auto_x || limits.auto_y) {
            CalculateAutoLimits();
        }
    }

    void Figure::Histogram(const std::vector<double>& data, int bins, const PlotStyle& style) {
        if (data.empty()) {
            return;
        }
        
        double min_val = *std::min_element(data.begin(), data.end());
        double max_val = *std::max_element(data.begin(), data.end());
        double bin_width = (max_val - min_val) / bins;
        
        std::vector<double> histogram(bins, 0);
        std::vector<double> bin_centers(bins);
        
        for (int i = 0; i < bins; i++) {
            bin_centers[i] = min_val + (i + 0.5) * bin_width;
        }
        
        for (double value : data) {
            int bin_index = std::min(int((value - min_val) / bin_width), bins - 1);
            histogram[bin_index]++;
        }
        
        Bar(bin_centers, histogram, style);
    }

    void Figure::CalculateAutoLimits() {
        if (data_x.empty()) {
            return;
        }
        
        double x_min = std::numeric_limits<double>::max();
        double x_max = std::numeric_limits<double>::lowest();
        double y_min = std::numeric_limits<double>::max();
        double y_max = std::numeric_limits<double>::lowest();
        
        for (size_t i = 0; i < data_x.size(); i++) {
            if (data_x[i].empty()) {
                continue;
            }
            
            auto x_min_max = std::minmax_element(data_x[i].begin(), data_x[i].end());
            auto y_min_max = std::minmax_element(data_y[i].begin(), data_y[i].end());
            
            x_min = std::min(x_min, *x_min_max.first);
            x_max = std::max(x_max, *x_min_max.second);
            y_min = std::min(y_min, *y_min_max.first);
            y_max = std::max(y_max, *y_min_max.second);
        }
        
        double x_range = x_max - x_min;
        double y_range = y_max - y_min;
        double padding_x = x_range * 0.05;
        double padding_y = y_range * 0.05;
        
        if (limits.auto_x) {
            limits.x_min = x_min - padding_x;
            limits.x_max = x_max + padding_x;
        }
        
        if (limits.auto_y) {
            limits.y_min = y_min - padding_y;
            limits.y_max = y_max + padding_y;
        }
    }

    std::string Figure::GetLineStyle(LineStyle style) const {
        switch (style) {
            case LineStyle::DASHED: {
                return "5,5";
            }

            case LineStyle::DOTTED: {
                return "2,2";
            }

            case LineStyle::DASHDOT: {
                return "5,2,2,2";
            }

            case LineStyle::NONE: {
                return "none";
            }

            default: {
                return "";
            }
        }
    }

    std::string Figure::GetMarkerPath(MarkerStyle style) const {
        switch (style) {
            case MarkerStyle::CIRCLE: {
                return "M -3,0 A 3,3 0 1,1 3,0 A 3,3 0 1,1 -3,0";
            }

            case MarkerStyle::SQUARE: {
                return "M -3,-3 L 3,-3 L 3,3 L -3,3 Z";
            }

            case MarkerStyle::DIAMOND: {
                return "M 0,-4 L 4,0 L 0,4 L -4,0 Z";
            }

            case MarkerStyle::TRIANGLE_UP: {
                return "M 0,-4 L 3.5,3 L -3.5,3 Z";
            }

            case MarkerStyle::TRIANGLE_DOWN: {
                return "M 0,4 L 3.5,-3 L -3.5,-3 Z";
            }

            case MarkerStyle::PLUS: {
                return "M -4,0 L 4,0 M 0,-4 L 0,4";
            }

            case MarkerStyle::CROSS: {
                return "M -3,-3 L 3,3 M 3,-3 L -3,3";
            }

            case MarkerStyle::STAR: {
                return "M 0,-5 L 2,-2 L 5,0 L 2,2 L 0,5 L -2,2 L -5,0 L -2,-2 Z";
            }

            default: {
                return "";
            }
        }
    }

    void Figure::Save(const std::string& filename, const std::string& format) {
        if (format != "svg") {
            std::cerr << "Only SVG format is supported" << std::endl;
            return;
        }
        
        std::ofstream svg_file(filename);

        if (!svg_file) {
            std::cerr << "Cannot open file: " << filename << std::endl;
            return;
        }
        
        last_saved_filename = filename;
        
        svg_file << "<?xml version='1.0' encoding='UTF-8'?>\n";
        svg_file << "<svg width='" << figure_width << "' height='" << figure_height << "' ";
        svg_file << "xmlns='http://www.w3.org/2000/svg'>\n";
        
        svg_file << "<rect width='100%' height='100%' fill='" << background_color.ToHex() << "'/>\n";
        
        double margin = 80;
        double plot_width = figure_width - 2 * margin;
        double plot_height = figure_height - 2 * margin;
        
        if (grid_visible) {
            svg_file << "<g stroke='" << grid_color.ToHex() << "' stroke-width='1' stroke-dasharray='";
            svg_file << (grid_style == "--" ? "5,5" : "2,2") << "' opacity='" << grid_alpha << "'>\n";
            
            for (int i = 0; i <= 10; i++) {
                double x = margin + (i * plot_width / 10);
                svg_file << "<line x1='" << x << "' y1='" << margin << "' x2='" << x;
                svg_file << "' y2='" << figure_height - margin << "'/>\n";
            }
            
            for (int i = 0; i <= 10; i++) {
                double y = margin + (i * plot_height / 10);
                svg_file << "<line x1='" << margin << "' y1='" << y << "' x2='";
                svg_file << figure_width - margin << "' y2='" << y << "'/>\n";
            }

            svg_file << "</g>\n";
        }
        
        svg_file << "<g stroke='" << axis_color.ToHex() << "' stroke-width='2'>\n";
        svg_file << "<line x1='" << margin << "' y1='" << figure_height - margin << "' x2='";
        svg_file << figure_width - margin << "' y2='" << figure_height - margin << "'/>\n";
        svg_file << "<line x1='" << margin << "' y1='" << margin << "' x2='";
        svg_file << margin << "' y2='" << figure_height - margin << "'/>\n";
        svg_file << "</g>\n";
        
        auto transform_x = [&](double x) {
            return margin + (x - limits.x_min) * plot_width / (limits.x_max - limits.x_min);
        };
        
        auto transform_y = [&](double y) {
            return figure_height - margin - (y - limits.y_min) * plot_height / (limits.y_max - limits.y_min);
        };
        
        for (size_t i = 0; i < data_x.size(); i++) {
            const auto& x_data = data_x[i];
            const auto& y_data = data_y[i];
            const auto& style = styles[i];
            
            if (x_data.empty()) {
                continue;
            }
            
            if (style.plot_type == PlotType::LINE && style.line_style != LineStyle::NONE) {
                svg_file << "<polyline points='";

                for (size_t j = 0; j < x_data.size(); j++) {
                    double x = transform_x(x_data[j]);
                    double y = transform_y(y_data[j]);
                    svg_file << x << "," << y << " ";
                }

                svg_file << "' fill='none' stroke='" << style.color.ToHex();
                svg_file << "' stroke-width='" << style.line_width;
                svg_file << "' stroke-dasharray='" << GetLineStyle(style.line_style);
                svg_file << "' opacity='" << style.alpha << "'/>\n";
            }
            
            if (style.marker_style != MarkerStyle::NONE) {
                for (size_t j = 0; j < x_data.size(); j++) {
                    double x = transform_x(x_data[j]);
                    double y = transform_y(y_data[j]);
                    
                    std::string marker_path = GetMarkerPath(style.marker_style);

                    if (!marker_path.empty()) {
                        svg_file << "<path d='" << marker_path << "' transform='translate(" << x << "," << y << ")'";
                        svg_file << " fill='" << style.marker_face_color.ToHex() << "' stroke='";
                        svg_file << style.marker_edge_color.ToHex() << "' stroke-width='";
                        svg_file << style.marker_edge_width << "' opacity='" << style.alpha << "'/>\n";
                    }
                }
            }
            
            if (style.plot_type == PlotType::BAR) {
                double bar_width = plot_width / (x_data.size() * 2);
                
                double base_y = limits.y_min;
                
                for (size_t j = 0; j < x_data.size(); j++) {
                    double y_value = y_data[j];
                    
                    if (y_value < limits.y_min || y_value > limits.y_max) {
                        continue;
                    }
                    
                    double x = transform_x(x_data[j]) - bar_width / 2;
                    double y_base = transform_y(base_y);
                    double y_top = transform_y(y_value);
                    double rect_y = std::min(y_base, y_top);
                    double rect_height = std::abs(y_base - y_top);
                    
                    svg_file << "<rect x='" << x << "' y='" << rect_y << "' width='" << bar_width;
                    svg_file << "' height='" << rect_height << "' fill='" << style.color.ToHex();
                    svg_file << "' opacity='" << style.alpha << "'/>\n";
                }
            }
        }
        
        if (!title.empty()) {
            svg_file << "<text x='" << figure_width / 2 << "' y='" << margin / 2;
            svg_file << "' text-anchor='middle' font-family='" << title_properties.font_family;
            svg_file << "' font-size='" << title_properties.font_size << "' fill='";
            svg_file << title_properties.color.ToHex() << "'";
            
            if (title_properties.bold) {
                svg_file << " font-weight='bold'";
            }

            if (title_properties.italic) {
                svg_file << " font-style='italic'";
            }

            svg_file << ">" << title << "</text>\n";
        }
        
        if (!x_label.empty()) {
            svg_file << "<text x='" << figure_width / 2 << "' y='" << figure_height - margin / 3;
            svg_file << "' text-anchor='middle' font-family='" << label_properties.font_family;
            svg_file << "' font-size='" << label_properties.font_size << "' fill='";
            svg_file << label_properties.color.ToHex() << "'>" << x_label << "</text>\n";
        }
        
        if (!y_label.empty()) {
            svg_file << "<text x='" << margin / 3 << "' y='" << figure_height / 2;
            svg_file << "' text-anchor='middle' transform='rotate(-90," << margin / 3;
            svg_file << "," << figure_height / 2 << ")' font-family='" << label_properties.font_family;
            svg_file << "' font-size='" << label_properties.font_size << "' fill='";
            svg_file << label_properties.color.ToHex() << "'>" << y_label << "</text>\n";
        }
        
        if (!legend_labels.empty()) {
            double legend_x = figure_width - margin - 100;
            double legend_y = margin + 20;
            
            if (legend_properties.frame_visible) {
                svg_file << "<rect x='" << legend_x - 10 << "' y='" << legend_y - 10;
                svg_file << "' width='120' height='" << legend_labels.size() * 25 + 10;
                svg_file << "' fill='" << legend_properties.frame_color.ToHex();
                svg_file << "' opacity='" << legend_properties.frame_alpha;
                svg_file << "' stroke='black' stroke-width='1'/>\n";
            }
            
            for (size_t i = 0; i < legend_labels.size(); i++) {
                if (i < styles.size()) {
                    const auto& style = styles[i];
                    double y = legend_y + i * 25;
                    
                    svg_file << "<line x1='" << legend_x << "' y1='" << y + 5;
                    svg_file << "' x2='" << legend_x + 20 << "' y2='" << y + 5;
                    svg_file << "' stroke='" << style.color.ToHex() << "' stroke-width='2'/>\n";
                    
                    if (style.marker_style != MarkerStyle::NONE) {
                        std::string marker_path = GetMarkerPath(style.marker_style);
                        svg_file << "<path d='" << marker_path << "' transform='translate(";
                        svg_file << legend_x + 10 << "," << y + 5 << ")' fill='";
                        svg_file << style.marker_face_color.ToHex() << "' stroke='";
                        svg_file << style.marker_edge_color.ToHex() << "' stroke-width='1'/>\n";
                    }
                    
                    svg_file << "<text x='" << legend_x + 30 << "' y='" << y + 8;
                    svg_file << "' font-family='Arial' font-size='12'>" << legend_labels[i] << "</text>\n";
                }
            }
        }
        
        svg_file << "</svg>\n";
        svg_file.close();
    }

    void Figure::Show() {
        if (last_saved_filename.empty()) {
            std::cerr << "No file has been saved yet to display" << std::endl;
            return;
        }
        
        #ifdef _WIN32
            ShellExecuteA(NULL, "open", last_saved_filename.c_str(), NULL, NULL, SW_SHOWNORMAL);
        #elif __APPLE__
            system(("open " + last_saved_filename).c_str());
        #else
            system(("xdg-open " + last_saved_filename).c_str());
        #endif
    }

    void Figure::SetTitle(const std::string& title_text, const TextProperties& props) {
        title = title_text;
        title_properties = props;
    }

    void Figure::SetXLabel(const std::string& label, const TextProperties& props) {
        x_label = label;

        if (props.font_size > 0) {
            label_properties = props;
        }
    }

    void Figure::SetYLabel(const std::string& label, const TextProperties& props) {
        y_label = label;

        if (props.font_size > 0) {
            label_properties = props;
        }
    }

    void Figure::SetLegend(const std::vector<std::string>& labels, const LegendProperties& props) {
        legend_labels = labels;
        legend_properties = props;
    }

    void Figure::SetXLimit(double min, double max) {
        limits.x_min = min;
        limits.x_max = max;
        limits.auto_x = false;
    }

    void Figure::SetYLimit(double min, double max) {
        limits.y_min = min;
        limits.y_max = max;
        limits.auto_y = false;
    }

    void Figure::Grid(bool visible, const std::string& style, Color color, double alpha) {
        grid_visible = visible;
        grid_style = style;
        grid_color = color;
        grid_alpha = alpha;
    }

    Color Figure::CreateColor(double r, double g, double b, double a) {
        return Color(r, g, b, a);
    }

    PlotStyle Figure::CreateStyle(Color color, double line_width, LineStyle line_style, MarkerStyle marker_style, double marker_size) {
        PlotStyle style;
        style.color = color;
        style.line_width = line_width;
        style.line_style = line_style;
        style.marker_style = marker_style;
        style.marker_size = marker_size;
        return style;
    }

    std::vector<double> Linspace(double start, double end, int num) {
        std::vector<double> result(num);
        double step = (end - start) / (num - 1);

        for (int i = 0; i < num; i++) {
            result[i] = start + i * step;
        }

        return result;
    }
}
