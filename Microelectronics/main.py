import numpy as np
import matplotlib.pyplot as plt
from matplotlib.backends.backend_tkagg import FigureCanvasTkAgg, NavigationToolbar2Tk
import tkinter as tk
from tkinter import ttk, messagebox, filedialog
import matplotlib
matplotlib.use('TkAgg')
import sys

class MDPCapacitanceApp:
    def __init__(self, root):
        self.root = root
        self.root.title("Расчет вольт-фарадной характеристики МДП-структуры")
        self.root.geometry("1400x900")
        
        self.root.protocol("WM_DELETE_WINDOW", self.onClosing)
        
        self.q = 1.6e-19
        self.eps0 = 8.85e-12
        self.k = 1.38e-23
        self.ni = 1.5e16
        self.phi_m = 4.1
        self.chi_si = 4.05
        
        self.semiconductorData = {
            "Кремний (Si)": {
                "Eg": 1.12,
                "epsilon": 11.7,
                "md_h": 0.81,
                "md_e": 1.18,
                "Ed": 0.045,
                "Nd0": 16.0,
                "T": 300
            },
            "Германий (Ge)": {
                "Eg": 0.66,
                "epsilon": 16.0,
                "md_h": 0.34,
                "md_e": 0.55,
                "Ed": 0.012,
                "Nd0": 16.0,
                "T": 300
            }
        }
        
        self.semiconductorVar = tk.StringVar(value="Кремний (Si)")
        self.EgVar = tk.DoubleVar(value=1.12)
        self.epsilonVar = tk.DoubleVar(value=11.7)
        self.md_hVar = tk.DoubleVar(value=0.81)
        self.md_eVar = tk.DoubleVar(value=1.18)
        self.EdVar = tk.DoubleVar(value=0.045)
        self.Nd0Var = tk.DoubleVar(value=16.0)
        self.TVar = tk.DoubleVar(value=300)
        self.dVar = tk.DoubleVar(value=10.0)
        self.SVar = tk.DoubleVar(value=1.0)
        self.epsilon_dVar = tk.DoubleVar(value=3.9)
        self.UmaxVar = tk.DoubleVar(value=5.0)
        
        self.createWidgets()
        self.updateSemiconductorParams()
        self.calculate()
    
    def onClosing(self):
        self.root.quit()
        self.root.destroy()
    
    def createWidgets(self):
        mainFrame = ttk.Frame(self.root, padding="10")
        mainFrame.pack(fill=tk.BOTH, expand=True)
        
        controlFrame = ttk.LabelFrame(mainFrame, text="Панель управления", padding="15")
        controlFrame.pack(fill=tk.X, padx=10, pady=5)
        
        plotFrame = ttk.LabelFrame(mainFrame, text="График вольт-фарадной характеристики", padding="10")
        plotFrame.pack(fill=tk.BOTH, expand=True, padx=10, pady=5)
        
        self.createControlPanel(controlFrame)
        self.createPlotArea(plotFrame)
    
    def createControlPanel(self, parent):
        topFrame = ttk.Frame(parent)
        topFrame.pack(fill=tk.X, pady=10)
        
        centerFrame = ttk.Frame(topFrame)
        centerFrame.pack(expand=True)
        
        ttk.Label(centerFrame, text="Полупроводник:").pack(side=tk.LEFT, padx=(0, 10))
        
        sem_combo = ttk.Combobox(centerFrame, textvariable=self.semiconductorVar, values=["Кремний (Si)", "Германий (Ge)", "Пользовательский"], state="readonly", width=20)
        sem_combo.pack(side=tk.LEFT, padx=(0, 20))
        sem_combo.bind('<<ComboboxSelected>>', self.onSemiconductorChange)
        
        ttk.Button(centerFrame, text="Сохранить данные", command=self.saveASCII).pack(side=tk.LEFT)
        
        paramsFrame = ttk.Frame(parent)
        paramsFrame.pack(fill=tk.X, pady=10)
        
        leftFrame = ttk.LabelFrame(paramsFrame, text="Параметры полупроводника", padding="10")
        leftFrame.pack(side=tk.LEFT, fill=tk.BOTH, expand=True, padx=(0, 10))
        
        rightFrame = ttk.LabelFrame(paramsFrame, text="Параметры структуры", padding="10")
        rightFrame.pack(side=tk.LEFT, fill=tk.BOTH, expand=True, padx=(10, 0))
        
        max_width = 35
        
        self.createParameterInput(leftFrame, "Запрещенная зона, эВ:", self.EgVar, 0.1, 5.0, 0.1, max_width)
        self.createParameterInput(leftFrame, "Диэлектрическая проницаемость:", self.epsilonVar, 1.0, 50.0, 1.0, max_width)
        self.createParameterInput(leftFrame, "Эффективная масса дырок:", self.md_hVar, 0.1, 5.0, 0.1, max_width)
        self.createParameterInput(leftFrame, "Эффективная масса электронов:", self.md_eVar, 0.1, 5.0, 0.1, max_width)
        self.createParameterInput(leftFrame, "Уровень донора, эВ:", self.EdVar, 0.001, 0.5, 0.01, max_width)
        self.createParameterInput(leftFrame, "Концентрация доноров, log₁₀(см⁻³):", self.Nd0Var, 10.0, 20.0, 0.5, max_width)
        self.createParameterInput(leftFrame, "Температура, К:", self.TVar, 50.0, 1000.0, 10.0, max_width)
        
        self.createParameterInput(rightFrame, "Толщина диэлектрика, нм:", self.dVar, 1.0, 500.0, 5.0, max_width)
        self.createParameterInput(rightFrame, "Площадь контакта, мм²:", self.SVar, 0.1, 100.0, 1.0, max_width)
        self.createParameterInput(rightFrame, "Диэлектрическая проницаемость диэлектрика:", self.epsilon_dVar, 1.0, 50.0, 1.0, max_width)
        self.createParameterInput(rightFrame, "Максимальное напряжение, В:", self.UmaxVar, 0.5, 50.0, 1.0, max_width)
    
    def createParameterInput(self, parent, label, variable, from_, to, increment, width):
        frame = ttk.Frame(parent)
        frame.pack(fill=tk.X, pady=3)
        
        ttk.Label(frame, text=label, width=width, anchor=tk.W).pack(side=tk.LEFT)
        
        def validate_input(P):
            if P == "":
                return True
            try:
                value = float(P)
                if from_ <= value <= to:
                    return True
                return False
            except:
                return False
        
        vcmd = (self.root.register(validate_input), '%P')
        
        spinbox = ttk.Spinbox(frame, textvariable=variable, from_=from_, to=to, increment=increment, width=15, command=self.calculate, validate="key", validatecommand=vcmd)
        spinbox.pack(side=tk.LEFT, padx=(0, 5))
        spinbox.bind('<KeyRelease>', lambda exception: self.calculate())
    
    def createPlotArea(self, parent):
        self.fig, self.ax = plt.subplots(figsize=(12, 7))
        
        self.canvas = FigureCanvasTkAgg(self.fig, master=parent)
        self.canvas.get_tk_widget().pack(fill=tk.BOTH, expand=True)
        
        toolbar = NavigationToolbar2Tk(self.canvas, parent)
        toolbar.update()
        
        self.ax.set_xlabel('Напряжение, В')
        self.ax.set_ylabel('Ёмкость, пФ')
        self.ax.set_title('Вольт-фарадная характеристика МДП-структуры')
        self.ax.grid(True, alpha=0.3)
        self.canvas.draw()
    
    def onSemiconductorChange(self, event=None):
        if self.semiconductorVar.get() in ["Кремний (Si)", "Германий (Ge)"]:
            self.updateSemiconductorParams()
        self.calculate()
    
    def updateSemiconductorParams(self):
        if self.semiconductorVar.get() in self.semiconductorData:
            data = self.semiconductorData[self.semiconductorVar.get()]
            self.EgVar.set(data["Eg"])
            self.epsilonVar.set(data["epsilon"])
            self.md_hVar.set(data["md_h"])
            self.md_eVar.set(data["md_e"])
            self.EdVar.set(data["Ed"])
            self.Nd0Var.set(data["Nd0"])
            self.TVar.set(data["T"])
    
    def calculateCapacitance(self):
        Eg = self.EgVar.get()
        epsilon = self.epsilonVar.get()
        md_h = self.md_hVar.get()
        md_e = self.md_eVar.get()
        Ed = self.EdVar.get()
        Nd0_log = self.Nd0Var.get()
        T = self.TVar.get()
        d = self.dVar.get() * 1e-9
        S = self.SVar.get() * 1e-6
        epsilon_d = self.epsilon_dVar.get()
        U_max = self.UmaxVar.get()
        
        Nd0 = 10 ** Nd0_log
        
        eps_semi = epsilon * self.eps0
        eps_diel = epsilon_d * self.eps0
        
        phi_F = (self.k * T / self.q) * np.log(Nd0 * 1e6 / self.ni)
        V_FB = self.phi_m - (self.chi_si + Eg/2 - phi_F)
        Cox = eps_diel * S / d
        
        V_G = np.linspace(-U_max, 0, 1000)
        C = []
        
        K = (eps_semi / eps_diel) * d * np.sqrt(2 * self.q * Nd0 * 1e6 / eps_semi)
        U_th = 2 * phi_F
        
        for Vg in V_G:
            U = Vg - V_FB
            if U <= 0:
                C.append(Cox)
            else:
                disc = K**2 + 4 * U
                if disc < 0:
                    Us = 0
                else:
                    Us = ((-K + np.sqrt(disc)) / 2)**2
                
                W = np.sqrt(2 * eps_semi * Us / (self.q * Nd0 * 1e6))
                if Us > U_th:
                    W_min = np.sqrt(2 * eps_semi * U_th / (self.q * Nd0 * 1e6))
                    W = W_min + (W - W_min) * np.tanh((Us - U_th) / 0.1)
                Cs = eps_semi * S / W
                C_total = 1 / (1/Cox + 1/Cs)
                C.append(C_total)
        
        return V_G, np.array(C)
    
    def calculate(self, event=None):
        try:
            self._calculate()
        except:
            pass
    
    def _calculate(self):
        try:
            voltages, capacitances = self.calculateCapacitance()
            self.updatePlot(voltages, capacitances)
        except Exception as exception:
            pass
    
    def updatePlot(self, voltages, capacitances):
        self.ax.clear()
        
        capacitances_pF = capacitances * 1e12
        
        self.ax.plot(voltages, capacitances_pF, 'b-', linewidth=2)
        self.ax.set_xlabel('Напряжение, В')
        self.ax.set_ylabel('Ёмкость, пФ')
        self.ax.set_title('Вольт-фарадная характеристика МДП-структуры')
        self.ax.grid(True, alpha=0.3)
        
        if len(capacitances_pF) > 0:
            y_min = capacitances_pF.min() * 0.95
            y_max = capacitances_pF.max() * 1.05
            self.ax.set_ylim(y_min, y_max)
        
        self.canvas.draw_idle()
    
    def saveASCII(self):
        try:
            filename = filedialog.asksaveasfilename(
                defaultextension=".txt",
                filetypes=[("Text files", "*.txt"), ("All files", "*.*")],
                title="Сохранить данные"
            )
            if filename:
                lines = self.ax.get_lines()
                if not lines or len(lines[0].get_xdata()) == 0:
                    messagebox.showwarning("Предупреждение", "Нет данных для сохранения")
                    return
                
                line = lines[0]
                voltages = line.get_xdata()
                capacitances = line.get_ydata()
                
                Nd0_actual = 10 ** self.Nd0Var.get()
                
                with open(filename, 'w', encoding='utf-8') as f:
                    f.write("# Вольт-фарадная характеристика МДП-структуры\n")
                    f.write("# Напряжение, В    Ёмкость, пФ\n")
                    f.write("# Полупроводник: " + self.semiconductorVar.get() + "\n")
                    f.write(f"# Запрещенная зона = {self.EgVar.get()} эВ\n")
                    f.write(f"# Диэлектрическая проницаемость = {self.epsilonVar.get()}\n")
                    f.write(f"# Эфф. масса дырок = {self.md_hVar.get()}\n")
                    f.write(f"# Эфф. масса электронов = {self.md_eVar.get()}\n")
                    f.write(f"# Уровень донора = {self.EdVar.get()} эВ\n")
                    f.write(f"# Концентрация доноров = {Nd0_actual:.2e} см⁻³\n")
                    f.write(f"# Температура = {self.TVar.get()} К\n")
                    f.write(f"# Толщина диэлектрика = {self.dVar.get()} нм\n")
                    f.write(f"# Площадь контакта = {self.SVar.get()} мм²\n")
                    f.write(f"# Диэлектрическая проницаемость диэлектрика = {self.epsilon_dVar.get()}\n")
                    f.write("#\n")
                    
                    for V, C_val in zip(voltages, capacitances):
                        f.write(f"{V:.3f}    {C_val:.6f}\n")
                
                messagebox.showinfo("Успех", f"Данные сохранены в файл:\n{filename}")
                
        except Exception as exception:
            messagebox.showerror("Ошибка", f"Не удалось сохранить данные:\n{str(exception)}")

def main():
    root = tk.Tk()
    app = MDPCapacitanceApp(root)
    root.mainloop()

if __name__ == "__main__":
    main()
