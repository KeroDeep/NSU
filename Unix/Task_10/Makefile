CC=gcc
CFLAGS=-c -Wall
SOURCES=main.c printer.c
OBJECTS=$(SOURCES:.c=.o)

all: main printer

main: main.o
	$(CC) $(LDFLAGS) main.o -o $@

printer: printer.o
	$(CC) $(LDFLAGS) printer.o -o $@

.c.o: $(SOURCES)
	$(CC) $(CFLAGS) $< -o $@ 
