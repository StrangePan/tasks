package tasks.cli.parser;

public final class ParserException extends RuntimeException {
  private static final long serialVersionUID = -3922266917865193312L;

  public ParserException(String reason) {
    super(reason);
  }

  public ParserException(String reason, Throwable cause) {
    super(reason, cause);
  }
}
