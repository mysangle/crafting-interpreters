package com.twentyhours.craftinginterpreters.lox

class LoxFunction(
    private val declaration: Function,
    private val closure: Environment
) : LoxCallable {
    override fun arity(): Int = declaration.params.size

    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
        val environment = Environment(closure)

        declaration.params.forEachIndexed { index, token ->
            environment.define(token.lexeme, arguments[index])
        }

        try {
            interpreter.executeBlock(declaration.body, environment)
        } catch (returnValue: LoxReturn) {
            return returnValue.value
        }

        return null
    }

    override fun toString(): String = "<fn ${declaration.name.lexeme}>"
}