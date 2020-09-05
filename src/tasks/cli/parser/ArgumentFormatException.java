package tasks.cli.parser;

public final class ArgumentFormatException extends RuntimeException {
  public ArgumentFormatException(String reason) {
    super(reason);
  }

  public ArgumentFormatException(String reason, Throwable cause) {
    super(reason, cause);
  }
}
