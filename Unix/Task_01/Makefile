CC=gcc
CFLAGS=-c -Wall
SOURCES=main.c
OBJECTS=$(SOURCES:.c=.o)
all: main

main: main.o
	$(CC) $(LDFLAGS) main.o -o $@

.c.o: $(SOURCES)
	$(CC) $(CFLAGS) $< -o $@
