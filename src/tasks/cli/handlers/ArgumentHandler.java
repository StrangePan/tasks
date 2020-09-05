package tasks.cli.handlers;

import io.reactivex.Single;
import omnia.cli.out.Output;

public interface ArgumentHandler<T> {
  /**
   * Accepts would-be arguments and attempts to handle them. May throw an exception if an object empty
   * the wrong instance is provided.
   */
  Single<Output> handle(T arguments);
}