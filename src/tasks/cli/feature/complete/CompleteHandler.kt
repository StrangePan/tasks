package tasks.cli.feature.complete

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.util.function.Function
import omnia.algorithm.SetAlgorithms.differenceBetween
import omnia.algorithm.SetAlgorithms.unionOf
import omnia.cli.out.Output
import omnia.data.cache.Memoized
import omnia.data.stream.Collectors.toImmutableSet
import omnia.data.structure.immutable.ImmutableSet
import omnia.data.structure.tuple.Triplet
import omnia.data.structure.tuple.Tuplet
import tasks.cli.command.common.CommonArguments
import tasks.cli.handler.ArgumentHandler
import tasks.cli.handler.HandlerException
import tasks.cli.handler.HandlerUtil
import tasks.model.ObservableTaskStore
import tasks.model.Task
import tasks.model.TaskId
import tasks.model.TaskMutator

/** Business logic for the Complete command.  */
class CompleteHandler(private val taskStore: Memoized<out ObservableTaskStore>) : ArgumentHandler<CompleteArguments> {

  override fun handle(arguments: CommonArguments<out CompleteArguments>): Single<Output> {
    // Validate arguments
    if (!arguments.specificArguments().tasks().isPopulated) {
      throw HandlerException("no tasks specified")
    }
    val taskStore = taskStore.value()
    return Observable.fromIterable(arguments.specificArguments().tasks())
        .flatMapSingle { task: Task -> taskStore.mutateTask(task) { obj: TaskMutator -> obj.complete() } }
        .reduceWith<Triplet<ImmutableSet.Builder<TaskId>>>(
            {
              Tuplet.of(
                  ImmutableSet.builder(),  // tasks that were already completed
                  ImmutableSet.builder(),  // tasks that were marked as completed
                  ImmutableSet.builder())  // tasks that became unblocked
            },
            { builders, mutationResult ->
              val becameCompleted = mutationResult.first()
                  .lookUpById(mutationResult.third().id())
                  .map { task -> !task.status().isCompleted }
                  .orElse(false)
              (if (becameCompleted) builders.second() else builders.first())
                  .add(mutationResult.third().id())
              if (becameCompleted) {
                mutationResult.third()
                    .blockedTasks()
                    .stream()
                    .filter(Task::isUnblocked)
                    .filter { task -> !task.status().isCompleted }
                    .map(Task::id)
                    .forEach(builders.third()::add)
              }
              builders
            })
        .map { groupedTasks -> groupedTasks.map(Function { it.build() }) }
        .map {
          Tuplet.of(
              it.first(),
              it.second(),
              ImmutableSet.copyOf(
                  differenceBetween(
                      it.third(),
                      unionOf(it.first(), it.second()))))
        }
        .flatMap { groupedTasks ->
          taskStore.observe()
              .firstOrError()
              .map { store ->
                groupedTasks.map(
                    Function { list ->
                      list.stream()
                          .map { id -> store.lookUpById(id) }
                          .map { obj -> obj.orElseThrow() }
                          .collect(toImmutableSet())
                    })
              }
        }
        .flatMapObservable { source -> Observable.fromIterable(source) }
        .zipWith(
            Observable.just(
                "task(s) already completed:",
                "task(s) completed:",
                "task(s) unblocked as a result:"),
            { groupedTasks, header -> HandlerUtil.stringifyIfPopulated(header, groupedTasks) })
        .collect(Output.Companion::builder, Output.Builder::append)
        .map { it.build() }
  }

}