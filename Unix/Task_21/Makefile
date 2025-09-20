CC=gcc
CFLAGS=-c -Wall
SOURCES=main.c
OBJECTS=$(SOURCES:.c=.o)

all: partA partB partC module widget

partA: partA.o
	$(CC) $(LDFLAGS) partA.o -o $@

partB: partB.o
	$(CC) $(LDFLAGS) partB.o -o $@

partC: partC.o
	$(CC) $(LDFLAGS) partC.o -o $@

module: module.o
	$(CC) $(LDFLAGS) module.o -o $@

widget: widget.o
	$(CC) $(LDFLAGS) widget.o -o $@

clean:
	rm partA partA.o partB partB.o partC partC.o widget widget.o module module.o

.c.o: $(SOURCES)
	$(CC) $(CFLAGS) $< -o $@
