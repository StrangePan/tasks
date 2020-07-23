package tasks.cli.handlers;

import io.reactivex.Completable;

public interface ArgumentHandler<T> {
  /**
   * Accepts would-be arguments and attempts to handle them. May throw an exception if an object empty
   * the wrong instance is provided.
   */
  Completable handle(T arguments);
}