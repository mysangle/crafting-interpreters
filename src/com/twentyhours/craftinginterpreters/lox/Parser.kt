package com.twentyhours.craftinginterpreters.lox

import java.lang.RuntimeException

/**
 * 토큰의 리스트를 받아서 처리
 */
class Parser(private val tokens: List<Token>) {
    class ParseError : RuntimeException()

    private var current = 0

    fun parse(): List<Stmt> {
        val statements = mutableListOf<Stmt>()
        while (!isAtEnd()) {
            declaration()?.let {
                statements.add(it)
            }
        }

        return statements
    }

    private fun expression(): Expr {
        return assignment()
    }

    private fun assignment(): Expr {
        val expr = or()

        if (match(TokenType.EQUAL)) {
            val equals = previous()
            val value = assignment()

            if (expr is Variable) {
                val name = expr.name
                return Assign(name, value)
            }

            error(equals, "Invalid assignment target.")
        }

        return expr
    }

    private fun or(): Expr {
        var expr = and()

        while (match(TokenType.OR)) {
            val operator = previous()
            val right = and()
            expr = Logical(expr, operator, right)
        }

        return expr
    }

    private fun and(): Expr {
        var expr = equality()

        while (match(TokenType.AND)) {
            val operator = previous()
            val right = equality()
            expr = Logical(expr, operator, right)
        }

        return expr
    }

    private fun declaration(): Stmt? {
        return try {
            when {
                match(TokenType.FUN) -> {
                    function("function")
                }
                match(TokenType.VAR) -> {
                    varDeclaration()
                }
                else -> {
                    statement()
                }
            }
        } catch (error: ParseError) {
            synchronize()
            null
        }
    }

    private fun statement(): Stmt {
        if (match(TokenType.FOR)) {
            return forStatement()
        }
        if (match(TokenType.IF)) {
            return ifStatement()
        }
        if (match(TokenType.PRINT)) {
            return printStatement()
        }
        if (match(TokenType.RETURN)) {
            return returnStatement()
        }
        if (match(TokenType.WHILE)) {
            return whileStatement()
        }
        if (match(TokenType.LEFT_BRACE)) {
            return Block(block())
        }

        return expressionStatement()
    }

    /**
     * translate'for' to 'while' - syntactic sugar
     * : Block(initializer, While(condition, Block(body, increment))
     *
     * for (var i = 0; i < 10; i = i + 1) {
     *   print i;
     * }
     * =>
     * {
     *   var i = 0; // initializer
     *   while (i < 10) {
     *     print i;
     *     i = i + 1;  // increment
     *   }
     * }
     *
     */
    private fun forStatement(): Stmt {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'for'.")

        val initializer = when {
            match(TokenType.SEMICOLON) -> null
            match(TokenType.VAR) -> varDeclaration()
            else -> expressionStatement()
        }

        val condition = if (!check(TokenType.SEMICOLON)) {
            expression()
        } else {
            null
        }
        consume(TokenType.SEMICOLON, "Expect ';' after loop condition.")

        val increment = if (!check(TokenType.RIGHT_PAREN)) {
            expression()
        } else {
            null
        }
        consume(TokenType.RIGHT_PAREN, "Expect ')' after for clauses.")

        var body = if (increment != null) {
            Block(listOf(statement(), Expression(increment)))
        } else {
            statement()
        }

        body = if (condition == null) {
            While(Literal(true), body)
        } else {
            While(condition, body)
        }

        return if (initializer != null) {
            Block(listOf(initializer, body))
        } else {
            body
        }
    }

    private fun ifStatement(): Stmt {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'if'.")
        val condition = expression()
        consume(TokenType.RIGHT_PAREN, "Expect ')' after if condition.")

        val thenBranch = statement()
        val elseBranch = if (match(TokenType.ELSE)) {
            statement()
        } else {
            null
        }

        return If(condition, thenBranch, elseBranch)
    }

    private fun printStatement(): Stmt {
        val value = expression()
        consume(TokenType.SEMICOLON, "Expect ';' after value.")
        return Print(value)
    }

    private fun returnStatement(): Stmt {
        val keyword = previous()
        val value = if (!check(TokenType.SEMICOLON)) {
            expression()
        } else {
            null
        }

        consume(TokenType.SEMICOLON, "Expect ';' after return value.")
        return Return(keyword, value)
    }

    private fun whileStatement(): Stmt {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'while'.")
        val condition = expression()
        consume(TokenType.RIGHT_PAREN, "Expect ')' after condition.")
        val body = statement()

        return While(condition, body)
    }

    private fun varDeclaration(): Stmt {
        val name = consume(TokenType.IDENTIFIER, "Expect variable name.")

        val initializer: Expr? = if (match(TokenType.EQUAL)) {
            expression()
        } else {
            null
        }

        consume(TokenType.SEMICOLON, "Expect ';' after variable declaration.")
        return Var(name, initializer)
    }

