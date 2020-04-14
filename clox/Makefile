CC = gcc
CFLAGS = -W -Wall
TARGET = clox
OBJECTS = main.o chunk.o memory.o debug.o value.o vm.o compiler.o scanner.o object.o table.o

all : $(TARGET)

$(TARGET) : $(OBJECTS)
	$(CC) $(CFLAGS) -o $@ $^

clean :
	rm *.o $(TARGET)

