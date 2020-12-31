package tasks.cli.feature.start

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.util.function.Function
import omnia.cli.out.Output
import omnia.cli.out.Output.Companion.builder
import omnia.data.cache.Memoized
import omnia.data.stream.Collectors.toImmutableSet
import omnia.data.structure.immutable.ImmutableSet
import omnia.data.structure.tuple.Tuplet
import tasks.cli.command.common.CommonArguments
import tasks.cli.handler.ArgumentHandler
import tasks.cli.handler.HandlerException
import tasks.cli.handler.HandlerUtil.stringifyIfPopulated
import tasks.model.ObservableTaskStore
import tasks.model.Task
import tasks.model.TaskId
import tasks.model.TaskMutator

/** Business logic for the Start command.  */
class StartHandler(private val taskStore: Memoized<out ObservableTaskStore>) : ArgumentHandler<StartArguments> {

  override fun handle(arguments: CommonArguments<out StartArguments>): Single<Output> {
    // Validate arguments
    if (!arguments.specificArguments().tasks().isPopulated) {
      throw HandlerException("no tasks specified")
    }
    val taskStore = taskStore.value()
    return Observable.fromIterable(arguments.specificArguments().tasks())
        .flatMapSingle { task: Task -> taskStore.mutateTask(task) { obj: TaskMutator -> obj.start() } }
        .reduce(
            Tuplet.of<ImmutableSet.Builder<TaskId>>(
                ImmutableSet.builder(),  // tasks that were already started
                ImmutableSet.builder(),  // tasks that were marked as started
                ImmutableSet.builder()),  // tasks that became blocked
            { builders, mutationResult ->
              val beforeStatus = mutationResult.first()
                  .lookUpById(mutationResult.third().id())
                  .map(Task::status)
                  .orElseThrow()
              val becameStarted = !beforeStatus.isStarted
              (if (becameStarted) builders.second() else builders.first())
                  .add(mutationResult.third().id())
              if (becameStarted && beforeStatus.isCompleted) {
                mutationResult.third()
                    .blockedTasks()
                    .stream()
                    .map(Task::id)
                    .forEach { builders.third().add(it) }
              }
              builders
            })
        .map { groupedTasks -> groupedTasks.map(Function { it.build() }) }
        .flatMap { groupedTasks ->
          taskStore.observe()
              .firstOrError()
              .map { store ->
                groupedTasks.map(
                    Function { list ->
                      list.stream()
                          .map(store::lookUpById)
                          .map { it.orElseThrow() }
                          .collect(toImmutableSet())
                    })
              }
        }
        .map { groupedTasks ->
          Tuplet.of(
              groupedTasks.first(),
              groupedTasks.second(),
              groupedTasks.third()
                  .stream()
                  .filter { !it.status().isCompleted }
                  .collect(toImmutableSet()))
        }
        .flatMapObservable { source -> Observable.fromIterable(source) }
        .zipWith(
            Observable.just(
                "task(s) already started:",
                "task(s) started:",
                "task(s) blocked as a result:"),
            { groupedTasks, header -> stringifyIfPopulated(header, groupedTasks) })
        .collect(Output::builder, Output.Builder::append)
        .map(Output.Builder::build)
  }

}