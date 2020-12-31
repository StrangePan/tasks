package tasks.cli.feature.info

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import omnia.cli.out.Output
import omnia.cli.out.Output.Companion.builder
import omnia.cli.out.Output.Companion.justNewline
import omnia.data.structure.Set
import tasks.cli.command.common.CommonArguments
import tasks.cli.handler.ArgumentHandler
import tasks.cli.handler.HandlerException
import tasks.cli.handler.HandlerUtil
import tasks.model.Task

/** Business logic for the Info command.  */
class InfoHandler : ArgumentHandler<InfoArguments> {
  override fun handle(arguments: CommonArguments<out InfoArguments>): Single<Output> {
    if (!arguments.specificArguments().tasks().isPopulated) {
      throw HandlerException("no tasks specified")
    }
    return Observable.fromIterable(arguments.specificArguments().tasks())
        .map { task: Task -> stringify(task) }
        .flatMap { output: Output -> Observable.just(justNewline(), output) }
        .skip(1)
        .collect(Output::builder, Output.Builder::appendLine)
        .map(Output.Builder::build)
  }

  companion object {
    private fun stringify(task: Task): Output {
      return builder()
          .appendLine(task.render())
          .appendLine(stringifyIfPopulated("tasks blocking this:", task.blockingTasks()))
          .appendLine(stringifyIfPopulated("tasks blocked by this:", task.blockedTasks()))
          .build()
    }

    private fun stringifyIfPopulated(prefix: String, tasks: Set<out Task>): Output {
      return HandlerUtil.stringifyIfPopulated(prefix, tasks)
    }
  }
}