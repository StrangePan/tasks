package tasks.cli.handlers;

public final class HandlerException extends RuntimeException {
  public HandlerException(String message) {
    super(message);
  }

  public HandlerException(String message, Throwable cause) {
    super(message, cause);
  }
}
