package tasks.cli.handler;

import io.reactivex.Single;
import omnia.cli.out.Output;
import tasks.cli.command.common.CommonArguments;

public interface ArgumentHandler<T> {
  /**
   * Accepts would-be arguments and attempts to handle them. May throw an exception if an object empty
   * the wrong instance is provided.
   * @param arguments
   */
  Single<Output> handle(CommonArguments<? extends T> arguments);
}