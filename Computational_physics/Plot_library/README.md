# Plot_library - библиотека для создания 2D-графиков в C++

**Plot_library** — это библиотека на C++ для создания двухмерных графиков (линии, точки, столбцы, гистограммы) с выводом в формат SVG. Она позволяет настраивать стили, заголовки, подписи осей и легенды. Библиотека предназначена для визуализации данных в научных, образовательных или личных проектах.

## Полный функционал

Библиотека находится в пространстве имён `PlotLibrary`. Ниже описаны все доступные классы, структуры, перечисления, методы и функции с их аргументами и значениями по умолчанию.

## Перечисления

### LineStyle

Стили линий для графиков:

- **SOLID** — сплошная линия (по умолчанию).
- **DASHED** — пунктирная линия (5,5).
- **DOTTED** — точечная линия (2,2).
- **DASHDOT** — линия с чередованием тире и точек (5,2,2,2).
- **NONE** — без линии.

### MarkerStyle

Стили маркеров для точечных графиков:

- **NONE** — без маркера (по умолчанию).
- **CIRCLE** — круг.
- **SQUARE** — квадрат.
- **DIAMOND** — ромб.
- **TRIANGLE_UP** — треугольник вверх.
- **TRIANGLE_DOWN** — треугольник вниз.
- **PLUS** — плюс.
- **CROSS** — крест.
- **STAR** — звезда.

### PlotType

Типы графиков (задаются автоматически методами):

- **LINE** — линия.
- **SCATTER** — точки.
- **BAR** — столбцы.

## Структуры

### Color

Представляет цвет в формате RGB с альфа-каналом (прозрачностью).

#### Поля:

- **r** (`double`): Красный канал (0.0–1.0).
- **g** (`double`): Зелёный канал (0.0–1.0).
- **b** (`double`): Синий канал (0.0–1.0).
- **a** (`double`): Прозрачность (0.0–1.0, по умолчанию 1.0).

#### Методы:

- **Color(double red = 0.0, double green = 0.0, double blue = 0.0, double alpha = 1.0)**: Конструктор.
- **std::string ToHex() const**: Возвращает цвет в HEX-формате (например, `#FF0000`).
- **std::string ToRgb() const**: Возвращает цвет в RGB-формате (например, `rgb(255,0,0)`).

### Colors

Статический класс с предопределёнными цветами:

- **RED** (1.0, 0.0, 0.0).
- **BLUE** (0.0, 0.0, 1.0, по умолчанию для линий и маркеров).
- **GREEN** (0.0, 0.5, 0.0).
- **BLACK** (0.0, 0.0, 0.0, по умолчанию для краёв маркеров).
- **WHITE** (1.0, 1.0, 1.0, по умолчанию для фона).
- **YELLOW** (1.0, 1.0, 0.0).
- **CYAN** (0.0, 1.0, 1.0).
- **MAGENTA** (1.0, 0.0, 1.0).
- **ORANGE** (1.0, 0.65, 0.0).
- **PURPLE** (0.5, 0.0, 0.5).
- **PINK** (1.0, 0.75, 0.8).
- **BROWN** (0.65, 0.16, 0.16).
- **GRAY** (0.5, 0.5, 0.5, по умолчанию для сетки).
- **LIGHT_BLUE** (0.68, 0.85, 0.9).
- **LIGHT_GREEN** (0.56, 0.93, 0.56).
- **LIGHT_RED** (1.0, 0.71, 0.71).

### PlotStyle

Настройки стиля для графиков.

#### Поля:

- **color** (`Color`): Цвет линии или заливки (по умолчанию `Colors::BLUE`).
- **line_width** (`double`): Толщина линии (по умолчанию 1.0).
- **line_style** (`LineStyle`): Стиль линии (по умолчанию `SOLID`).
- **marker_style** (`MarkerStyle`): Стиль маркера (по умолчанию `NONE`).
- **marker_size** (`double`): Размер маркера (по умолчанию 6.0).
- **marker_edge_color** (`Color`): Цвет края маркера (по умолчанию `Colors::BLACK`).
- **marker_face_color** (`Color`): Цвет заливки маркера (по умолчанию `Colors::BLUE`).
- **marker_edge_width** (`double`): Толщина края маркера (по умолчанию 1.0).
- **alpha** (`double`): Прозрачность (0.0–1.0, по умолчанию 1.0).
- **fill** (`bool`): Включить заливку (по умолчанию `false`, для `Bar` автоматически `true`).
- **fill_color** (`Color`): Цвет заливки (по умолчанию `Colors::BLUE`).
- **plot_type** (`PlotType`): Тип графика (задаётся автоматически).

#### Конструктор:

- **PlotStyle()**: Создаёт стиль с значениями по умолчанию.

### AxisLimits

Настройки пределов осей.

#### Поля:

- **x_min**, **x_max** (`double`): Пределы по оси X (по умолчанию 0 и 1).
- **y_min**, **y_max** (`double`): Пределы по оси Y (по умолчанию 0 и 1).
- **auto_x**, **auto_y** (`bool`): Автоматическое вычисление пределов (по умолчанию `true`).

#### Конструктор:

- **AxisLimits()**: Создаёт с значениями по умолчанию.

### TextProperties

