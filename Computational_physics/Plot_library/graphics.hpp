#ifndef GRAPHICS_HPP
#define GRAPHICS_HPP

#include <vector>
#include <string>

namespace PlotLibrary {
    enum class LineStyle {
        SOLID,
        DASHED,
        DOTTED,
        DASHDOT,
        NONE
    };

    enum class MarkerStyle {
        NONE,
        CIRCLE,
        SQUARE,
        DIAMOND,
        TRIANGLE_UP,
        TRIANGLE_DOWN,
        PLUS,
        CROSS,
        STAR
    };

    enum class PlotType {
        LINE,
        SCATTER,
        BAR
    };

    struct Color {
        double r, g, b, a;
        Color(double red = 0.0, double green = 0.0, double blue = 0.0, double alpha = 1.0);
        std::string ToHex() const;
        std::string ToRgb() const;
    };

    struct Colors {
        static const Color RED;
        static const Color BLUE;
        static const Color GREEN;
        static const Color BLACK;
        static const Color WHITE;
        static const Color YELLOW;
        static const Color CYAN;
        static const Color MAGENTA;
        static const Color ORANGE;
        static const Color PURPLE;
        static const Color PINK;
        static const Color BROWN;
        static const Color GRAY;
        static const Color LIGHT_BLUE;
        static const Color LIGHT_GREEN;
        static const Color LIGHT_RED;
    };

    struct PlotStyle {
        Color color;
        double line_width;
        LineStyle line_style;
        MarkerStyle marker_style;
        double marker_size;
        Color marker_edge_color;
        Color marker_face_color;
        double marker_edge_width;
        double alpha;
        bool fill;
        Color fill_color;
        PlotType plot_type;
        PlotStyle();
    };

    struct AxisLimits {
        double x_min, x_max, y_min, y_max;
        bool auto_x, auto_y;
        AxisLimits();
    };

    struct TextProperties {
        std::string font_family;
        int font_size;
        Color color;
        std::string horizontal_alignment;
        std::string vertical_alignment;
        double rotation;
        bool bold;
        bool italic;
        TextProperties();
    };

    struct LegendProperties {
        std::string location;
        int columns;
        bool frame_visible;
        Color frame_color;
        double frame_alpha;
        double frame_width;
        double frame_height;
        bool auto_size;
        LegendProperties();
    };

    class Figure {
    private:
        double figure_width, figure_height, dpi;
        bool grid_visible;
        std::string grid_style;
        Color grid_color;
        double grid_alpha;
        Color background_color;
        Color axis_color;
        std::string title, x_label, y_label;
        TextProperties title_properties;
        TextProperties label_properties;
        std::vector<std::string> legend_labels;
        LegendProperties legend_properties;
        std::vector<std::vector<double>> data_x, data_y;
        std::vector<PlotStyle> styles;
        AxisLimits limits;
        std::string last_saved_filename;
        std::vector<double> x_ticks;
        std::vector<double> y_ticks;
        int x_precision;
        int y_precision;

    public:
        Figure(double width, double height, double dpi_value = 100.0);
        void Plot(const std::vector<double>& x, const std::vector<double>& y, const PlotStyle& style);
        void Scatter(const std::vector<double>& x, const std::vector<double>& y, const PlotStyle& style);
        void Bar(const std::vector<double>& x, const std::vector<double>& y, const PlotStyle& style);
        void Histogram(const std::vector<double>& data, int bins, const PlotStyle& style);
        void CalculateAutoLimits();
        std::string GetLineStyle(LineStyle style) const;
        std::string GetMarkerPath(MarkerStyle style) const;
        void Save(const std::string& filename, const std::string& format = "svg");
        void Show();
        void SetTitle(const std::string& title_text, const TextProperties& props = TextProperties());
        void SetXLabel(const std::string& label, const TextProperties& props = TextProperties());
        void SetYLabel(const std::string& label, const TextProperties& props = TextProperties());
        void SetLegend(const std::vector<std::string>& labels, const LegendProperties& props = LegendProperties());
        void SetXLimit(double min, double max);
        void SetYLimit(double min, double max);
        void Grid(bool visible, const std::string& style = "--", Color color = Colors::GRAY, double alpha = 0.3);
        Color CreateColor(double r, double g, double b, double a = 1.0);
        PlotStyle CreateStyle(Color color, double line_width, LineStyle line_style, MarkerStyle marker_style = MarkerStyle::NONE, double marker_size = 6.0);
        void SetXTicks(const std::vector<double>& ticks);
        void SetYTicks(const std::vector<double>& ticks);
        void SetXRange(double min, double max);
        void SetYRange(double min, double max);
        void SetLegendAutoSize(bool auto_size);
        void SetXPrecision(int precision);
        void SetYPrecision(int precision);
    };

    std::vector<double> Linspace(double start, double end, int num);
}

#endif
