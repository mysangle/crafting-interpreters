package com.twentyhours.craftinginterpreters.lox

class AstPrinter : Expr.Visitor<String> {
    fun print(expr: Expr): String {
        return expr.accept(this)
    }

    override fun visitBinaryExpr(expr: Binary): String {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right)
    }

    override fun visitGroupingExpr(expr: Grouping): String {
        return parenthesize("group", expr.expression)
    }

    override fun visitLiteralExpr(expr: Literal): String {
        if (expr.value == null) {
            return "nil"
        }
        return expr.value.toString()
    }

    override fun visitUnaryExpr(expr: Unary): String {
        return parenthesize(expr.operator.lexeme, expr.right)
    }

    private fun parenthesize(name: String, vararg exprs: Expr): String {
        val builder = StringBuilder()

        builder.append("(").append(name)
        for (expr in exprs) {
            builder.append(" ")
            builder.append(expr.accept(this))
        }
        builder.append(")")

        return builder.toString()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val expression = Binary(
                Unary(
                    Token(TokenType.MINUS, "-", null, 1),
                    Literal(123)
                ),
                Token(TokenType.STAR, "*", null, 1),
                Grouping(
                    Literal(45.67)
                )
            )

            println(AstPrinter().print(expression))
        }
    }
}