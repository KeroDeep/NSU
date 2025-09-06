#include <iostream>
#include <cmath>

using namespace std;

double find_machine_epsilon_double() {
    double epsilon = 1.0;
    while (1.0 + epsilon / 2.0 != 1.0) {
        epsilon /= 2.0;
    }
    return epsilon;
}

/* 
У нас есть начальное уравнение Шрёдингера:
-(1 / 2) * (d^2(Psi) / dx^2) + U(x) * Psi = E * Psi
и прямоугольный потенциал:
U(x) = -U0, при |x| <= a
U(x) = 0, при |x| > a
То есть картинка выглядит примерно так:
           -a          | 0         a             
___________.___________.___________.____________\
           |           |           |            /
           |           |           |             
           |___________|___________|             
                       | -U0                     
                       |                         
При |x| <= a, то есть вне ямы, U(x) = 0 =>
уравнение Шрёдингера принимает вид:
-(1 / 2) * (d^2(Psi) / dx^2) = E * Psi =>
d^2(Psi) / dx^2 = -2 * E * Psi
Обозначим: n^2 = -2 * E
Получим: d^2(Psi) / dx^2 = n^2 * Psi
Решение: Psi(x) = A * exp(-n * x) + B * exp(n * x)
Решение вне ямы должно затухать. Проверим:
* Для x > a:
x -> inf => exp(-n * x) -> 0 - подходит
x -> inf => exp(n * x) -> inf - не подходит
Значит второе слагаемое не подходит для x > a,
поэтому решение для этой части:
Psi(x) = A * exp(-n * x)
* Для x < -a:
x -> -inf => exp(-n * x) -> inf - не подходит
x -> -inf => exp(n * x) -> 0 - подходит
Значит первое слагаемое не подходит для x < -a,
поэтому решение для этой части:
Psi(x) = A * exp(n * x)
При |x| > a, то есть в яме, U(x) = -U0 =>
уравнение Шрёдингера принимает вид:
-(1 / 2) * (d^2(Psi) / dx^2) - U0 * Psi = E * Psi =>
-(1 / 2) * (d^2(Psi) / dx^2) = U0 + * Psi + E * Psi =>
d^2(Psi) / dx^2 = -2 * (U0 + E) * Psi
Обозначим: m^2 = -2 * (U0 + E)
Так как U₀ > 0 и E > -U₀, то: m^2 = 2 * (U0 + E)
Решение: Psi(x) = A * sin(m * x) + B * cos(m * x)
Но так как мы ищем основное состояние то нечётная
часть решения нам не подходит, поэтому решение:
Psi(x) = B * cos(m * x)
Общая картина решений:
       I   -a    II    | 0   II    a    III      
___________.___________.___________.____________\
           |           |           |            /
  exp(nx)  |  cos(mx)  |  cos(mx)  |  exp(-nx)   
           |___________|___________|             
                       | -U0                     
                       |                         
Теперь нужно сшить решения внутри и вне ямы на
границах x = a и x = -a. Условия сшивки:
1) Равны сами функции
2) Равны их производные
* На границе I и II:
1) A * exp(-n * -a) = B * cos(m * -a) =>
A * exp(n * a) = B * cos(m * a)
2) n * A * exp(n * a) = -m * B * sin(m * -a)
Делим 2) на 1) и получим:
n = -m * tg(m * -a) => n = m * tg(m * a)
* На границе II и III:
1) A * exp(-n * a) = B * cos(m * a)
2) -n * A * exp(-n * a) = -m * B * sin(m * a)
Делим 2) на 1) и получим:
-n = -m * tg(m * a) => n = m * tg(m * a)
Получили уравнение: m * tg(m * a) - n = 0, где
n^2 = -2 * E и m^2 = -2 * (U0 + E)
Или в виде:
m * sin(m * a) - n * cos(m * a) = 0
Это уравнение вида: f(E) = 0
Теперь для его решения можно использовать
численные методы, чтобы найти E.
*/

double energy_equation(double energy, double width, double height) {
    if (energy >= 0 || energy <= -height) {
        return NAN;
    }

    if (2.0 * (height + energy) < 0 || -2.0 * energy < 0) {
        return NAN;
    }

    double m = sqrt(2.0 * (height + energy));
    double n = sqrt(-2.0 * energy);

    return m * sin(m * width) - n * cos(m * width);
}

const double tolerance = 10 * find_machine_epsilon_double();
const int max_iterations = 1000;

double dichotomy_method(double width, double height) {
    double point_left = -height + 1000 * tolerance;
    double point_right = -1000 * tolerance;
    
    double function_left = energy_equation(point_left, width, height);
    double function_right = energy_equation(point_right, width, height);
    
    if (isnan(function_left) || isnan(function_right) || function_left * function_right >= 0) {
        cout << endl << "Invalid interval for dichotomy method" << endl;
        return NAN;
    }

    double point_middle;
    for (int i = 0; i < max_iterations; i++) {
        point_middle = (point_left + point_right) / 2.0;
        double function_middle = energy_equation(point_middle, width, height);

        if (isnan(function_middle)) {
            cout << endl << "Function undefined in dichotomy method" << endl;
            return NAN;
        }

        if (abs(function_middle) < tolerance) {
            break;
        }

        if (function_left * function_middle < 0) {
            point_right = point_middle;
            function_right = function_middle;
        } else {
            point_left = point_middle;
            function_left = function_middle;
        }
    }

    return point_middle;
}

