package tasks.cli.handler

import io.reactivex.rxjava3.core.Single
import omnia.cli.out.Output
import tasks.cli.command.common.CommonArguments

interface ArgumentHandler<T : Any> {
  /**
   * Accepts would-be arguments and attempts to handle them. May throw an exception if an object empty
   * the wrong instance is provided.
   * @param arguments
   */
  fun handle(arguments: CommonArguments<out T>): Single<Output>
}