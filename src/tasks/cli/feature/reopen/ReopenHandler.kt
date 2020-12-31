package tasks.cli.feature.reopen

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.util.function.Function
import omnia.cli.out.Output
import omnia.data.cache.Memoized
import omnia.data.stream.Collectors.toImmutableSet
import omnia.data.structure.immutable.ImmutableSet
import omnia.data.structure.tuple.Tuplet
import tasks.cli.command.common.CommonArguments
import tasks.cli.handler.ArgumentHandler
import tasks.cli.handler.HandlerException
import tasks.cli.handler.HandlerUtil.stringifyIfPopulated
import tasks.model.ObservableTaskStore
import tasks.model.TaskId
import tasks.model.TaskMutator

/** Business logic for the Reopen command.  */
class ReopenHandler(private val taskStore: Memoized<out ObservableTaskStore>) : ArgumentHandler<ReopenArguments> {

  override fun handle(arguments: CommonArguments<out ReopenArguments>): Single<Output> {
    // Validate arguments
    if (!arguments.specificArguments().tasks().isPopulated) {
      throw HandlerException("no tasks specified")
    }
    val taskStore = taskStore.value()
    return Observable.fromIterable(arguments.specificArguments().tasks())
        .flatMapSingle { task -> taskStore.mutateTask(task, TaskMutator::reopen) }
        .reduce(
            Tuplet.of<ImmutableSet.Builder<TaskId>>(
                ImmutableSet.builder(),  // tasks that were already open
                ImmutableSet.builder(),  // tasks that were reopened
                ImmutableSet.builder()),  // tasks that became blocked
            { builders, mutationResult ->
              val becameOpen = (mutationResult.third().status().isOpen
                  && mutationResult.first()
                  .lookUpById(mutationResult.third().id())
                  .map { it.status().isCompleted }
                  .orElse(false))
              (if (becameOpen) builders.second() else builders.first())
                  .add(mutationResult.third().id())
              if (becameOpen) {
                mutationResult.third()
                    .blockedTasks()
                    .stream()
                    .map { it.id() }
                    .forEach { builders.third().add(it) }
              }
              builders
            })
        .map { groupedTasks -> groupedTasks.map(Function { it.build() }) }
        .flatMap { groupedTasks ->
          taskStore.observe()
              .firstOrError()
              .map { store ->
                groupedTasks.map(Function { list ->
                  list.stream()
                      .map { store.lookUpById(it) }
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
        .flatMapObservable { Observable.fromIterable(it) }
        .zipWith(
            Observable.just(
                "task(s) already open:",
                "task(s) reopened:",
                "task(s) blocked as a result:"),
            { groupedTasks, header -> stringifyIfPopulated(header, groupedTasks) })
        .collect(Output::builder, Output.Builder::append)
        .map(Output.Builder::build)
  }

}