    private fun expressionStatement(): Stmt {
        val expr = expression()
        consume(TokenType.SEMICOLON, "Expect ';' after expression.")
        return Expression(expr)
    }

    private fun function(kind: String): Function {
        val name = consume(TokenType.IDENTIFIER, "Expect $kind name.")
        consume(TokenType.LEFT_PAREN, "Expect '(' after $kind name.")
        val parameters = mutableListOf<Token>()
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                if (parameters.size >= 255) {
                    error(peek(), "Cannot have more than 255 parameters.")
                }

                parameters.add(consume(TokenType.IDENTIFIER, "Expect parameter name."))
            } while (match(TokenType.COMMA))
        }
        consume(TokenType.RIGHT_PAREN, "Expect ')' after parameters.")

        consume(TokenType.LEFT_BRACE, "Expect '{' before $kind body.")
        val body = block()
        return Function(name, parameters, body)
    }

    private fun block(): List<Stmt> {
        val statements = mutableListOf<Stmt>()

        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            declaration()?.let {
                statements.add(it)
            }
        }

        consume(TokenType.RIGHT_BRACE, "Expect '}' after block.")
        return statements
    }

    private fun equality(): Expr {
        var expr = comparison()

        while (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
            val operator = previous()
            val right = comparison()
            expr = Binary(expr, operator, right)
        }

        return expr
    }

    private fun comparison(): Expr {
        var expr = addition()

        while (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
            val operator = previous()
            val right = addition()
            expr = Binary(expr, operator, right)
        }

        return expr
    }

    private fun addition(): Expr {
        var expr = multiplication()

        while (match(TokenType.MINUS, TokenType.PLUS)) {
            val operator = previous()
            val right = multiplication()
            expr = Binary(expr, operator, right)
        }

        return expr
    }

    private fun multiplication(): Expr {
        var expr = unary()

        while (match(TokenType.SLASH, TokenType.STAR)) {
            val operator = previous()
            val right = unary()
            expr = Binary(expr, operator, right)
        }

        return expr
    }

    private fun unary(): Expr {
        return if (match(TokenType.BANG, TokenType.MINUS)) {
            val operator = previous()
            val right = unary()
            Unary(operator, right)
        } else {
            call()
        }
    }

    private fun call(): Expr {
        var expr = primary()

        while (true) {
            if (match(TokenType.LEFT_PAREN)) {
                expr = finishCall(expr)
            } else {
                break
            }
        }

        return expr
    }

    private fun finishCall(callee: Expr): Expr {
        val arguments = mutableListOf<Expr>()
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                if (arguments.size >= 255) {
                    error(peek(), "Cannot have more than 255 arguments.")
                }
                arguments.add(expression())
            } while (match(TokenType.COMMA))
        }

        val paren = consume(TokenType.RIGHT_PAREN, "Expect ')' after arguments.")

        return Call(callee, paren, arguments)
    }

    @Throws(ParseError::class)
    private fun primary(): Expr {
        if (match(TokenType.FALSE)) {
            return Literal(false)
        }
        if (match(TokenType.TRUE)) {
            return Literal(true)
        }
        if (match(TokenType.NIL)) {
            return Literal(null)
        }
        if (match(TokenType.NUMBER, TokenType.STRING)) {
            return Literal(previous().literal)
        }
        if (match(TokenType.IDENTIFIER)) {
            return Variable(previous())
        }
        if (match(TokenType.LEFT_PAREN)) {
            val expr = expression()
            consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.")
            return Grouping(expr)
        }

        throw error(peek(), "Expect a expression")
    }

    private fun match(vararg types: TokenType): Boolean {
        for (type in types) {
            if (check(type)) {
                advance()
                return true
            }
        }

        return false
    }

    @Throws(ParseError::class)
    private fun consume(type: TokenType, message: String):Token {
        if (check(type)) {
            return advance()
        }

        throw error(peek(), message)
    }

    private fun check(type: TokenType) = if (isAtEnd()) {
        false
    } else {
        peek().type == type
    }

    private fun advance(): Token {
        if (!isAtEnd()) {
            current++
        }
        return previous()
    }

    private fun isAtEnd() = peek().type == TokenType.EOF

    private fun peek() = tokens[current]

    private fun previous() = tokens[current - 1]

    private fun error(token: Token, message: String): ParseError {
        Lox.error(token, message)
        return ParseError()
    }

    private fun synchronize() {
        advance()

        while (!isAtEnd()) {
            if (previous().type == TokenType.SEMICOLON) {
                return
            }

            when (peek().type) {
                TokenType.CLASS, TokenType.FUN, TokenType.VAR, TokenType.FOR,
                TokenType.IF, TokenType.WHILE, TokenType.PRINT, TokenType.RETURN -> return
                else -> {}
            }

            advance()
        }
    }
}