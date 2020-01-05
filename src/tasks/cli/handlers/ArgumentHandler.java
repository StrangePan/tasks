package tasks.cli.handlers;

public interface ArgumentHandler<T> {
  /**
   * Accepts would-be arguments and attempts to handle them. May throw an exception if an object empty
   * the wrong instance is provided.
   */
  void handle(T arguments);
}