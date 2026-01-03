import numpy as np
import matplotlib.pyplot as plt
from matplotlib.backends.backend_tkagg import FigureCanvasTkAgg, NavigationToolbar2Tk
import tkinter as tk
from tkinter import ttk, messagebox, filedialog
from PIL import Image, ImageTk

import matplotlib
matplotlib.use('TkAgg')

import warnings
warnings.filterwarnings("ignore", category=RuntimeWarning)


class MDPCapacitanceApplication:
    """Главный класс приложения для анализа характеристик МДП-структур (MOS-структур)"""
    def __init__(self, root):
        """Инициализация приложения: настройка окна, констант, базы материалов и переменных интерфейса"""
        self.root = root
        self.root.title("Анализ МДП структур")

        # Запоминаем нормальный размер
        self.normal_size = "1600x1000"

        # Получаем размеры экрана
        screen_width = self.root.winfo_screenwidth()
        screen_height = self.root.winfo_screenheight()

        # Устанавливаем окно на весь экран
        self.root.geometry(f"{screen_width}x{screen_height}+0+0")
        self.root.state('zoomed')  # Максимизируем окно

        # Добавляем обработчик события изменения состояния окна
        self.root.bind("<Configure>", self.on_window_configure)

        # Попытка установить иконку приложения
        try:
            self.root.iconphoto(False, ImageTk.PhotoImage(Image.open("logo.png")))
        except Exception:
            pass

        # Обработка закрытия окна – подавление лишних сообщений в консоль
        self.root.protocol("WM_DELETE_WINDOW", self.on_closing)

        # Физические константы
        self.electron_charge = 1.6e-19
        self.vacuum_permittivity = 8.85e-12
        self.boltzmann_constant = 1.38e-23
        self.electron_mass = 9.1093837e-31
        self.planck_constant = 6.62607015e-34

        # База данных полупроводниковых материалов
        self.semiconductor_data = {
            "Si": {
                "display": {"Русский": "Кремний (Si)", "English": "Silicon (Si)"},
                "bandgap": 1.12,
                "permittivity": 11.7,
                "effective_mass_holes": 0.81,
                "effective_mass_electrons": 1.18,
                "ionization_level": 0.045,
                "doping_mantissa": 1.0,
                "doping_exponent": 16,
                "temperature": 300
            },
            "Ge": {
                "display": {"Русский": "Германий (Ge)", "English": "Germanium (Ge)"},
                "bandgap": 0.66,
                "permittivity": 16.0,
                "effective_mass_holes": 0.34,
                "effective_mass_electrons": 0.55,
                "ionization_level": 0.012,
                "doping_mantissa": 1.0,
                "doping_exponent": 16,
                "temperature": 300
            },
            "GaAs": {
                "display": {"Русский": "Арсенид галлия (GaAs)", "English": "Gallium arsenide (GaAs)"},
                "bandgap": 1.42,
                "permittivity": 12.9,
                "effective_mass_holes": 0.51,
                "effective_mass_electrons": 0.067,
                "ionization_level": 0.006,
                "doping_mantissa": 1.0,
                "doping_exponent": 16,
                "temperature": 300
            }
        }

        # База данных диэлектриков
        self.dielectric_data = {
            "SiO2": {
                "display": {"Русский": "Диоксид кремния (SiO₂)", "English": "Silicon dioxide (SiO₂)"},
                "permittivity": 3.9
            },
            "Si3N4": {
                "display": {"Русский": "Нитрид кремния (Si₃N₄)", "English": "Silicon nitride (Si₃N₄)"},
                "permittivity": 7.5
            },
            "HfO2": {
                "display": {"Русский": "Оксид гафния (HfO₂)", "English": "Hafnium oxide (HfO₂)"},
                "permittivity": 25.0
            },
            "Al2O3": {
                "display": {"Русский": "Оксид алюминия (Al₂O₃)", "English": "Aluminum oxide (Al₂O₃)"},
                "permittivity": 9.0
            }
        }

        # Переменные для параметров интерфейса
        self.semiconductor_display = tk.StringVar(value=self.semiconductor_data["Si"]["display"]["Русский"])
        self.semiconductor_type_display = tk.StringVar(value="n-типа")
        self.dielectric_display = tk.StringVar(value=self.dielectric_data["SiO2"]["display"]["Русский"])
        self.bandgap_variable = tk.DoubleVar(value=1.12)
        self.permittivity_variable = tk.DoubleVar(value=11.7)
        self.effective_mass_holes_variable = tk.DoubleVar(value=0.81)
        self.effective_mass_electrons_variable = tk.DoubleVar(value=1.18)
        self.ionization_level_variable = tk.DoubleVar(value=0.045)
        self.doping_mantissa_variable = tk.DoubleVar(value=1.0)
        self.doping_exponent_variable = tk.IntVar(value=16)
        self.temperature_variable = tk.DoubleVar(value=300)
        self.thickness_variable = tk.DoubleVar(value=10.0)
        self.area_variable = tk.DoubleVar(value=1.0)
        self.dielectric_permittivity_variable = tk.DoubleVar(value=3.9)
        self.maximum_voltage_variable = tk.DoubleVar(value=5.0)
        self.voltage_step_variable = tk.DoubleVar(value=0.01)

        # Настройки масштабирования интерфейса
        self.scale_levels = [80, 84, 88, 92, 96, 100, 104, 108, 112, 116, 120]
        self.current_scale_index = self.scale_levels.index(100)
        self.scale_factor = 1.0

        # Текущий язык интерфейса
        self.current_language = "Русский"
        self.translations = self.get_translations()

        # Стиль
        self.style = ttk.Style()

        # Создание интерфейса и начальный расчёт
        self.create_widgets()
        self.update_semiconductor_parameters()
        self.update_dielectric_parameters()
        self.update_labels()
        self.calculate()
        self.style.configure('TLabelframe.Label', font=('Arial', int(12 * self.scale_factor)))


    def on_window_configure(self, event=None):
        """Обработчик изменения размера окна"""
        # Если окно больше не максимизировано и мы в полноэкранном режиме
        if hasattr(self, 'was_maximized') and self.was_maximized:
            if self.root.state() != 'zoomed':
                # Выходим из полноэкранного режима
                self.was_maximized = False
                # Устанавливаем нормальный размер
                self.root.geometry(self.normal_size)
                # Центрируем окно
                self.center_window()
        else:
            # Запоминаем, если окно максимизировано
            self.was_maximized = (self.root.state() == 'zoomed')


    def center_window(self):
        """Центрирование окна на экране"""
        self.root.update_idletasks()
        # Устанавливаем конкретный размер
        self.root.geometry(self.normal_size)

        width = 1600
        height = 1000
        screen_width = self.root.winfo_screenwidth()
        screen_height = self.root.winfo_screenheight()

        x = (screen_width - width) // 2
        y = (screen_height - height) // 2

        self.root.geometry(f"{width}x{height}+{x}+{y}")


    def get_translations(self):
        """Словарь переводов для интерфейса"""
        return {
            "Русский": {
                "title": "Анализ МДП структур",
                "control_panel": "Панель управления",
                "management": "Управление",
                "save_data": "Сохранить данные",
                "export_plots": "Экспорт графиков",
                "reset_parameters": "Сбросить параметры",
                "change_language": "Сменить язык",
                "decrease_scale": "−",
                "increase_scale": "+",
                "materials": "Материалы",
                "semiconductor": "Полупроводник:",
                "semiconductor_type": "Тип полупроводника:",
                "dielectric": "Диэлектрик:",
                "custom": "Пользовательский",
                "dielectric_permittivity": "Диэлектрическая проницаемость:",
                "geometric_parameters": "Геометрические параметры",
                "thickness": "Толщина диэлектрика, нм:",
                "area": "Площадь контакта, мм²:",
                "max_voltage": "Максимальное напряжение, В:",
                "voltage_step": "Шаг напряжения, В:",
                "semiconductor_parameters": "Параметры полупроводника",
                "bandgap": "Запрещенная зона, эВ:",
                "permittivity": "Диэлектрическая проницаемость:",
                "effective_mass_holes": "Эффективная масса дырок:",
                "effective_mass_electrons": "Эффективная масса электронов:",
                "ionization_level_n": "Уровень донора, эВ:",
                "ionization_level_p": "Уровень акцептора, эВ:",
                "doping_mantissa_n": "Мантисса концентрации доноров:",
                "doping_mantissa_p": "Мантисса концентрации акцепторов:",
                "doping_exponent": "Степень 10^:",
                "temperature": "Температура, К:",
                "results_modeling": "Результаты моделирования",
                "analysis_parameters": "Анализ параметров структуры",
                "volt_farad": "Вольт-фарадная характеристика",
                "depletion_width": "Ширина области пространственного заряда",
                "surface_potential": "Поверхностный потенциал",
                "electric_field": "Максимальное электрическое поле",
                "voltage": "Напряжение, В",
                "capacitance": "Ёмкость, пФ",
                "width_nm": "Ширина ОПЗ, нм",
                "potential_v": "Поверхностный потенциал, В",
                "field_mv_cm": "Электрическое поле, МВ/см",
                "analysis_title": "РЕЗУЛЬТАТЫ АНАЛИЗА МДП-СТРУКТУРЫ",
                "material": "Материал:",
                "dielectric_label": "Диэлектрик:",
                "main_parameters": "ОСНОВНЫЕ ПАРАМЕТРЫ:",
                "doping_concentration_n": "Концентрация доноров:",
                "doping_concentration_p": "Концентрация акцепторов:",
                "thickness_label": "Толщина диэлектрика:",
                "area_label": "Площадь контакта:",
                "temperature_label": "Температура:",
                "calculated_characteristics": "РАСЧЕТНЫЕ ХАРАКТЕРИСТИКИ:",
                "max_capacitance": "Максимальная ёмкость:",
                "min_capacitance": "Минимальная ёмкость:",
                "ratio_cmin_cmax": "Отношение Cmin/Cmax:",
                "max_depletion_width": "Максимальная ширина ОПЗ:",
                "max_field": "Максимальное поле:",
                "max_potential": "Максимальный потенциал:",
                "success_save": "Данные сохранены в файл:",
                "success_export": "Графики экспортированы в файл:",
                "warning_no_data": "Нет данных для сохранения",
                "error_save": "Не удалось сохранить данные:",
                "error_export": "Не удалось экспортировать графики:"
            },
            "English": {
                "title": "MDS structure analysis",
                "control_panel": "Control panel",
                "management": "Management",
                "save_data": "Save data",
                "export_plots": "Export plots",
                "reset_parameters": "Reset parameters",
                "change_language": "Change language",
                "decrease_scale": "−",
                "increase_scale": "+",
                "materials": "Materials",
                "semiconductor": "Semiconductor:",
                "semiconductor_type": "Semiconductor type:",
                "dielectric": "Dielectric:",
                "custom": "Custom",
                "dielectric_permittivity": "Dielectric permittivity:",
                "geometric_parameters": "Geometric parameters",
                "thickness": "Dielectric thickness, nm:",
                "area": "Contact area, mm²:",
                "max_voltage": "Maximum voltage, V:",
                "voltage_step": "Voltage step, V:",
                "semiconductor_parameters": "Semiconductor parameters",
                "bandgap": "Bandgap, eV:",
                "permittivity": "Permittivity:",
                "effective_mass_holes": "Hole effective mass:",
                "effective_mass_electrons": "Electron effective mass:",
                "ionization_level_n": "Donor level, eV:",
                "ionization_level_p": "Acceptor level, eV:",
                "doping_mantissa_n": "Donor concentration mantissa:",
                "doping_mantissa_p": "Acceptor concentration mantissa:",
                "doping_exponent": "Exponent 10^:",
                "temperature": "Temperature, K:",
                "results_modeling": "Modeling results",
                "analysis_parameters": "Structure parameters analysis",
                "volt_farad": "Volt-farad characteristic",
                "depletion_width": "Depletion region width",
                "surface_potential": "Surface potential",
                "electric_field": "Maximum electric field",
                "voltage": "Voltage, V",
                "capacitance": "Capacitance, pF",
                "width_nm": "Depletion width, nm",
                "potential_v": "Surface potential, V",
                "field_mv_cm": "Electric field, MV/cm",
                "analysis_title": "MOS STRUCTURE ANALYSIS RESULTS",
                "material": "Material:",
                "dielectric_label": "Dielectric:",
                "main_parameters": "MAIN PARAMETERS:",
                "doping_concentration_n": "Donor concentration:",
                "doping_concentration_p": "Acceptor concentration:",
                "thickness_label": "Dielectric thickness:",
                "area_label": "Contact area:",
                "temperature_label": "Temperature:",
                "calculated_characteristics": "CALCULATED CHARACTERISTICS:",
                "max_capacitance": "Maximum capacitance:",
                "min_capacitance": "Minimum capacitance:",
                "ratio_cmin_cmax": "Cmin/Cmax ratio:",
                "max_depletion_width": "Maximum depletion width:",
                "max_field": "Maximum field:",
                "max_potential": "Maximum potential:",
                "success_save": "Data saved to file:",
                "success_export": "Plots exported to file:",
                "warning_no_data": "No data to save",
                "error_save": "Failed to save data:",
                "error_export": "Failed to export plots:"
            }
        }


    def on_closing(self):
        """Корректное закрытие приложения"""
        self.root.quit()
        self.root.destroy()


    def create_widgets(self):
        """Создание всех элементов интерфейса"""
        self.root.title(self.translations[self.current_language]["title"])
        self.main_frame = ttk.Frame(self.root, padding="10")
        self.main_frame.pack(fill=tk.BOTH, expand=True)

        # Панель управления - 4 колонки
        self.control_frame = ttk.LabelFrame(self.main_frame, text=self.translations[self.current_language]["control_panel"], padding="10")
        self.control_frame.pack(fill=tk.X, padx=5, pady=5)
        self.control_frame.columnconfigure(0, weight=1, uniform="cols")
        self.control_frame.columnconfigure(1, weight=1, uniform="cols")
        self.control_frame.columnconfigure(2, weight=1, uniform="cols")
        self.control_frame.columnconfigure(3, weight=1, uniform="cols")

        self.create_buttons_column(self.control_frame)
        self.create_materials_column(self.control_frame)
        self.create_geometry_column(self.control_frame)
        self.create_semiconductor_column(self.control_frame)

        # Область графиков
        self.plot_frame = ttk.LabelFrame(self.main_frame, text=self.translations[self.current_language]["results_modeling"], padding="10")
        self.plot_frame.pack(fill=tk.BOTH, expand=True, padx=5, pady=5)

        # Панель анализа
        self.analysis_frame = ttk.LabelFrame(self.main_frame, text=self.translations[self.current_language]["analysis_parameters"], padding="10")
        self.analysis_frame.pack(fill=tk.X, padx=5, pady=5)

        self.create_plot_area(self.plot_frame)
        self.create_analysis_panel(self.analysis_frame)


    def create_buttons_column(self, parent):
        """Колонка с кнопками управления"""
        self.buttons_frame = ttk.LabelFrame(parent, text=self.translations[self.current_language]["management"], padding="10")
        self.buttons_frame.grid(row=0, column=0, sticky="nsew", padx=(0, 5))

        container = ttk.Frame(self.buttons_frame)
        container.pack(expand=True, fill=tk.BOTH)
        center_frame = ttk.Frame(container)
        center_frame.pack(expand=True)

        button_width = int(28 * self.scale_factor)

        self.save_button = ttk.Button(center_frame, text=self.translations[self.current_language]["save_data"], command=self.save_data, width=button_width)
        self.save_button.pack(pady=5)

        self.export_button = ttk.Button(center_frame, text=self.translations[self.current_language]["export_plots"], command=self.export_plots, width=button_width)
        self.export_button.pack(pady=5)

        self.reset_button = ttk.Button(center_frame, text=self.translations[self.current_language]["reset_parameters"], command=self.reset_parameters, width=button_width)
        self.reset_button.pack(pady=5)

        self.language_button = ttk.Button(center_frame, text=self.translations[self.current_language]["change_language"], command=self.toggle_language, width=button_width)
        self.language_button.pack(pady=5)

        # Кнопки масштаба
        scale_frame = ttk.Frame(center_frame)
        scale_frame.pack(pady=5)
        ttk.Button(scale_frame, text=self.translations[self.current_language]["decrease_scale"], command=self.decrease_scale, width=4).pack(side=tk.LEFT, padx=5)
        self.scale_label = ttk.Label(scale_frame, text=f"{self.scale_levels[self.current_scale_index]}%", font=('Arial', int(12 * self.scale_factor)))
        self.scale_label.pack(side=tk.LEFT)
        ttk.Button(scale_frame, text=self.translations[self.current_language]["increase_scale"], command=self.increase_scale, width=4).pack(side=tk.LEFT, padx=5)


    def decrease_scale(self):
        """Уменьшение масштаба интерфейса"""
        if self.current_scale_index > 0:
            self.current_scale_index -= 1
            self.scale_factor = self.scale_levels[self.current_scale_index] / 100
            self.update_font_sizes()


    def increase_scale(self):
        """Увеличение масштаба интерфейса"""
        if self.current_scale_index < len(self.scale_levels) - 1:
            self.current_scale_index += 1
            self.scale_factor = self.scale_levels[self.current_scale_index] / 100
            self.update_font_sizes()


    def toggle_language(self):
        """Переключение языка интерфейса"""
        # Меняем язык
        self.current_language = "English" if self.current_language == "Русский" else "Русский"

        # Обновляем все текстовые элементы интерфейса
        self.update_all_interface_texts()

        # Обновляем подписи и пересчитываем
        self.update_labels()
        self.calculate()


    def update_all_interface_texts(self):
        """Обновление всех текстовых элементов интерфейса при смене языка"""
        # Обновляем заголовок окна
        self.root.title(self.translations[self.current_language]["title"])

        # Обновляем заголовки всех основных фреймов
        self.update_frame_titles()

        # Обновляем все подписи в интерфейсе
        self.update_all_labels()

        # Обновляем подписи графиков
        self.update_plot_labels()

        # Обновляем выпадающие списки материалов
        self.update_material_comboboxes()

        # Обновляем кнопки
        self.update_buttons()


    def update_frame_titles(self):
        """Обновление заголовков всех фреймов"""
        language = self.translations[self.current_language]

        # Обновляем заголовок панели управления
        self.control_frame.config(text=language["control_panel"])

        # Обновляем заголовок блока управления
        self.buttons_frame.config(text=language["management"])

        # Обновляем заголовки всех внутренних фреймов
        for widget in self.control_frame.winfo_children():
            if isinstance(widget, ttk.LabelFrame):
                current_text = widget.cget('text')
                if current_text in ["Материалы", "Materials"]:
                    widget.config(text=language["materials"])
                elif current_text in ["Геометрические параметры", "Geometric parameters"]:
                    widget.config(text=language["geometric_parameters"])
                elif current_text in ["Параметры полупроводника", "Semiconductor parameters"]:
                    widget.config(text=language["semiconductor_parameters"])

        # Обновляем заголовки других фреймов
        self.plot_frame.config(text=language["results_modeling"])
        self.analysis_frame.config(text=language["analysis_parameters"])


    def update_buttons(self):
        """Обновление текста кнопок"""
        language = self.translations[self.current_language]

        if hasattr(self, 'save_button'):
            self.save_button.config(text=language["save_data"])

        if hasattr(self, 'export_button'):
            self.export_button.config(text=language["export_plots"])

        if hasattr(self, 'reset_button'):
            self.reset_button.config(text=language["reset_parameters"])

        if hasattr(self, 'language_button'):
            self.language_button.config(text=language["change_language"])


    def update_all_labels(self):
        """Обновление всех текстовых подписей в интерфейсе"""
        language = self.translations[self.current_language]

        # Обновляем подписи в блоке материалов
        if hasattr(self, 'materials_labels'):
            for label_widget in self.materials_labels:
                current_text = label_widget.cget('text')
                if current_text in ["Полупроводник:", "Semiconductor:"]:
                    label_widget.config(text=language["semiconductor"])
                elif current_text in ["Тип полупроводника:", "Semiconductor type:"]:
                    label_widget.config(text=language["semiconductor_type"])
                elif current_text in ["Диэлектрик:", "Dielectric:"]:
                    label_widget.config(text=language["dielectric"])
                elif current_text in ["Диэлектрическая проницаемость:", "Dielectric permittivity:"]:
                    label_widget.config(text=language["dielectric_permittivity"])

        # Обновляем подписи в блоке геометрических параметров
        if hasattr(self, 'geometry_labels'):
            for label_widget in self.geometry_labels:
                current_text = label_widget.cget('text')
                if current_text in ["Толщина диэлектрика, нм:", "Dielectric thickness, nm:"]:
                    label_widget.config(text=language["thickness"])
                elif current_text in ["Площадь контакта, мм²:", "Contact area, mm²:"]:
                    label_widget.config(text=language["area"])
                elif current_text in ["Максимальное напряжение, В:", "Maximum voltage, V:"]:
                    label_widget.config(text=language["max_voltage"])
                elif current_text in ["Шаг напряжения, В:", "Voltage step, V:"]:
                    label_widget.config(text=language["voltage_step"])

        # Обновляем подписи в блоке параметров полупроводника
        if hasattr(self, 'semiconductor_labels'):
            for label_widget in self.semiconductor_labels:
                current_text = label_widget.cget('text')
                if current_text in ["Запрещенная зона, эВ:", "Bandgap, eV:"]:
                    label_widget.config(text=language["bandgap"])
                elif current_text in ["Диэлектрическая проницаемость:", "Permittivity:"]:
                    label_widget.config(text=language["permittivity"])
                elif current_text in ["Эффективная масса дырок:", "Hole effective mass:"]:
                    label_widget.config(text=language["effective_mass_holes"])
                elif current_text in ["Эффективная масса электронов:", "Electron effective mass:"]:
                    label_widget.config(text=language["effective_mass_electrons"])
                elif current_text in ["Степень 10^:", "Exponent 10^:"]:
                    label_widget.config(text=language["doping_exponent"])
                elif current_text in ["Температура, К:", "Temperature, K:"]:
                    label_widget.config(text=language["temperature"])
                # Динамические подписи обрабатываются в update_labels()


    def update_material_comboboxes(self):
        """Обновление выпадающих списков материалов при смене языка"""
        try:
            # Обновляем значения в выпадающем списке полупроводников
            semiconductor_values = [data["display"][self.current_language] for data in self.semiconductor_data.values()]
            self.semiconductor_combobox['values'] = semiconductor_values

            # Обновляем текущее значение
            current_key = self.get_semiconductor_key()
            if current_key in self.semiconductor_data:
                self.semiconductor_display.set(self.semiconductor_data[current_key]["display"][self.current_language])

            # Обновляем значения в выпадающем списке типов
            type_values = ["n-type", "p-type"] if self.current_language == "English" else ["n-типа", "p-типа"]
            self.semiconductor_type_combobox['values'] = type_values

            # Обновляем текущее значение типа
            current_type = self.semiconductor_type_display.get()
            if current_type in ["n-типа", "n-type"]:
                self.semiconductor_type_display.set(type_values[0])
            elif current_type in ["p-типа", "p-type"]:
                self.semiconductor_type_display.set(type_values[1])

            # Обновляем значения в выпадающем списке диэлектриков
            dielectric_values = [data["display"][self.current_language] for data in self.dielectric_data.values()] + [self.translations[self.current_language]["custom"]]
            self.dielectric_combobox['values'] = dielectric_values

            # Обновляем текущее значение диэлектрика
            current_key = self.get_dielectric_key()
            if current_key is None:
                self.dielectric_display.set(self.translations[self.current_language]["custom"])
            elif current_key in self.dielectric_data:
                self.dielectric_display.set(self.dielectric_data[current_key]["display"][self.current_language])

        except Exception as exception:
            print(f"Ошибка при обновлении списков материалов: {exception}")


    def update_font_sizes(self):
        """Обновление размеров шрифтов в зависимости от масштаба"""
        base_font_size = int(10 * self.scale_factor)
        large_font_size = int(12 * self.scale_factor)

        plt.rcParams.update({'font.size': base_font_size})
        self.style.configure('TLabelframe.Label', font=('Arial', large_font_size))
        self.style.configure('TLabel', font=('Arial', base_font_size))
        self.style.configure('TButton', font=('Arial', base_font_size))
        self.style.configure('TCombobox', font=('Arial', base_font_size))
        self.style.configure('TSpinbox', font=('Arial', base_font_size))

        # Обновляем метку масштаба
        if hasattr(self, 'scale_label'):
            self.scale_label.config(text=f"{self.scale_levels[self.current_scale_index]}%", font=('Arial', large_font_size))

        # Обновляем шрифт в текстовом поле анализа
        if hasattr(self, 'analysis_text'):
            self.analysis_text.config(font=('Consolas', base_font_size))

        # Обновляем подписи графиков
        self.update_plot_labels()


    def get_semiconductor_key(self):
        """Получить внутренний ключ полупроводника по отображаемому имени"""
        display_name = self.semiconductor_display.get()

        for key, data in self.semiconductor_data.items():
            if data["display"][self.current_language] == display_name:
                return key

        return "Si"


    def get_dielectric_key(self):
        """Получить внутренний ключ диэлектрика по отображаемому имени"""
        display_name = self.dielectric_display.get()

        if display_name == self.translations[self.current_language]["custom"]:
            return None

        for key, data in self.dielectric_data.items():
            if data["display"][self.current_language] == display_name:
                return key

        return "SiO2"


    def get_semiconductor_type(self):
        """Получить тип полупроводника ('n' или 'p')"""
        type_display = self.semiconductor_type_display.get().lower()

        if 'n' in type_display:
            return 'n'
        else:
            return 'p'


    def create_materials_column(self, parent):
        """Колонка выбора материалов (полупроводник и диэлектрик)"""
        self.materials_frame = ttk.LabelFrame(parent, text=self.translations[self.current_language]["materials"], padding="10")
        self.materials_frame.grid(row=0, column=1, sticky="nsew", padx=(0, 5))

        container = ttk.Frame(self.materials_frame)
        container.pack(expand=True, fill=tk.BOTH)
        center_frame = ttk.Frame(container)
        center_frame.pack(expand=True)

        width_maximum = 25
        label_width = 32

        # Список для хранения меток материалов
        self.materials_labels = []

        # Выбор полупроводника
        semiconductor_frame = ttk.Frame(center_frame)
        semiconductor_frame.pack(pady=5, fill=tk.X)
        semiconductor_label = ttk.Label(semiconductor_frame, text=self.translations[self.current_language]["semiconductor"], width=label_width, anchor=tk.W)
        semiconductor_label.pack(side=tk.LEFT, padx=(0, 5))
        self.materials_labels.append(semiconductor_label)

        semiconductor_values = [data["display"][self.current_language] for data in self.semiconductor_data.values()]
        self.semiconductor_combobox = ttk.Combobox(semiconductor_frame, textvariable=self.semiconductor_display, values=semiconductor_values, state="readonly", width=width_maximum)
        self.semiconductor_combobox.pack(side=tk.RIGHT)
        self.semiconductor_combobox.bind('<<ComboboxSelected>>', self.on_semiconductor_change)

        # Выбор типа полупроводника
        type_frame = ttk.Frame(center_frame)
        type_frame.pack(pady=5, fill=tk.X)
        type_label = ttk.Label(type_frame, text=self.translations[self.current_language]["semiconductor_type"], width=label_width, anchor=tk.W)
        type_label.pack(side=tk.LEFT, padx=(0, 5))
        self.materials_labels.append(type_label)

        type_values = ["n-type", "p-type"] if self.current_language == "English" else ["n-типа", "p-типа"]
        self.semiconductor_type_combobox = ttk.Combobox(type_frame, textvariable=self.semiconductor_type_display, values=type_values, state="readonly", width=width_maximum)
        self.semiconductor_type_combobox.pack(side=tk.RIGHT)
        self.semiconductor_type_combobox.bind('<<ComboboxSelected>>', self.on_semiconductor_type_change)

        # Выбор диэлектрика
        dielectric_frame = ttk.Frame(center_frame)
        dielectric_frame.pack(pady=5, fill=tk.X)
        dielectric_label = ttk.Label(dielectric_frame, text=self.translations[self.current_language]["dielectric"], width=label_width, anchor=tk.W)
        dielectric_label.pack(side=tk.LEFT, padx=(0, 5))
        self.materials_labels.append(dielectric_label)

        dielectric_values = [data["display"][self.current_language] for data in self.dielectric_data.values()] + [self.translations[self.current_language]["custom"]]
        self.dielectric_combobox = ttk.Combobox(dielectric_frame, textvariable=self.dielectric_display, values=dielectric_values, state="readonly", width=width_maximum)
        self.dielectric_combobox.pack(side=tk.RIGHT)
        self.dielectric_combobox.bind('<<ComboboxSelected>>', self.on_dielectric_change)

        # Пользовательская диэлектрическая проницаемость
        custom_dielectric_frame = ttk.Frame(center_frame)
        custom_dielectric_frame.pack(pady=5, fill=tk.X)
        permittivity_label = ttk.Label(custom_dielectric_frame, text=self.translations[self.current_language]["dielectric_permittivity"], width=label_width, anchor=tk.W)
        permittivity_label.pack(side=tk.LEFT, padx=(0, 5))
        self.materials_labels.append(permittivity_label)

        self.dielectric_permittivity_spinbox = ttk.Spinbox(custom_dielectric_frame, textvariable=self.dielectric_permittivity_variable, from_=1.0, to=100.0, increment=0.1, width=width_maximum, state="disabled")
        self.dielectric_permittivity_spinbox.pack(side=tk.RIGHT)
        self.dielectric_permittivity_spinbox.bind('<KeyRelease>', lambda event: self.calculate())


    def create_geometry_column(self, parent):
        """Колонка геометрических параметров структуры"""
        self.geometry_frame = ttk.LabelFrame(parent, text=self.translations[self.current_language]["geometric_parameters"], padding="10")
        self.geometry_frame.grid(row=0, column=2, sticky="nsew", padx=(0, 5))

        container = ttk.Frame(self.geometry_frame)
        container.pack(expand=True, fill=tk.BOTH)
        center_frame = ttk.Frame(container)
        center_frame.pack(expand=True)

        label_width = 30

        # Список для хранения меток геометрических параметров
        self.geometry_labels = []

        self.create_parameter_input(center_frame, self.translations[self.current_language]["thickness"], self.thickness_variable, 0.1, 1000.0, 1.0, label_width, self.geometry_labels)
        self.create_parameter_input(center_frame, self.translations[self.current_language]["area"], self.area_variable, 0.001, 1000.0, 0.1, label_width, self.geometry_labels)
        self.create_parameter_input(center_frame, self.translations[self.current_language]["max_voltage"], self.maximum_voltage_variable, 0.01, 100.0, 0.5, label_width, self.geometry_labels)
        self.create_parameter_input(center_frame, self.translations[self.current_language]["voltage_step"], self.voltage_step_variable, 0.0001, 1.0, 0.001, label_width, self.geometry_labels)


    def create_semiconductor_column(self, parent):
        """Колонка параметров полупроводника"""
        self.semiconductor_params_frame = ttk.LabelFrame(parent, text=self.translations[self.current_language]["semiconductor_parameters"], padding="10")
        self.semiconductor_params_frame.grid(row=0, column=3, sticky="nsew")

        container = ttk.Frame(self.semiconductor_params_frame)
        container.pack(expand=True, fill=tk.BOTH)
        center_frame = ttk.Frame(container)
        center_frame.pack(expand=True)

        label_width = 35

        # Список для хранения меток параметров полупроводника
        self.semiconductor_labels = []

        self.create_parameter_input(center_frame, self.translations[self.current_language]["bandgap"], self.bandgap_variable, 0.01, 10.0, 0.1, label_width, self.semiconductor_labels)
        self.create_parameter_input(center_frame, self.translations[self.current_language]["permittivity"], self.permittivity_variable, 1.0, 100.0, 1.0, label_width, self.semiconductor_labels)
        self.create_parameter_input(center_frame, self.translations[self.current_language]["effective_mass_holes"], self.effective_mass_holes_variable, 0.01, 10.0, 0.1, label_width, self.semiconductor_labels)
        self.create_parameter_input(center_frame, self.translations[self.current_language]["effective_mass_electrons"], self.effective_mass_electrons_variable, 0.01, 10.0, 0.1, label_width, self.semiconductor_labels)

        # Уровень ионизации с динамической подписью
        ionization_level_frame = ttk.Frame(center_frame)
        ionization_level_frame.pack(fill=tk.X, pady=3)
        self.ionization_level_label = ttk.Label(ionization_level_frame, width=label_width, anchor=tk.W)
        self.ionization_level_label.pack(side=tk.LEFT, padx=(0, 5))
        self.semiconductor_labels.append(self.ionization_level_label)
        self.create_parameter_spinbox(ionization_level_frame, self.ionization_level_variable, 0.0001, 1.0, 0.01)

        # Мантисса концентрации легирования с динамической подписью
        doping_mantissa_frame = ttk.Frame(center_frame)
        doping_mantissa_frame.pack(fill=tk.X, pady=3)
        self.doping_mantissa_label = ttk.Label(doping_mantissa_frame, width=label_width, anchor=tk.W)
        self.doping_mantissa_label.pack(side=tk.LEFT, padx=(0, 5))
        self.semiconductor_labels.append(self.doping_mantissa_label)
        self.create_parameter_spinbox(doping_mantissa_frame, self.doping_mantissa_variable, 0.1, 9.99, 0.1)

        # Степень концентрации легирования
        doping_exponent_frame = ttk.Frame(center_frame)
        doping_exponent_frame.pack(fill=tk.X, pady=3)
        exponent_label = ttk.Label(doping_exponent_frame, text=self.translations[self.current_language]["doping_exponent"], width=label_width, anchor=tk.W)
        exponent_label.pack(side=tk.LEFT, padx=(0, 5))
        self.semiconductor_labels.append(exponent_label)
        self.create_parameter_spinbox(doping_exponent_frame, self.doping_exponent_variable, 10, 25, 1)

        self.create_parameter_input(center_frame, self.translations[self.current_language]["temperature"], self.temperature_variable, 1.0, 1500.0, 10.0, label_width, self.semiconductor_labels)


    def create_parameter_input(self, parent, label, variable, minimum_value, maximum_value, increment, label_width, labels_list=None):
        """Создание элемента ввода с подписью слева и валидацией"""
        frame = ttk.Frame(parent)
        frame.pack(fill=tk.X, pady=3)
        label_widget = ttk.Label(frame, text=label, width=label_width, anchor=tk.W)
        label_widget.pack(side=tk.LEFT, padx=(0, 5))

        if labels_list is not None:
            labels_list.append(label_widget)

        self.create_parameter_spinbox(frame, variable, minimum_value, maximum_value, increment)


    def create_parameter_spinbox(self, parent, variable, minimum_value, maximum_value, increment):
        """Создание спинбокса с валидацией"""
        def validate_input(new_value):
            """Валидация вводимых данных"""
            if new_value == "":
                return True

            try:
                value = float(new_value) if isinstance(variable, tk.DoubleVar) else int(new_value)

                if minimum_value <= value <= maximum_value:
                    return True

                return False
            except Exception:
                return False

        validate_command = (self.root.register(validate_input), '%P')
        spinbox = ttk.Spinbox(parent, textvariable=variable, from_=minimum_value, to=maximum_value, increment=increment, width=10, command=self.calculate, validate="key", validatecommand=validate_command)
        spinbox.pack(side=tk.RIGHT)
        spinbox.bind('<KeyRelease>', lambda event: self.calculate())


    def create_plot_area(self, parent):
        """Создание области для четырёх графиков"""
        self.figure, ((self.axes1, self.axes2), (self.axes3, self.axes4)) = plt.subplots(2, 2, figsize=(14, 8))
        self.canvas = FigureCanvasTkAgg(self.figure, master=parent)
        self.canvas.get_tk_widget().pack(fill=tk.BOTH, expand=True)

        toolbar = NavigationToolbar2Tk(self.canvas, parent)
        toolbar.update()

        self.setup_plot_axes()
        self.figure.tight_layout(pad=2.0, h_pad=2.0, w_pad=2.0)
        self.canvas.draw()


    def setup_plot_axes(self):
        """Настройка заголовков и подписей осей графиков"""
        language = self.translations[self.current_language]

        self.axes1.set_title(language["volt_farad"])
        self.axes1.set_xlabel(language["voltage"])
        self.axes1.set_ylabel(language["capacitance"])
        self.axes1.grid(True, alpha=0.3)

        self.axes2.set_title(language["depletion_width"])
        self.axes2.set_xlabel(language["voltage"])
        self.axes2.set_ylabel(language["width_nm"])
        self.axes2.grid(True, alpha=0.3)

        self.axes3.set_title(language["surface_potential"])
        self.axes3.set_xlabel(language["voltage"])
        self.axes3.set_ylabel(language["potential_v"])
        self.axes3.grid(True, alpha=0.3)

        self.axes4.set_title(language["electric_field"])
        self.axes4.set_xlabel(language["voltage"])
        self.axes4.set_ylabel(language["field_mv_cm"])
        self.axes4.grid(True, alpha=0.3)
        
    def update_plot_labels(self):
        """Обновление подписей графиков"""
        language = self.translations[self.current_language]

        if hasattr(self, 'axes1'):
            self.axes1.set_title(language["volt_farad"])
            self.axes1.set_xlabel(language["voltage"])
            self.axes1.set_ylabel(language["capacitance"])

        if hasattr(self, 'axes2'):
            self.axes2.set_title(language["depletion_width"])
            self.axes2.set_xlabel(language["voltage"])
            self.axes2.set_ylabel(language["width_nm"])

        if hasattr(self, 'axes3'):
            self.axes3.set_title(language["surface_potential"])
            self.axes3.set_xlabel(language["voltage"])
            self.axes3.set_ylabel(language["potential_v"])

        if hasattr(self, 'axes4'):
            self.axes4.set_title(language["electric_field"])
            self.axes4.set_xlabel(language["voltage"])
            self.axes4.set_ylabel(language["field_mv_cm"])

        if hasattr(self, 'canvas'):
            self.canvas.draw_idle()


    def create_analysis_panel(self, parent):
        """Создание текстовой панели с результатами расчётов"""
        base_font_size = int(9 * self.scale_factor)
        self.analysis_text = tk.Text(parent, height=8, width=100, font=('Consolas', base_font_size))
        scrollbar = ttk.Scrollbar(parent, orient=tk.VERTICAL, command=self.analysis_text.yview)
        self.analysis_text.configure(yscrollcommand=scrollbar.set)

        self.analysis_text.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)
        scrollbar.pack(side=tk.RIGHT, fill=tk.Y)


    def on_semiconductor_change(self, event=None):
        """Обработчик изменения выбранного полупроводника"""
        self.update_semiconductor_parameters()
        self.calculate()


    def on_semiconductor_type_change(self, event=None):
        """Обработчик изменения типа полупроводника"""
        self.update_labels()
        self.calculate()


    def on_dielectric_change(self, event=None):
        """Обработчик изменения выбранного диэлектрика"""
        dielectric_name = self.dielectric_display.get()

        if dielectric_name == self.translations[self.current_language]["custom"]:
            self.dielectric_permittivity_spinbox.config(state="normal")
        else:
            self.dielectric_permittivity_spinbox.config(state="disabled")
            self.update_dielectric_parameters()

        self.calculate()


    def reset_parameters(self):
        """Сброс всех параметров к значениям по умолчанию (Si + SiO₂)"""
        self.semiconductor_display.set(self.semiconductor_data["Si"]["display"][self.current_language])
        type_values = ["n-type", "p-type"] if self.current_language == "English" else ["n-типа", "p-типа"]
        self.semiconductor_type_display.set(type_values[0])
        self.dielectric_display.set(self.dielectric_data["SiO2"]["display"][self.current_language])

        self.update_semiconductor_parameters()
        self.update_dielectric_parameters()

        self.thickness_variable.set(10.0)
        self.area_variable.set(1.0)
        self.maximum_voltage_variable.set(5.0)
        self.voltage_step_variable.set(0.01)

        self.dielectric_permittivity_spinbox.config(state="disabled")

        self.update_labels()
        self.calculate()


    def update_semiconductor_parameters(self):
        """Обновление параметров полупроводника из выбранной записи базы"""
        key = self.get_semiconductor_key()

        if key in self.semiconductor_data:
            data = self.semiconductor_data[key]
            self.bandgap_variable.set(data["bandgap"])
            self.permittivity_variable.set(data["permittivity"])
            self.effective_mass_holes_variable.set(data["effective_mass_holes"])
            self.effective_mass_electrons_variable.set(data["effective_mass_electrons"])
            self.ionization_level_variable.set(data["ionization_level"])
            self.doping_mantissa_variable.set(data["doping_mantissa"])
            self.doping_exponent_variable.set(data["doping_exponent"])
            self.temperature_variable.set(data["temperature"])


    def update_dielectric_parameters(self):
        """Обновление диэлектрической проницаемости из выбранной записи базы"""
        key = self.get_dielectric_key()

        if key in self.dielectric_data:
            data = self.dielectric_data[key]
            self.dielectric_permittivity_variable.set(data["permittivity"])


    def update_labels(self):
        """Обновление динамических подписей в зависимости от типа полупроводника"""
        semiconductor_type = self.get_semiconductor_type()

        if hasattr(self, 'ionization_level_label'):
            self.ionization_level_label['text'] = self.translations[self.current_language][f"ionization_level_{semiconductor_type}"]

        if hasattr(self, 'doping_mantissa_label'):
            self.doping_mantissa_label['text'] = self.translations[self.current_language][f"doping_mantissa_{semiconductor_type}"]


    def calculate_capacitance(self):
        """Основной расчёт характеристик МДП-структуры"""
        try:
            semiconductor_type = self.get_semiconductor_type()
            bandgap = self.bandgap_variable.get()
            epsilon_s = self.permittivity_variable.get()
            doping_mantissa = self.doping_mantissa_variable.get()
            doping_exponent = self.doping_exponent_variable.get()
            temperature = self.temperature_variable.get()
            thickness = self.thickness_variable.get() * 1e-9
            area = self.area_variable.get() * 1e-6
            epsilon_d = self.dielectric_permittivity_variable.get()
            maximum_voltage = self.maximum_voltage_variable.get()
            voltage_step = self.voltage_step_variable.get()

            doping_concentration = doping_mantissa * 10 ** doping_exponent

            epsilon_semiconductor = epsilon_s * self.vacuum_permittivity
            epsilon_dielectric = epsilon_d * self.vacuum_permittivity

            dielectric_capacitance = epsilon_dielectric * area / thickness

            # Расчёт собственной концентрации
            thermal_potential = self.boltzmann_constant * temperature / self.electron_charge
            effective_mass_electrons = self.effective_mass_electrons_variable.get()
            effective_mass_holes = self.effective_mass_holes_variable.get()
            reduced_planck_constant = self.planck_constant / (2 * np.pi)
            density_of_states_prefactor = (2 * np.pi * self.electron_mass * self.boltzmann_constant * temperature / (reduced_planck_constant ** 2)) ** (1.5)
            effective_density_of_states_conduction_band = 2 * (effective_mass_electrons) ** (1.5) * density_of_states_prefactor
            effective_density_of_states_valence_band = 2 * (effective_mass_holes) ** (1.5) * density_of_states_prefactor
            intrinsic_carrier_concentration_m3 = np.sqrt(effective_density_of_states_conduction_band * effective_density_of_states_valence_band) * np.exp(-bandgap / (2 * thermal_potential))
            intrinsic_carrier_concentration = intrinsic_carrier_concentration_m3 / 1e6
            built_in_potential = thermal_potential * np.log(doping_concentration / intrinsic_carrier_concentration)
            threshold_potential = 2 * built_in_potential

            # Диапазон напряжений
            polarity = -1 if semiconductor_type == 'n' else 1
            voltages = np.arange(0, polarity * maximum_voltage + (polarity * voltage_step) / 2, polarity * voltage_step)

            capacitances = []
            surface_potentials = []
            depletion_widths = []
            electric_fields = []

            # Предвычисленный параметр для упрощения формулы поверхностного потенциала
            calculation_parameter = (epsilon_semiconductor / epsilon_dielectric) * thickness * np.sqrt(2 * self.electron_charge * doping_concentration * 1e6 / epsilon_semiconductor)

            for voltage in voltages:
                effective_voltage = polarity * voltage

                if effective_voltage <= 0:
                    capacitances.append(dielectric_capacitance)
                    surface_potentials.append(0)
                    depletion_widths.append(0)
                    electric_fields.append(0)
                else:
                    discriminant = calculation_parameter**2 + 4 * effective_voltage

                    if discriminant < 0:
                        surface_potential = 0
                    else:
                        surface_potential = ((-calculation_parameter + np.sqrt(discriminant)) / 2)**2

                        if surface_potential > threshold_potential:
                            surface_potential = threshold_potential

                    depletion_width = np.sqrt(2 * epsilon_semiconductor * surface_potential / (self.electron_charge * doping_concentration * 1e6))
                    semiconductor_capacitance = epsilon_semiconductor * area / depletion_width if depletion_width > 0 else np.inf
                    total_capacitance = 1 / (1 / dielectric_capacitance + 1 / semiconductor_capacitance) if semiconductor_capacitance != np.inf else dielectric_capacitance
                    electric_field = np.sqrt(2 * self.electron_charge * doping_concentration * 1e6 * surface_potential / epsilon_semiconductor)

                    capacitances.append(total_capacitance)
                    surface_potentials.append(surface_potential)
                    depletion_widths.append(depletion_width)
                    electric_fields.append(electric_field)

            return (np.array(voltages), np.array(capacitances), np.array(surface_potentials), np.array(depletion_widths), np.array(electric_fields), semiconductor_type, doping_concentration)
        except Exception as exception:
            print(f"Ошибка при расчёте: {exception}")
            return None, None, None, None, None, None, None


    def calculate(self, event=None):
        """Автоматический пересчёт и обновление графиков/анализа при изменении любого параметра"""
        try:
            results = self.calculate_capacitance()
            if results[0] is not None:
                self.update_plot(*results[:5], results[5])
                self.update_analysis(*results)
        except Exception as exception:
            print(f"Ошибка при обновлении графиков: {exception}")


    def update_plot(self, voltages, capacitances, surface_potentials, depletion_widths, electric_fields, semiconductor_type):
        """Обновление всех четырёх графиков новыми данными"""
        for axes in [self.axes1, self.axes2, self.axes3, self.axes4]:
            axes.clear()

        polarity = -1 if semiconductor_type == 'n' else 1
        signed_surface_potentials = polarity * surface_potentials

        self.axes1.plot(voltages, capacitances * 1e12, 'b-', linewidth=2)
        self.axes2.plot(voltages, depletion_widths * 1e9, 'r-', linewidth=2)
        self.axes3.plot(voltages, signed_surface_potentials, 'g-', linewidth=2)
        self.axes4.plot(voltages, electric_fields * 1e-8, 'm-', linewidth=2)

        self.setup_plot_axes()
        self.canvas.draw_idle()


    def update_analysis(self, voltages, capacitances, surface_potentials, depletion_widths, electric_fields, semiconductor_type, doping_concentration):
        """Формирование и вывод текстового отчёта с ключевыми параметрами"""
        language = self.translations[self.current_language]
        analysis_text = f"{language['analysis_title']}\n{'=' * 50}\n\n"
        analysis_text += f"{language['material']} {self.semiconductor_display.get()}\n"
        analysis_text += f"{language['dielectric_label']} {self.dielectric_display.get()}\n\n"
        analysis_text += f"{language['main_parameters']}\n{'-' * 30}\n"
        analysis_text += f"{language[f'doping_concentration_{semiconductor_type}']} {doping_concentration:.2e} см⁻³\n"
        analysis_text += f"{language['thickness_label']} {self.thickness_variable.get()} нм\n"
        analysis_text += f"{language['area_label']} {self.area_variable.get()} мм²\n"
        analysis_text += f"{language['temperature_label']} {self.temperature_variable.get()} К\n\n"
        analysis_text += f"{language['calculated_characteristics']}\n{'-' * 30}\n"
        analysis_text += f"{language['max_capacitance']} {np.max(capacitances)*1e12:.2f} пФ\n"
        analysis_text += f"{language['min_capacitance']} {np.min(capacitances)*1e12:.2f} пФ\n"
        analysis_text += f"{language['ratio_cmin_cmax']} {np.min(capacitances)/np.max(capacitances):.3f}\n"
        analysis_text += f"{language['max_depletion_width']} {np.max(depletion_widths)*1e9:.2f} нм\n"
        analysis_text += f"{language['max_field']} {np.max(electric_fields)*1e-8:.2f} МВ/см\n"
        analysis_text += f"{language['max_potential']} {np.max(surface_potentials):.3f} В\n"

        self.analysis_text.delete(1.0, tk.END)
        self.analysis_text.insert(1.0, analysis_text)


    def save_data(self):
        """Сохранение рассчитанных данных и параметров в текстовый файл"""
        language = self.translations[self.current_language]

        try:
            filename = filedialog.asksaveasfilename(
                defaultextension=".txt",
                filetypes=[("Text files", "*.txt"), ("All files", "*.*")],
                title=language["save_data"]
            )

            if filename:
                results = self.calculate_capacitance()

                if results[0] is None:
                    messagebox.showwarning("Warning", language["warning_no_data"])
                    return

                voltages, capacitances, surface_potentials, depletion_widths, electric_fields, semiconductor_type, doping_concentration = results

                doping_type = "Donor concentration" if semiconductor_type == 'n' else "Acceptor concentration"

                if self.current_language == "Русский":
                    doping_type = "Концентрация доноров" if semiconductor_type == 'n' else "Концентрация акцепторов"

                with open(filename, 'w', encoding='utf-8') as file:
                    file.write("# Voltage[V] Capacitance[pF] Potential[V] Depletion[nm] Field[MV/cm]\n")
                    file.write(f"# Semiconductor: {self.semiconductor_display.get()}\n")
                    file.write(f"# Type: {self.semiconductor_type_display.get()}\n")
                    file.write(f"# Dielectric: {self.dielectric_display.get()}\n")
                    file.write(f"# Bandgap = {self.bandgap_variable.get()} eV\n")
                    file.write(f"# Permittivity = {self.permittivity_variable.get()}\n")
                    file.write(f"# Hole effective mass = {self.effective_mass_holes_variable.get()}\n")
                    file.write(f"# Electron effective mass = {self.effective_mass_electrons_variable.get()}\n")
                    file.write(f"# Ionization level = {self.ionization_level_variable.get()} eV\n")
                    file.write(f"# {doping_type} = {doping_concentration:.2e} cm⁻³\n")
                    file.write(f"# Temperature = {self.temperature_variable.get()} K\n")
                    file.write(f"# Dielectric thickness = {self.thickness_variable.get()} nm\n")
                    file.write(f"# Contact area = {self.area_variable.get()} mm²\n")
                    file.write(f"# Dielectric permittivity = {self.dielectric_permittivity_variable.get()}\n")

                    for voltage, capacitance, surface_potential, depletion_width, electric_field in zip(voltages, capacitances, surface_potentials, depletion_widths, electric_fields):
                        file.write(f"{voltage:.4f}    {capacitance*1e12:.6f}    {surface_potential:.6f}    {depletion_width*1e9:.6f}    {electric_field*1e-8:.6f}\n")

                messagebox.showinfo("Success", f"{language['success_save']}\n{filename}")
        except Exception as exception:
            messagebox.showerror("Error", f"{language['error_save']}\n{str(exception)}")


    def export_plots(self):
        """Экспорт текущих графиков в изображение (PNG/PDF)"""
        language = self.translations[self.current_language]

        try:
            filename = filedialog.asksaveasfilename(
                defaultextension=".png",
                filetypes=[("PNG files", "*.png"), ("PDF files", "*.pdf"), ("All files", "*.*")],
                title=language["export_plots"]
            )

            if filename:
                self.figure.savefig(filename, dpi=300, bbox_inches='tight')
                messagebox.showinfo("Success", f"{language['success_export']}\n{filename}")
        except Exception as exception:
            messagebox.showerror("Error", f"{language['error_export']}\n{str(exception)}")


def main():
    """Главная функция запуска приложения"""
    root = tk.Tk()
    application = MDPCapacitanceApplication(root)
    root.mainloop()


if __name__ == "__main__":
    main()
