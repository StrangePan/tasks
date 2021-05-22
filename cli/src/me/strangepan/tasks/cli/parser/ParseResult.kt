package me.strangepan.tasks.cli.parser

/**
 * A simple object representing the result of parsing a value. Composes the value itself, or a
 * failure message if the parsing failed.
 */
class ParseResult<T : Any> private constructor(val successResult: T?, val failureMessage: String?) {

  companion object {
    fun <T : Any> success(result: T): ParseResult<T> {
      return ParseResult(result, null)
    }

    fun <T : Any> failure(message: String): ParseResult<T> {
      return ParseResult(null, message)
    }
  }

}