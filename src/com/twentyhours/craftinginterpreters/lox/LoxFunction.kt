package com.twentyhours.craftinginterpreters.lox

class LoxFunction(
    private val declaration: Function,
    private val closure: Environment,
    private val isInitializer: Boolean
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
            return if (isInitializer) {
                closure.getAt(0, "this")
            } else {
                returnValue.value
            }
        }

        if (isInitializer) {
            return closure.getAt(0, "this")
        }

        return null
    }

    override fun toString(): String = "<fn ${declaration.name.lexeme}>"

    fun bind(instance: LoxInstance): LoxFunction {
        val environment = Environment(closure)
        environment.define("this", instance)
        return LoxFunction(declaration, environment, isInitializer)
    }
}