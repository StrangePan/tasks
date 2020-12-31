package tasks.cli.parser;

import static java.util.Objects.requireNonNull;

import java.util.Optional;

/**
 * A simple object representing the result of parsing a value. Composes the value itself, or a
 * failure message if the parsing failed.
 */
public final class ParseResult<T> {
  private final Optional<T> successResult;
  private final Optional<String> failureMessage;

  public static <T> ParseResult<T> success(T result) {
    return new ParseResult<>(Optional.of(result), Optional.empty());
  }

  public static <T> ParseResult<T> failure(String message) {
    return new ParseResult<>(Optional.empty(), Optional.of(message));
  }

  private ParseResult(Optional<T> successResult, Optional<String> failureMessage) {
    this.successResult = requireNonNull(successResult);
    this.failureMessage = requireNonNull(failureMessage);
  }

  public Optional<T> successResult() {
    return successResult;
  }

  public Optional<String> failureMessage() {
    return failureMessage;
  }
}
