package com.twentyhours.craftinginterpreters.lox

import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess

class Lox {
    companion object {
        private var hadError = false

        @JvmStatic
        fun main(args: Array<String>) {
            when {
                args.size > 1 -> {
                    println("Usage: jlox [script]")
                    exitProcess(64)
                }
                args.size == 1 -> runFile(args[0])
                else -> runPrompt()
            }
        }

        private fun runFile(path: String) {
            val bytes = Files.readAllBytes(Paths.get(path))
            run(String(bytes, Charset.defaultCharset()))

            if (hadError) {
                exitProcess(65)
            }
        }

        private fun runPrompt() {
            val input = InputStreamReader(System.`in`)
            val reader = BufferedReader(input)
            while (true) {
                print("> ")
                run(reader.readLine())
                hadError = false
            }
        }

        private fun run(source: String) {
            val scanner = Scanner(source)
            val tokens = scanner.scanTokens()
            val expression = Parser(tokens).parse()
            if (hadError) {
                return
            }

            println(AstPrinter().print(expression!!))
        }

        /**
         * TODO: 에러 메시지 좀 더 자세하게 만들어 보기
         *       ex) 에러의 시작지점과 끝 지점 표시하기
         */
        fun error(line: Int, message: String) {
            report(line, "", message)
        }

        fun error(token: Token, message: String) {
            if (token.type == TokenType.EOF) {
                report(token.line, " at end", message)
            } else {
                report(token.line, " at '${token.lexeme}'", message)
            }
        }

        private fun report(line: Int, where: String, message: String) {
            println("[line $line] Error$where: $message")
            hadError = true
        }
    }
}