package me.strangepan.tasks.cli.parser

import java.util.Optional

/**
 * A simple object representing the result of parsing a value. Composes the value itself, or a
 * failure message if the parsing failed.
 */
class ParseResult<T : Any> private constructor(val successResult: Optional<T>, val failureMessage: Optional<String>) {

  companion object {
    fun <T : Any> success(result: T): ParseResult<T> {
      return ParseResult(Optional.of(result), Optional.empty())
    }

    fun <T : Any> failure(message: String): ParseResult<T> {
      return ParseResult(Optional.empty(), Optional.of(message))
    }
  }

}