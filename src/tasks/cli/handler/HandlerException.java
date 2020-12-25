package tasks.cli.handler;

public final class HandlerException extends RuntimeException {
  private static final long serialVersionUID = 374445314238057858L;

  public HandlerException(String message) {
    super(message);
  }

  public HandlerException(String message, Throwable cause) {
    super(message, cause);
  }
}
