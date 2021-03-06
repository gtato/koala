#!/usr/bin/gnuplot

if (!exists("filename")) filename='../out/results/results0.5.dat'


set title "Occurrences in paths"
set xlabel "Percentage of messages"
set ylabel 'Number of nodes'

set yrange [ 0.1 : * ]

set logscale y 10

 


datafile = filename


set style data histograms
set style fill solid 0.5
set bars front
set boxwidth 1 relative

set xtics rotate by -90 out

#plot datafile  using 1:2 notitle with points 
plot datafile using 2:xticlabels(1) linecolor rgb "dark-blue" notitle ,\
	 '' using 0:2:2 with labels offset char 0.5,0.5 notitle
pause mouse close