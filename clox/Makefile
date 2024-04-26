CC = gcc
CFLAGS = -W -Wall -Wunused-parameter
TARGET = clox
OBJECTS = main.o chunk.o memory.o debug.o value.o vm.o compiler.o scanner.o object.o table.o

all : $(TARGET)

$(TARGET) : $(OBJECTS)
	$(CC) $(CFLAGS) -o $@ $^

memory.o: memory.c memory.h common.h

object.o: object.c object.h common.h

clean :
	rm *.o $(TARGET)