Настройки текста (для заголовков и подписей).

#### Поля:

- **font_family** (`std::string`): Шрифт (по умолчанию `"Arial"`).
- **font_size** (`int`): Размер шрифта (по умолчанию 12, для заголовка 16, для подписей осей 14).
- **color** (`Color`): Цвет текста (по умолчанию `Colors::BLACK`).
- **horizontal_alignment** (`std::string`): Горизонтальное выравнивание (по умолчанию `"center"`).
- **vertical_alignment** (`std::string`): Вертикальное выравнивание (по умолчанию `"center"`).
- **rotation** (`double`): Угол поворота текста в градусах (по умолчанию 0).
- **bold** (`bool`): Жирный шрифт (по умолчанию `false`, для заголовка `true`).
- **italic** (`bool`): Курсив (по умолчанию `false`).

#### Конструктор:

- **TextProperties()**: Создаёт с значениями по умолчанию.

### LegendProperties

Настройки легенды.

#### Поля:

- **location** (`std::string`): Расположение легенды (по умолчанию `"upper right"`, возможные значения: `"upper left"`, `"lower right"`, и т.д.).
- **columns** (`int`): Количество столбцов (по умолчанию 1).
- **frame_visible** (`bool`): Показывать рамку (по умолчанию `true`).
- **frame_color** (`Color`): Цвет рамки (по умолчанию `Colors::WHITE`).
- **frame_alpha** (`double`): Прозрачность рамки (по умолчанию 0.8).

#### Конструктор:

- **LegendProperties()**: Создаёт с значениями по умолчанию.

## Класс Figure

Основной класс для создания и настройки графиков.

### Конструктор

- **Figure(double width, double height, double dpi_value = 100.0)**:
  - `width`: Ширина графика в пикселях.
  - `height`: Высота графика в пикселях.
  - `dpi_value`: Разрешение (по умолчанию 100.0).

По умолчанию: Фон — `Colors::WHITE`, оси — `Colors::BLACK`, сетка — `Colors::GRAY` (прозрачность 0.3).

### Методы

#### Добавление графиков:

- **void Plot(const std::vector<double>& x, const std::vector<double>& y, const PlotStyle& style)**: Рисует линию, соединяющую точки `(x, y)`.
- **void Scatter(const std::vector<double>& x, const std::vector<double>& y, const PlotStyle& style)**: Рисует точки. Если `marker_style` — `NONE`, автоматически используется `CIRCLE`.
- **void Bar(const std::vector<double>& x, const std::vector<double>& heights, const PlotStyle& style)**: Рисует столбцы с центрами в `x` и высотами `y`. Значения вне пределов осей обрезаются.
- **void Histogram(const std::vector<double>& data, int bins, const PlotStyle& style)**: Создаёт гистограмму из одномерных данных с заданным числом бинов. Значения вне пределов осей обрезаются.

#### Настройка графика:

- **void SetTitle(const std::string& title_text, const TextProperties& props = TextProperties())**: Задаёт заголовок графика.
- **void SetXLabel(const std::string& label, const TextProperties& props = TextProperties())**: Задаёт подпись оси X.
- **void SetYLabel(const std::string& label, const TextProperties& props = TextProperties())**: Задаёт подпись оси Y.
- **void SetLegend(const std::vector<std::string>& labels, const LegendProperties& props = LegendProperties())**: Добавляет легенду с метками.
- **void SetXLimit(double min, double max)**: Задаёт пределы по оси X (отключает авто-пределы).
- **void SetYLimit(double min, double max)**: Задаёт пределы по оси Y (отключает авто-пределы).
- **void Grid(bool visible, const std::string& style = "--", Color color = Colors::GRAY, double alpha = 0.3)**: Включает/выключает сетку, задаёт стиль, цвет и прозрачность.
- **Color CreateColor(double r, double g, double b, double a = 1.0)**: Создаёт пользовательский цвет.
- **PlotStyle CreateStyle(Color color, double line_width, LineStyle line_style, MarkerStyle marker_style = MarkerStyle::NONE, double marker_size = 6.0)**: Создаёт стиль для графика.
- **void SetXPrecision(int precision)**: Задаёт точность для подписей оси X (по умолчанию 2).
- **void SetYPrecision(int precision)**: Задаёт точность для подписей оси Y (по умолчанию 2).

#### Сохранение и отображение:

- **void Save(const std::string& filename, const std::string& format = "svg")**: Сохраняет график в файл (поддерживается только SVG).
- **void Show()**: Открывает последний сохранённый SVG-файл в системном просмотрщике (например, браузере).

#### Внутренние методы (обычно не используются напрямую):

- **void CalculateAutoLimits()**: Вычисляет пределы осей автоматически на основе данных.
- **std::string GetLineStyle(LineStyle style) const**: Возвращает SVG-строку для стиля линии.
- **std::string GetMarkerPath(MarkerStyle style) const**: Возвращает SVG-путь для маркера.

## Глобальная функция

- **std::vector<double> Linspace(double start, double end, int num)**:
  - Создаёт вектор из `num` равномерно распределённых значений от `start` до `end`.
  - Пример: `Linspace(0, 10, 5)` вернёт `{0, 2.5, 5, 7.5, 10}`.
