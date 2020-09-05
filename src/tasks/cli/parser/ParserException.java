package tasks.cli.parser;

public final class ParserException extends RuntimeException {

  public ParserException(String reason) {
    super(reason);
  }

  public ParserException(String reason, Throwable cause) {
    super(reason, cause);
  }
}
