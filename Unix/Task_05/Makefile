CC=gcc
CFLAGS=-c -Wall
SOURCES=main.c
OBJECTS=$(SOURCES:.c=.o)

all: main

main: $(OBJECTS)
	$(CC) $(LDFLAGS) $(OBJECTS) -o $@

.c.o: $(SOURCES)
	$(CC) $(CFLAGS) $< -o $@

clean:
	rm -rf *.o main
