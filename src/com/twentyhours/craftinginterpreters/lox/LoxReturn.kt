package com.twentyhours.craftinginterpreters.lox

class LoxReturn(val value: Any?) : RuntimeException(null, null, false, false) {
}