double iteration_method(double width, double height) {
    double point_current = -height / 2.0;
    double relaxation = tolerance * 1000;
    
    for (int i = 0; i < max_iterations; i++) {
        double function_current = energy_equation(point_current, width, height);
        if (isnan(function_current)) {
            cout << endl << "Function undefined in iteration method" << endl;
            return NAN;
        }
        
        double point_next = point_current - relaxation * function_current;
        
        if (point_next >= 0) point_next = -tolerance;
        if (point_next <= -height) point_next = -height + tolerance;
        
        if (abs(point_next - point_current) < tolerance) {
            return point_next;
        }
        
        point_current = point_next;
    }
    
    cout << endl << "Iteration method did not converge" << endl;
    return point_current;
}

double newton_method(double width, double height) {
    double point_current = -height / 2.0;
    const double step = sqrt(tolerance);
    
    for (int i = 0; i < max_iterations; i++) {
        double function_current = energy_equation(point_current, width, height);
        if (isnan(function_current)) {
            cout << endl << "Function undefined in Newton method" << endl;
            return NAN;
        }
        
        double function_plus = energy_equation(point_current + step, width, height);
        double function_minus = energy_equation(point_current - step, width, height);
        
        if (isnan(function_plus) || isnan(function_minus)) {
            cout << endl << "Derivative calculation failed in Newton method" << endl;
            return NAN;
        }
        
        double derivative = (function_plus - function_minus) / (2.0 * step);
        
        if (abs(derivative) < tolerance) {
            cout << endl << "Small derivative in Newton method" << endl;
            break;
        }
        
        double point_next = point_current - function_current / derivative;
        
        if (point_next >= 0) point_next = -tolerance;
        if (point_next <= -height) point_next = -height + tolerance;
        
        if (abs(point_next - point_current) < tolerance) {
            return point_next;
        }
        
        point_current = point_next;
    }
    
    cout << endl << "Newton method did not converge" << endl;
    return point_current;
}

double muller_method(double width, double height) {
    double x_0 = -height * 0.9;
    double x_1 = -height * 0.6; 
    double x_2 = -height * 0.3;
    
    for (int i = 0; i < max_iterations; i++) {
        double function_0 = energy_equation(x_0, width, height);
        double function_1 = energy_equation(x_1, width, height);
        double function_2 = energy_equation(x_2, width, height);
        
        if (isnan(function_0) || isnan(function_1) || isnan(function_2)) {
            cout << endl << "Function undefined in Muller method" << endl;
            return NAN;
        }
        
        double step_0 = x_1 - x_0;
        double step_1 = x_2 - x_1;
        double delta_0 = (function_1 - function_0) / step_0;
        double delta_1 = (function_2 - function_1) / step_1;
        
        double a = (delta_1 - delta_0) / (step_1 + step_0);
        double b = a * step_1 + delta_1;
        double c = function_2;
        
        double discriminant = b * b - 4.0 * a * c;
        if (discriminant < 0) {
            cout << endl << "Complex roots in Muller method" << endl;
            return NAN;
        }
        discriminant = sqrt(discriminant);
        
        double denominator = (abs(b + discriminant) > abs(b - discriminant)) ? (b + discriminant) : (b - discriminant);
        if (abs(denominator) < tolerance) {
            cout << endl << "Small denominator in Muller method" << endl;
            break;
        }
        
        double delta_x = -2.0 * c / denominator;
        double x_3 = x_2 + delta_x;
        
        if (x_3 >= 0) x_3 = -tolerance;
        if (x_3 <= -height) x_3 = -height + tolerance;
        
        if (abs(delta_x) < tolerance) {
            return x_3;
        }
        
        x_0 = x_1;
        x_1 = x_2;
        x_2 = x_3;
    }
    
    cout << endl << "Muller method did not converge" << endl;
    return x_2;
}

double stephenson_method(double width, double height) {
    double point_current = -height / 2.0;
    const double step = sqrt(tolerance);
    
    for (int i = 0; i < max_iterations; i++) {
        double function_current = energy_equation(point_current, width, height);
        if (isnan(function_current)) {
            cout << endl << "Function undefined in Stephenson method" << endl;
            return NAN;
        }
        
        double function_plus = energy_equation(point_current + step, width, height);
        double function_minus = energy_equation(point_current - step, width, height);
        
        if (isnan(function_plus) || isnan(function_minus)) {
            cout << endl << "Derivative calculation failed in Stephenson method" << endl;
            return NAN;
        }
        
        double derivative = (function_plus - function_minus) / (2.0 * step);
        
        if (abs(derivative) < tolerance) {
            cout << endl << "Small derivative in Stephenson method" << endl;
            break;
        }
        
        double second_derivative = (function_plus - 2.0 * function_current + function_minus) / (step * step);
        
        double denominator = derivative - (function_current * second_derivative) / (2.0 * derivative);
        
        if (abs(denominator) < tolerance) {
            cout << endl << "Small denominator in Stephenson method" << endl;
            break;
        }
        
        double point_next = point_current - function_current / denominator;
        
        if (point_next >= 0) point_next = -tolerance;
        if (point_next <= -height) point_next = -height + tolerance;
        
        if (abs(point_next - point_current) < tolerance) {
            return point_next;
        }
        
        point_current = point_next;
    }
    
    cout << endl << "Stephenson method did not converge" << endl;
    return point_current;
}

int main() {
    double width;
    double height;

    cout << endl;

    cout << "Enter hole width: ";
    cin >> width;
    cout << endl;
    cout << "Enter hole height: ";
    cin >> height;

    cout << endl;

    cout << "Dichotomy method (bisection): " << dichotomy_method(width, height) << endl;
    cout << endl;
    cout << "Simple iteration method: " << iteration_method(width, height) << endl;
    cout << endl;
    cout << "Newton method: " << newton_method(width, height) << endl;
    cout << endl;
    cout << "Muller method: " << muller_method(width, height) << endl;
    cout << endl;
    cout << "Stephenson method: " << stephenson_method(width, height) << endl;

    cout << endl;
    
    return 0;
}
