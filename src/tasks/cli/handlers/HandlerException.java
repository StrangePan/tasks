package tasks.cli.handlers;

public final class HandlerException extends RuntimeException {
  HandlerException(String message) {
    super(message);
  }

  HandlerException(String message, Throwable cause) {
    super(message, cause);
  }
}
