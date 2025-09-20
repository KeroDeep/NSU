CC=gcc
CFLAGS=-c -Wall
SOURCES=main.c
OBJECTS=$(SOURCES:.c=.o)

all: send listen

send: send.o
	$(CC) $(LDFLAGS) send.o -o $@

listen: listen.o
	$(CC) $(LDFLAGS) listen.o -o $@

.c.o: $(SOURCES)
	$(CC) $(CFLAGS) $< -o $@
