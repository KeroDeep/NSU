import numpy as np
import matplotlib.pyplot as plt
from matplotlib.backends.backend_tkagg import FigureCanvasTkAgg, NavigationToolbar2Tk
import tkinter as tk
from tkinter import ttk, messagebox, filedialog
from PIL import Image, ImageTk
import matplotlib
matplotlib.use('TkAgg')
import sys

class MDPCapacitanceApplication:
    def __init__(self, root):
        self.root = root
        self.root.title("Анализ МДП структур")
        self.root.geometry("1600x1000")

        try:
            self.root.iconphoto(False, ImageTk.PhotoImage(Image.open("logo.png")))
        except Exception:
            pass
        
        # Убираем все выводы в консоль при закрытии
        self.root.protocol("WM_DELETE_WINDOW", self.on_closing)
        
        # Физические константы
        self.electron_charge = 1.6e-19
        self.vacuum_permittivity = 8.85e-12
        self.boltzmann_constant = 1.38e-23
        self.intrinsic_concentration = 1.5e16
        self.metal_work_function = 4.1
        self.silicon_affinity = 4.05
        
        # База данных материалов
        self.semiconductor_database = {
            "Кремний (Si)": {
                "bandgap": 1.12,
                "permittivity": 11.7,
                "effective_mass_holes": 0.81,
                "effective_mass_electrons": 1.18,
                "donor_level": 0.045,
                "doping_concentration_log": 16.0,
                "temperature": 300
            },
            "Германий (Ge)": {
                "bandgap": 0.66,
                "permittivity": 16.0,
                "effective_mass_holes": 0.34,
                "effective_mass_electrons": 0.55,
                "donor_level": 0.012,
                "doping_concentration_log": 16.0,
                "temperature": 300
            },
            "Арсенид галлия (GaAs)": {
                "bandgap": 1.42,
                "permittivity": 12.9,
                "effective_mass_holes": 0.51,
                "effective_mass_electrons": 0.067,
                "donor_level": 0.006,
                "doping_concentration_log": 16.0,
                "temperature": 300
            }
        }

        self.dielectric_database = {
            "Диоксид кремния (SiO₂)": {"permittivity": 3.9},
            "Нитрид кремния (Si₃N₄)": {"permittivity": 7.5},
            "Оксид гафния (HfO₂)": {"permittivity": 25.0},
            "Оксид алюминия (Al₂O₃)": {"permittivity": 9.0}
        }
        
        # Переменные интерфейса
        self.semiconductor_variable = tk.StringVar(value="Кремний (Si)")
        self.dielectric_variable = tk.StringVar(value="Диоксид кремния (SiO₂)")
        self.bandgap_variable = tk.DoubleVar(value=1.12)
        self.permittivity_variable = tk.DoubleVar(value=11.7)
        self.effective_mass_holes_variable = tk.DoubleVar(value=0.81)
        self.effective_mass_electrons_variable = tk.DoubleVar(value=1.18)
        self.donor_level_variable = tk.DoubleVar(value=0.045)
        self.doping_concentration_log_variable = tk.DoubleVar(value=16.0)
        self.temperature_variable = tk.DoubleVar(value=300)
        self.thickness_variable = tk.DoubleVar(value=10.0)
        self.area_variable = tk.DoubleVar(value=1.0)
        self.dielectric_permittivity_variable = tk.DoubleVar(value=3.9)
        self.maximum_voltage_variable = tk.DoubleVar(value=5.0)
        self.voltage_step_variable = tk.DoubleVar(value=0.01)
        
        # Создание интерфейса
        self.create_widgets()
        self.update_semiconductor_parameters()
        self.update_dielectric_parameters()
        self.calculate()
    
    def on_closing(self):
        """Корректное закрытие приложения"""
        self.root.quit()
        self.root.destroy()
    
    def create_widgets(self):
        """Создание всех элементов интерфейса"""
        main_frame = ttk.Frame(self.root, padding="10")
        main_frame.pack(fill=tk.BOTH, expand=True)
        
        # Панель управления - 4 колонки
        control_frame = ttk.LabelFrame(main_frame, text="Панель управления", padding="15")
        control_frame.pack(fill=tk.X, padx=10, pady=5)
        
        # Настройка равномерного распределения колонок
        control_frame.columnconfigure(0, weight=1, uniform="cols")
        control_frame.columnconfigure(1, weight=1, uniform="cols")
        control_frame.columnconfigure(2, weight=1, uniform="cols")
        control_frame.columnconfigure(3, weight=1, uniform="cols")
        
        # Создаем 4 колонки
        self.create_buttons_column(control_frame)
        self.create_materials_column(control_frame)
        self.create_geometry_column(control_frame)
        self.create_semiconductor_column(control_frame)
        
        # Область графиков
        plot_frame = ttk.LabelFrame(main_frame, text="Результаты моделирования", padding="10")
        plot_frame.pack(fill=tk.BOTH, expand=True, padx=10, pady=5)
        
        # Панель анализа
        analysis_frame = ttk.LabelFrame(main_frame, text="Анализ параметров структуры", padding="10")
        analysis_frame.pack(fill=tk.X, padx=10, pady=5)
        
        self.create_plot_area(plot_frame)
        self.create_analysis_panel(analysis_frame)
    
    def create_buttons_column(self, parent):
        """Колонка с кнопками управления"""
        buttons_frame = ttk.LabelFrame(parent, text="Управление", padding="15")
        buttons_frame.grid(row=0, column=0, sticky="nsew", padx=(0, 10))
        
        # Контейнер для центрирования
        container = ttk.Frame(buttons_frame)
        container.pack(expand=True, fill=tk.BOTH)
        
        # Центральный фрейм для элементов
        center_frame = ttk.Frame(container)
        center_frame.pack(expand=True)
        
        # Кнопки с фиксированной шириной (без растягивания)
        button_width = 22
        
        ttk.Button(center_frame, text="Сохранить данные", command=self.save_data, width=button_width).pack(pady=8)
        ttk.Button(center_frame, text="Экспорт графиков", command=self.export_plots, width=button_width).pack(pady=8)
        ttk.Button(center_frame, text="Сбросить параметры", command=self.reset_parameters, width=button_width).pack(pady=8)
    
    def create_materials_column(self, parent):
        """Колонка выбора материалов"""
        materials_frame = ttk.LabelFrame(parent, text="Материалы", padding="15")
        materials_frame.grid(row=0, column=1, sticky="nsew", padx=(0, 10))
        
        # Контейнер для центрирования
        container = ttk.Frame(materials_frame)
        container.pack(expand=True, fill=tk.BOTH)
        
        # Центральный фрейм для элементов
        center_frame = ttk.Frame(container)
        center_frame.pack(expand=True)
        
        # Определяем максимальную ширину для выравнивания
        max_width = 25
        
        # Выбор полупроводника
        semiconductor_frame = ttk.Frame(center_frame)
        semiconductor_frame.pack(pady=12)
        ttk.Label(semiconductor_frame, text="Полупроводник:", width=15, anchor=tk.W).pack(side=tk.LEFT, padx=(0, 8))
        semiconductor_combobox = ttk.Combobox(semiconductor_frame, textvariable=self.semiconductor_variable, values=list(self.semiconductor_database.keys()), state="readonly", width=max_width)
        semiconductor_combobox.pack(side=tk.LEFT)
        semiconductor_combobox.bind('<<ComboboxSelected>>', self.on_semiconductor_change)
        
        # Выбор диэлектрика
        dielectric_frame = ttk.Frame(center_frame)
        dielectric_frame.pack(pady=12)
        ttk.Label(dielectric_frame, text="Диэлектрик:", width=15, anchor=tk.W).pack(side=tk.LEFT, padx=(0, 8))
        dielectric_combobox = ttk.Combobox(dielectric_frame, textvariable=self.dielectric_variable, values=list(self.dielectric_database.keys()), state="readonly", width=max_width)
        dielectric_combobox.pack(side=tk.LEFT)
        dielectric_combobox.bind('<<ComboboxSelected>>', self.on_dielectric_change)
    
    def create_geometry_column(self, parent):
        """Колонка геометрических параметров"""
        geometry_frame = ttk.LabelFrame(parent, text="Геометрические параметры", padding="15")
        geometry_frame.grid(row=0, column=2, sticky="nsew", padx=(0, 10))
        
        # Контейнер для центрирования
        container = ttk.Frame(geometry_frame)
        container.pack(expand=True, fill=tk.BOTH)
        
        # Центральный фрейм для элементов
        center_frame = ttk.Frame(container)
        center_frame.pack(expand=True)
        
        # Параметры структуры с центрированием
        self.create_centered_parameter_input(center_frame, "Толщина диэлектрика, нм:", self.thickness_variable, 0.1, 1000.0, 1.0, 25)
        self.create_centered_parameter_input(center_frame, "Площадь контакта, мм²:", self.area_variable, 0.001, 1000.0, 0.1, 25)
        self.create_centered_parameter_input(center_frame, "Максимальное напряжение, В:", self.maximum_voltage_variable, 0.01, 100.0, 0.5, 25)
        self.create_centered_parameter_input(center_frame, "Шаг напряжения, В:", self.voltage_step_variable, 0.0001, 1.0, 0.001, 25)
    
    def create_semiconductor_column(self, parent):
        """Колонка параметров полупроводника"""
        semiconductor_frame = ttk.LabelFrame(parent, text="Параметры полупроводника", padding="15")
        semiconductor_frame.grid(row=0, column=3, sticky="nsew")
        
        # Контейнер для центрирования
        container = ttk.Frame(semiconductor_frame)
        container.pack(expand=True, fill=tk.BOTH)
        
        # Центральный фрейм для элементов
        center_frame = ttk.Frame(container)
        center_frame.pack(expand=True)
        
        # Параметры полупроводника с центрированием
        self.create_centered_parameter_input(center_frame, "Запрещенная зона, эВ:", self.bandgap_variable, 0.01, 10.0, 0.1, 30)
        self.create_centered_parameter_input(center_frame, "Диэлектрическая проницаемость:", self.permittivity_variable, 1.0, 100.0, 1.0, 30)
        self.create_centered_parameter_input(center_frame, "Эффективная масса дырок:", self.effective_mass_holes_variable, 0.01, 10.0, 0.1, 30)
        self.create_centered_parameter_input(center_frame, "Эффективная масса электронов:", self.effective_mass_electrons_variable, 0.01, 10.0, 0.1, 30)
        self.create_centered_parameter_input(center_frame, "Уровень донора, эВ:", self.donor_level_variable, 0.0001, 1.0, 0.01, 30)
        self.create_centered_parameter_input(center_frame, "Концентрация доноров, log₁₀(см⁻³):", self.doping_concentration_log_variable, 10.0, 25.0, 0.5, 30)
        self.create_centered_parameter_input(center_frame, "Температура, К:", self.temperature_variable, 1.0, 1500.0, 10.0, 30)
    
    def create_centered_parameter_input(self, parent, label, variable, minimum_value, maximum_value, increment, label_width):
        """Создание центрированного элемента ввода"""
        frame = ttk.Frame(parent)
        frame.pack(pady=4)
        
        # Подпись слева
        ttk.Label(frame, text=label, width=label_width, anchor=tk.W).pack(side=tk.LEFT, padx=(0, 10))
        
        def validate_input(new_value):
            """Валидация вводимых данных"""
            if new_value == "":
                return True
            try:
                value = float(new_value)
                if minimum_value <= value <= maximum_value:
                    return True
                return False
            except Exception:
                return False
        
        validate_command = (self.root.register(validate_input), '%P')
        
        spinbox = ttk.Spinbox(frame, textvariable=variable, from_=minimum_value, to=maximum_value, increment=increment, width=12, command=self.calculate, validate="key", validatecommand=validate_command)
        spinbox.pack(side=tk.LEFT)
        spinbox.bind('<KeyRelease>', lambda event: self.calculate())

    def create_parameter_input(self, parent, label, variable, minimum_value, maximum_value, increment, label_width):
        """Создание элемента ввода с подписью слева"""
        frame = ttk.Frame(parent)
        frame.pack(fill=tk.X, pady=4)
        
        # Подпись слева с увеличенной шириной
        ttk.Label(frame, text=label, width=label_width, anchor=tk.W).pack(side=tk.LEFT, padx=(0, 10))
        
        def validate_input(new_value):
            """Валидация вводимых данных"""
            if new_value == "":
                return True
            try:
                value = float(new_value)
                if minimum_value <= value <= maximum_value:
                    return True
                return False
            except Exception:
                return False
        
        validate_command = (self.root.register(validate_input), '%P')
        
        spinbox = ttk.Spinbox(frame, textvariable=variable, from_=minimum_value, to=maximum_value, increment=increment, width=12, command=self.calculate, validate="key", validatecommand=validate_command)
        spinbox.pack(side=tk.LEFT)
        spinbox.bind('<KeyRelease>', lambda event: self.calculate())
    
    def create_plot_area(self, parent):
        """Создание области для графиков"""
        # Создаем фигуру с правильными настройками размера
        self.figure, ((self.axes1, self.axes2), (self.axes3, self.axes4)) = plt.subplots(2, 2, figsize=(14, 8))
        
        self.canvas = FigureCanvasTkAgg(self.figure, master=parent)
        self.canvas.get_tk_widget().pack(fill=tk.BOTH, expand=True)
        
        toolbar = NavigationToolbar2Tk(self.canvas, parent)
        toolbar.update()
        
        # Настройка осей графиков
        self.setup_plot_axes()
        
        self.figure.tight_layout(pad=3.0, h_pad=3.0, w_pad=3.0)
        self.canvas.draw()
    
    def setup_plot_axes(self):
        """Настройка осей графиков"""
        # График 1: Вольт-фарадная характеристика
        self.axes1.set_xlabel('Напряжение, В')
        self.axes1.set_ylabel('Ёмкость, пФ')
        self.axes1.set_title('Вольт-фарадная характеристика')
        self.axes1.grid(True, alpha=0.3)
        
        # График 2: Ширина ОПЗ
        self.axes2.set_xlabel('Напряжение, В')
        self.axes2.set_ylabel('Ширина ОПЗ, нм')
        self.axes2.set_title('Ширина области пространственного заряда')
        self.axes2.grid(True, alpha=0.3)
        
        # График 3: Поверхностный потенциал
        self.axes3.set_xlabel('Напряжение, В')
        self.axes3.set_ylabel('Поверхностный потенциал, В')
        self.axes3.set_title('Поверхностный потенциал')
        self.axes3.grid(True, alpha=0.3)
        
        # График 4: Электрическое поле
        self.axes4.set_xlabel('Напряжение, В')
        self.axes4.set_ylabel('Электрическое поле, МВ/см')
        self.axes4.set_title('Максимальное электрическое поле')
        self.axes4.grid(True, alpha=0.3)
    
    def create_analysis_panel(self, parent):
        """Создание панели анализа результатов"""
        self.analysis_text = tk.Text(parent, height=8, width=100, font=('Consolas', 9))
        scrollbar = ttk.Scrollbar(parent, orient=tk.VERTICAL, command=self.analysis_text.yview)
        self.analysis_text.configure(yscrollcommand=scrollbar.set)
        
        self.analysis_text.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)
        scrollbar.pack(side=tk.RIGHT, fill=tk.Y)
    
    def on_semiconductor_change(self, event=None):
        """Обработчик изменения полупроводника"""
        self.update_semiconductor_parameters()
        self.calculate()
    
    def on_dielectric_change(self, event=None):
        """Обработчик изменения диэлектрика"""
        self.update_dielectric_parameters()
        self.calculate()
    
    def reset_parameters(self):
        """Сброс параметров к значениям по умолчанию"""
        # Сброс материалов
        self.semiconductor_variable.set("Кремний (Si)")
        self.dielectric_variable.set("Диоксид кремния (SiO₂)")
        
        # Обновление параметров материалов из базы данных
        self.update_semiconductor_parameters()
        self.update_dielectric_parameters()
        
        # Сброс геометрических параметров к значениям по умолчанию
        self.thickness_variable.set(10.0)
        self.area_variable.set(1.0)
        self.maximum_voltage_variable.set(5.0)
        self.voltage_step_variable.set(0.01)
        
        self.calculate()
    
    def update_semiconductor_parameters(self):
        """Обновление параметров полупроводника из базы данных"""
        semiconductor_name = self.semiconductor_variable.get()
        if semiconductor_name in self.semiconductor_database:
            data = self.semiconductor_database[semiconductor_name]
            self.bandgap_variable.set(data["bandgap"])
            self.permittivity_variable.set(data["permittivity"])
            self.effective_mass_holes_variable.set(data["effective_mass_holes"])
            self.effective_mass_electrons_variable.set(data["effective_mass_electrons"])
            self.donor_level_variable.set(data["donor_level"])
            self.doping_concentration_log_variable.set(data["doping_concentration_log"])
            self.temperature_variable.set(data["temperature"])
    
    def update_dielectric_parameters(self):
        """Обновление параметров диэлектрика из базы данных"""
        dielectric_name = self.dielectric_variable.get()
        if dielectric_name in self.dielectric_database:
            data = self.dielectric_database[dielectric_name]
            self.dielectric_permittivity_variable.set(data["permittivity"])
    
    def calculate_capacitance(self):
        """Расчет емкости МДП структуры с полным циклом напряжений"""
        try:
            # Получение параметров
            bandgap = self.bandgap_variable.get()
            epsilon_s = self.permittivity_variable.get()
            doping_log = self.doping_concentration_log_variable.get()
            temperature = self.temperature_variable.get()
            thickness = self.thickness_variable.get() * 1e-9
            area = self.area_variable.get() * 1e-6
            epsilon_d = self.dielectric_permittivity_variable.get()
            maximum_voltage = self.maximum_voltage_variable.get()
            voltage_step = self.voltage_step_variable.get()
            
            # Концентрация доноров
            doping_concentration = 10 ** doping_log
            
            # Диэлектрические проницаемости
            epsilon_semiconductor = epsilon_s * self.vacuum_permittivity
            epsilon_dielectric = epsilon_d * self.vacuum_permittivity
            
            # Емкость диэлектрика
            dielectric_capacitance = epsilon_dielectric * area / thickness
            
            # Расчет уровня Ферми
            fermi_potential = (self.boltzmann_constant * temperature / self.electron_charge) * np.log(doping_concentration * 1e6 / self.intrinsic_concentration)
            
            # Напряжение плоских зон
            flatband_voltage = self.metal_work_function - (self.silicon_affinity + bandgap/2 - fermi_potential)
            
            # Полный цикл напряжений для отображения всех эффектов
            positive_voltages = np.arange(maximum_voltage, 0, -voltage_step)
            negative_voltages = np.arange(0, -maximum_voltage, -voltage_step)
            voltages = np.concatenate([positive_voltages, negative_voltages])
            
            capacitances = []
            surface_potentials = []
            depletion_widths = []
            electric_fields = []
            
            # Параметр для расчета
            calculation_parameter = (epsilon_semiconductor / epsilon_dielectric) * thickness * np.sqrt(2 * self.electron_charge * doping_concentration * 1e6 / epsilon_semiconductor)
            threshold_voltage = 2 * fermi_potential
            
            for voltage in voltages:
                effective_voltage = voltage - flatband_voltage
                
                if effective_voltage <= 0:
                    # Режим накопления
                    capacitances.append(dielectric_capacitance)
                    surface_potentials.append(0)
                    depletion_widths.append(0)
                    electric_fields.append(0)
                else:
                    # Режим обеднения/инверсии
                    discriminant = calculation_parameter**2 + 4 * effective_voltage
                    if discriminant < 0:
                        surface_potential = 0
                    else:
                        surface_potential = ((-calculation_parameter + np.sqrt(discriminant)) / 2)**2
                    
                    # Ширина ОПЗ
                    depletion_width = np.sqrt(2 * epsilon_semiconductor * surface_potential / (self.electron_charge * doping_concentration * 1e6))
                    
                    # Коррекция для режима инверсии
                    if surface_potential > threshold_voltage:
                        minimum_width = np.sqrt(2 * epsilon_semiconductor * threshold_voltage / (self.electron_charge * doping_concentration * 1e6))
                        depletion_width = minimum_width + (depletion_width - minimum_width) * np.tanh((surface_potential - threshold_voltage) / 0.1)
                    
                    # Емкость полупроводника
                    semiconductor_capacitance = epsilon_semiconductor * area / depletion_width
                    
                    # Полная емкость
                    total_capacitance = 1 / (1/dielectric_capacitance + 1/semiconductor_capacitance)
                    
                    # Электрическое поле
                    electric_field = np.sqrt(2 * self.electron_charge * doping_concentration * 1e6 * surface_potential / epsilon_semiconductor)
                    
                    capacitances.append(total_capacitance)
                    surface_potentials.append(surface_potential)
                    depletion_widths.append(depletion_width)
                    electric_fields.append(electric_field)
            
            return (np.array(voltages), np.array(capacitances), np.array(surface_potentials), np.array(depletion_widths), np.array(electric_fields))
            
        except Exception:
            return None, None, None, None, None
    
    def calculate(self, event=None):
        """Автоматический расчет при изменении параметров"""
        try:
            results = self.calculate_capacitance()
            if results[0] is not None:
                self.update_plot(*results)
                self.update_analysis(*results)
        except Exception:
            pass
    
    def update_plot(self, voltages, capacitances, surface_potentials, depletion_widths, electric_fields):
        """Обновление графиков"""
        # Очистка графиков
        for axes in [self.axes1, self.axes2, self.axes3, self.axes4]:
            axes.clear()
        
        # График 1: Вольт-фарадная характеристика
        self.axes1.plot(voltages, capacitances * 1e12, 'b-', linewidth=2)
        self.axes1.set_xlabel('Напряжение, В')
        self.axes1.set_ylabel('Ёмкость, пФ')
        self.axes1.set_title('Вольт-фарадная характеристика')
        self.axes1.grid(True, alpha=0.3)
        
        # График 2: Ширина ОПЗ
        self.axes2.plot(voltages, depletion_widths * 1e9, 'r-', linewidth=2)
        self.axes2.set_xlabel('Напряжение, В')
        self.axes2.set_ylabel('Ширина ОПЗ, нм')
        self.axes2.set_title('Ширина области пространственного заряда')
        self.axes2.grid(True, alpha=0.3)
        
        # График 3: Поверхностный потенциал
        self.axes3.plot(voltages, surface_potentials, 'g-', linewidth=2)
        self.axes3.set_xlabel('Напряжение, В')
        self.axes3.set_ylabel('Поверхностный потенциал, В')
        self.axes3.set_title('Поверхностный потенциал')
        self.axes3.grid(True, alpha=0.3)
        
        # График 4: Электрическое поле
        self.axes4.plot(voltages, electric_fields * 1e-6, 'm-', linewidth=2)
        self.axes4.set_xlabel('Напряжение, В')
        self.axes4.set_ylabel('Электрическое поле, МВ/см')
        self.axes4.set_title('Максимальное электрическое поле')
        self.axes4.grid(True, alpha=0.3)
        
        self.canvas.draw_idle()
    
    def update_analysis(self, voltages, capacitances, surface_potentials, depletion_widths, electric_fields):
        """Обновление панели анализа"""
        analysis_text = "РЕЗУЛЬТАТЫ АНАЛИЗА МДП-СТРУКТУРЫ\n"
        analysis_text += "=" * 50 + "\n\n"
        
        analysis_text += f"Материал: {self.semiconductor_variable.get()}\n"
        analysis_text += f"Диэлектрик: {self.dielectric_variable.get()}\n\n"
        
        analysis_text += "ОСНОВНЫЕ ПАРАМЕТРЫ:\n"
        analysis_text += "-" * 30 + "\n"
        analysis_text += f"Концентрация доноров: {10**self.doping_concentration_log_variable.get():.2e} см⁻³\n"
        analysis_text += f"Толщина диэлектрика: {self.thickness_variable.get()} нм\n"
        analysis_text += f"Площадь контакта: {self.area_variable.get()} мм²\n"
        analysis_text += f"Температура: {self.temperature_variable.get()} К\n\n"
        
        analysis_text += "РАСЧЕТНЫЕ ХАРАКТЕРИСТИКИ:\n"
        analysis_text += "-" * 30 + "\n"
        analysis_text += f"Максимальная ёмкость: {np.max(capacitances)*1e12:.2f} пФ\n"
        analysis_text += f"Минимальная ёмкость: {np.min(capacitances)*1e12:.2f} пФ\n"
        analysis_text += f"Отношение Cmin/Cmax: {np.min(capacitances)/np.max(capacitances):.3f}\n"
        analysis_text += f"Максимальная ширина ОПЗ: {np.max(depletion_widths)*1e9:.2f} нм\n"
        analysis_text += f"Максимальное поле: {np.max(electric_fields)*1e-6:.2f} МВ/см\n"
        analysis_text += f"Максимальный потенциал: {np.max(surface_potentials):.3f} В\n"
        
        self.analysis_text.delete(1.0, tk.END)
        self.analysis_text.insert(1.0, analysis_text)
    
    def save_data(self):
        """Сохранение данных в файл"""
        try:
            filename = filedialog.asksaveasfilename(
                defaultextension=".txt",
                filetypes=[("Текстовые файлы", "*.txt"), ("Все файлы", "*.*")],
                title="Сохранить данные"
            )
            
            if filename:
                results = self.calculate_capacitance()
                if results[0] is None:
                    messagebox.showwarning("Предупреждение", "Нет данных для сохранения")
                    return
                
                voltages, capacitances, surface_potentials, depletion_widths, electric_fields = results
                
                actual_doping_concentration = 10 ** self.doping_concentration_log_variable.get()
                
                with open(filename, 'w', encoding='utf-8') as file:
                    file.write("# Вольт-фарадная характеристика МДП-структуры\n")
                    file.write("# Напряжение[В] Ёмкость[пФ] Потенциал[В] ОПЗ[нм] Поле[МВ/см]\n")
                    file.write(f"# Полупроводник: {self.semiconductor_variable.get()}\n")
                    file.write(f"# Диэлектрик: {self.dielectric_variable.get()}\n")
                    file.write(f"# Запрещенная зона = {self.bandgap_variable.get()} эВ\n")
                    file.write(f"# Диэлектрическая проницаемость = {self.permittivity_variable.get()}\n")
                    file.write(f"# Эффективная масса дырок = {self.effective_mass_holes_variable.get()}\n")
                    file.write(f"# Эффективная масса электронов = {self.effective_mass_electrons_variable.get()}\n")
                    file.write(f"# Уровень донора = {self.donor_level_variable.get()} эВ\n")
                    file.write(f"# Концентрация доноров = {actual_doping_concentration:.2e} см⁻³\n")
                    file.write(f"# Температура = {self.temperature_variable.get()} К\n")
                    file.write(f"# Толщина диэлектрика = {self.thickness_variable.get()} нм\n")
                    file.write(f"# Площадь контакта = {self.area_variable.get()} мм²\n")
                    file.write(f"# Диэлектрическая проницаемость диэлектрика = {self.dielectric_permittivity_variable.get()}\n")
                    
                    for voltage, capacitance, surface_potential, depletion_width, electric_field in zip(voltages, capacitances, surface_potentials, depletion_widths, electric_fields):
                        file.write(f"{voltage:.4f}    {capacitance*1e12:.6f}    {surface_potential:.6f}    {depletion_width*1e9:.6f}    {electric_field*1e-6:.6f}\n")
                
                messagebox.showinfo("Успех", f"Данные сохранены в файл:\n{filename}")
                
        except Exception as exception:
            messagebox.showerror("Ошибка", f"Не удалось сохранить данные:\n{str(exception)}")
    
    def export_plots(self):
        """Экспорт графиков в файл"""
        try:
            filename = filedialog.asksaveasfilename(
                defaultextension=".png",
                filetypes=[("PNG файлы", "*.png"), ("PDF файлы", "*.pdf"), ("Все файлы", "*.*")],
                title="Экспорт графиков"
            )
            
            if filename:
                self.figure.savefig(filename, dpi=300, bbox_inches='tight')
                messagebox.showinfo("Успех", f"Графики экспортированы в файл:\n{filename}")
                
        except Exception as exception:
            messagebox.showerror("Ошибка", f"Не удалось экспортировать графики:\n{str(exception)}")

def main():
    """Главная функция приложения"""
    root = tk.Tk()
    application = MDPCapacitanceApplication(root)
    root.mainloop()

if __name__ == "__main__":
    main()
