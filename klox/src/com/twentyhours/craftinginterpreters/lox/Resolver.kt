package com.twentyhours.craftinginterpreters.lox

import java.util.*
import kotlin.collections.HashMap

class Resolver(private val interpreter: Interpreter) : Expr.Visitor<Unit>, Stmt.Visitor<Unit> {
    private enum class FunctionType { NONE, FUNCTION, INITIALIZER, METHOD }
    private enum class ClassType { NONE, CLASS, SUBCLASS }

    private val scopes = Stack<HashMap<String, Boolean>>()
    private var currentFunction = FunctionType.NONE
    private var currentClass = ClassType.NONE

    fun resolve(statements: List<Stmt>) {
        statements.forEach {
            resolve(it)
        }
    }

    private fun resolve(stmt: Stmt) {
        stmt.accept(this)
    }

    private fun resolve(expr: Expr) {
        expr.accept(this)
    }

    private fun beginScope() {
        scopes.push(HashMap())
    }

    private fun endScope() {
        scopes.pop()
    }

    private fun declare(name: Token) {
        if (scopes.isEmpty()) {
            return
        }

        val scope = scopes.peek()
        if (scope.containsKey(name.lexeme)) {
            Lox.error(name,
                "Variable with this name already declared in this scope.")
        }
        scope[name.lexeme] = false
    }

    private fun define(name: Token) {
        if (scopes.isEmpty()) {
            return
        }

        scopes.peek()[name.lexeme] = true
    }

    private fun resolveLocal(expr: Expr, name: Token) {
        for (i in (scopes.size-1) downTo 0) {
            if (scopes[i].containsKey(name.lexeme)) {
                interpreter.resolve(expr, scopes.size - 1 - i)
                return
            }
        }
    }

    private fun resolveFunction(function: Function, type: FunctionType) {
        val enclosingFunction = currentFunction
        currentFunction = type

        beginScope()
        function.params.forEach {
            declare(it)
            define(it)
        }
        resolve(function.body)
        endScope()
        currentFunction = enclosingFunction
    }

    override fun visitAssignExpr(expr: Assign) {
        resolve(expr.value)
        resolveLocal(expr, expr.name)
    }

    override fun visitBinaryExpr(expr: Binary) {
        resolve(expr.left)
        resolve(expr.right)
    }

    override fun visitCallExpr(expr: Call) {
        resolve(expr.callee)
        expr.arguments.forEach {
            resolve(it)
        }
    }

    override fun visitGetExpr(expr: Get) {
        resolve(expr.obj)
    }

    override fun visitGroupingExpr(expr: Grouping) {
        resolve(expr.expression)
    }

    override fun visitLiteralExpr(expr: Literal) {
    }

    override fun visitLogicalExpr(expr: Logical) {
        resolve(expr.left)
        resolve(expr.right)
    }

    override fun visitSetExpr(expr: Set) {
        resolve(expr.value)
        resolve(expr.obj)
    }

    override fun visitSuperExpr(expr: Super) {
        if (currentClass == ClassType.NONE) {
            Lox.error(expr.keyword, "Cannot use 'super' outside of a class.")
        } else if (currentClass != ClassType.SUBCLASS) {
            Lox.error(expr.keyword, "Cannot use 'super' in a class with no superclass.")
        }

        resolveLocal(expr, expr.keyword)
    }

    override fun visitThisExpr(expr: This) {
        if (currentClass == ClassType.NONE) {
            Lox.error(expr.keyword, "Cannot use 'this' outside of a class.")
            return
        }
        resolveLocal(expr, expr.keyword)
    }

    override fun visitUnaryExpr(expr: Unary) {
        resolve(expr.right)
    }

    override fun visitVariableExpr(expr: Variable) {
        if (!scopes.isEmpty() &&
                scopes.peek()[expr.name.lexeme] == false) {
            Lox.error(expr.name,
                "Cannot read local variable in its own initializer.")
        }

        resolveLocal(expr, expr.name)
    }

    override fun visitBlockStmt(stmt: Block) {
        beginScope()
        resolve(stmt.statements)
        endScope()
    }

    override fun visitClassStmt(stmt: Class) {
        val enclosingClass = currentClass
        currentClass = ClassType.CLASS

        declare(stmt.name)
        define(stmt.name)

        if (stmt.superclass != null &&
                stmt.name.lexeme == stmt.superclass.name.lexeme) {
            Lox.error(stmt.superclass.name, "A class cannot inherit from itself.")
        }

        stmt.superclass?.let {
            currentClass = ClassType.SUBCLASS
            resolve(it)

            beginScope()
            scopes.peek()["super"] = true
        }

        beginScope()
        scopes.peek()["this"] = true

        stmt.methods.forEach {
            val declaration = if (it.name.lexeme == "init") {
                FunctionType.INITIALIZER
            } else {
                FunctionType.METHOD
            }
            resolveFunction(it, declaration)
        }

        endScope()

        stmt.superclass?.let {
            endScope()
        }

        currentClass = enclosingClass
    }

    override fun visitExpressionStmt(stmt: Expression) {
        resolve(stmt.expression)
    }

    override fun visitFunctionStmt(stmt: Function) {
        declare(stmt.name)
        define(stmt.name)

        resolveFunction(stmt, FunctionType.FUNCTION)
    }

    override fun visitIfStmt(stmt: If) {
        resolve(stmt.condition)
        resolve(stmt.thenBranch)
        stmt.elseBranch?.let {
            resolve(it)
        }
    }

    override fun visitPrintStmt(stmt: Print) {
        resolve(stmt.expression)
    }

    override fun visitReturnStmt(stmt: Return) {
        if (currentFunction == FunctionType.NONE) {
            Lox.error(stmt.keyword, "Cannot return from top-level code.")
        }

        stmt.value?.let {
            if (currentFunction == FunctionType.INITIALIZER) {
                Lox.error(stmt.keyword, "Cannot return a value from an initializer.")
            }
            resolve(it)
        }
    }

    override fun visitVarStmt(stmt: Var) {
        declare(stmt.name)
        stmt.initializer?.let {
            resolve(it)
        }
        define(stmt.name)
    }

    override fun visitWhileStmt(stmt: While) {
        resolve(stmt.condition)
        resolve(stmt.body)
    }
}