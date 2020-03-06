CC = gcc
CFLAGS = -W -Wall
TARGET = clox
OBJECTS = main.o chunk.o memory.o debug.o value.o vm.o

all : $(TARGET)

$(TARGET) : $(OBJECTS)
	$(CC) $(CFLAGS) -o $@ $^

clean :
	rm *.o $(TARGET)

