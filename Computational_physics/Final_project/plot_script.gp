set terminal pngcairo enhanced font 'Arial,12' size 800,600
set output 'phase_curve.png'
set title 'Phase trajectory |F_1| from |F_2| for different gamma'
set xlabel '|F_1|'
set ylabel '|F_2|'
set grid
set key outside right top
set xrange [0.0638045:0.0647248]
set yrange [0.0231584:0.0242815]
set format x '%.4f'
set format y '%.4f'

plot \
    'gamma_0.0.dat' with lines title 'γ = 0.0' lw 2, \
    'gamma_0.5.dat' with lines title 'γ = 0.5' lw 2, \
    'gamma_1.0.dat' with lines title 'γ = 1.0' lw 2, \
    'gamma_2.0.dat' with lines title 'γ = 2.0' lw 2, \
    'gamma_5.0.dat' with lines title 'γ = 5.0' lw 2, \
    'gamma_10.0.dat' with lines title 'γ = 10.0' lw 2
