package tasks.cli.feature.add

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import omnia.cli.out.Output
import omnia.cli.out.Output.Companion.builder
import omnia.data.cache.Memoized
import omnia.data.structure.Set
import omnia.data.structure.immutable.ImmutableSet
import tasks.cli.command.common.CommonArguments
import tasks.cli.handler.ArgumentHandler
import tasks.cli.handler.HandlerException
import tasks.model.ObservableTaskStore
import tasks.model.Task
import tasks.model.TaskBuilder

/** Business logic for the Add command.  */
class AddHandler(private val taskStore: Memoized<out ObservableTaskStore>) : ArgumentHandler<AddArguments> {

  override fun handle(arguments: CommonArguments<out AddArguments>): Single<Output> {
    // Validate arguments
    val description = arguments.specificArguments().description().trim { it <= ' ' }
    if (description.isEmpty()) {
      throw HandlerException("description cannot be empty or whitespace only")
    }

    // Collect the dependencies and dependents
    val blockingTasks: Set<Task> = ImmutableSet.copyOf(arguments.specificArguments().blockingTasks())
    val blockedTasks: Set<Task> = ImmutableSet.copyOf(arguments.specificArguments().blockedTasks())
    val taskStore = taskStore.value()

    // Construct the new task, commit to disk, print output
    return taskStore.createTask(
        description
    ) { builder: TaskBuilder ->
      Single.just(builder)
          .flatMap { b: TaskBuilder -> Observable.fromIterable(blockingTasks).reduce(b, { obj: TaskBuilder, task: Task -> obj.addBlockingTask(task) }) }
          .flatMap { b: TaskBuilder -> Observable.fromIterable(blockedTasks).reduce(b, { obj: TaskBuilder, task: Task -> obj.addBlockedTask(task) }) }
          .blockingGet()
    }
        .map { obj -> obj.third() }
        .map { obj -> obj.render() }
        .map { output: Output -> builder().append("task created: ").append(output).build() }
        .map { output: Output -> builder().appendLine(output).build() }
  }

}