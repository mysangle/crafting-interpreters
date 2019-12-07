package com.twentyhours.craftinginterpreters.tool

import java.io.IOException
import java.io.PrintWriter
import kotlin.system.exitProcess

class GenerateAstKt {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            if (args.size != 1) {
                println("Usage: generate_ast <output directory>")
                exitProcess(1)
            }

            defineAst(
                args[0], "Expr", listOf(
                    "Assign   - val name: Token, val value: Expr",
                    "Binary   - val left: Expr, val operator: Token, val right: Expr",
                    "Call     - val callee: Expr, val paren: Token, val arguments: List<Expr>",
                    "Get      - val obj: Expr, val name: Token",
                    "Grouping - val expression: Expr",
                    "Literal  - val value: Any?",
                    "Logical  - val left: Expr, val operator: Token, val right: Expr",
                    "Set      - val obj: Expr, val name: Token, val value: Expr",
                    "This     - val keyword: Token",
                    "Unary    - val operator: Token, val right: Expr",
                    "Variable - val name: Token"
                )
            )

            defineAst(
                args[0], "Stmt", listOf(
                    "Block      - val statements: List<Stmt>",
                    "Class      - val name: Token, val methods: List<Function>",
                    "Expression - val expression: Expr",
                    "Function   - val name: Token, val params: List<Token>, val body: List<Stmt>",
                    "If         - val condition: Expr, val thenBranch: Stmt, val elseBranch: Stmt?",
                    "Print      - val expression: Expr",
                    "Return     - val keyword: Token, val value: Expr?",
                    "Var        - val name: Token, val initializer: Expr?",
                    "While      - val condition: Expr, val body: Stmt"
                )
            )
        }

        @Throws(IOException::class)
        private fun defineAst(
            outputDir: String, baseName: String, types: List<String>
        ) {
            val path = "$outputDir/$baseName.kt"
            val writer = PrintWriter(path, "UTF-8")

            writer.println("package com.twentyhours.craftinginterpreters.lox")
            writer.println()
            writer.println("abstract class $baseName {")

            defineVisitor(writer, baseName, types)

            // The base accept() method.
            writer.println("    abstract fun <R> accept(visitor: Visitor<R>): R")

            writer.println("}")

            // The AST classes.
            for (type in types) {
                val className = type.split("-")[0].trim()
                val fields = type.split("-")[1].trim()
                defineType(writer, baseName, className, fields)
            }

            writer.close()
        }

        private fun defineVisitor(writer: PrintWriter, baseName: String, types: List<String>) {
            writer.println("    interface Visitor<R> {")

            for (type in types) {
                val typeName = type.split("-")[0].trim()
                writer.println("        fun visit$typeName$baseName(${baseName.toLowerCase()}: $typeName): R")
            }

            writer.println("    }")
        }

        private fun defineType(
            writer: PrintWriter, baseName: String, className: String, fieldList: String
        ) {
            writer.println()
            writer.println("class $className($fieldList) : $baseName() {")

            writer.println("    override fun <R> accept(visitor: Visitor<R>): R {")
            writer.println("        return visitor.visit$className$baseName(this)")
            writer.println("    }")

            writer.println("}")
        }
    }
}