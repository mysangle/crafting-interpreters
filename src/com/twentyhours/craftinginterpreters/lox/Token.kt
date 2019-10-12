package com.twentyhours.craftinginterpreters.lox

/**
 * 예)
 * "lox"
 *   lexeme: "lox"
 *   literal: lox
 * 1234
 *   lexeme: 1234
 *   literal: 1234.0
 *
 * 토큰이 orchid인 경우: 토큰 분석시 or을 먼저 만난다고 해서 or이라는 토큰을 만들지 않는다.
 *   -> maximal munch
 */
data class Token(
    val type: TokenType,
    val lexeme: String, // token string
    val literal: Any?, // token value
    val line: Int // 토큰이 위치한 라인
) {
}