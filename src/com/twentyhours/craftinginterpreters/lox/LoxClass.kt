package com.twentyhours.craftinginterpreters.lox

class LoxClass(
    val name: String,
    val superclass: LoxClass?,
    private val methods: HashMap<String, LoxFunction>
) : LoxCallable {
    override fun arity(): Int {
        return findMethod("init")?.arity() ?: return 0
    }

    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
        val instance = LoxInstance(this)
        findMethod("init")?.bind(instance)?.call(interpreter, arguments)
        return instance
    }

    override fun toString(): String = name

    fun findMethod(name: String): LoxFunction? {
        if (methods.containsKey(name)) {
            return methods[name]!!
        }

        if (superclass != null) {
            return superclass.findMethod(name)
        }

        return null
    }
}