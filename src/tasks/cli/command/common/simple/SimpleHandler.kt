package tasks.cli.command.common.simple

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.util.function.Function
import omnia.algorithm.SetAlgorithms
import omnia.cli.out.Output
import omnia.data.cache.Memoized
import omnia.data.stream.Collectors.toImmutableSet
import omnia.data.structure.immutable.ImmutableSet
import omnia.data.structure.tuple.Quadruplet
import omnia.data.structure.tuple.Tuplet
import tasks.cli.command.common.CommonArguments
import tasks.cli.handler.ArgumentHandler
import tasks.cli.handler.HandlerException
import tasks.cli.handler.HandlerUtil
import tasks.model.ObservableTaskStore
import tasks.model.Task
import tasks.model.TaskId
import tasks.model.TaskMutator

/**
 * An [ArgumentHandler] that changes the state of one or more tasks simultaneously and outputs the
 * resulting updates.
 *
 * @param taskStore the task store to mutate
 * @param mutator the mutation to apply to all tasks specified in the commands
 * @param diffDetector the comparator to use to detect if the [mutator] did its job
 * @param headerWhenChanged the header to print above the tasks that were successfully mutated
 * @param headerWhenUnchanged the header to print above the tasks that were not affected for
 *     whatever reason
 */
abstract class SimpleHandler<T : SimpleArguments> protected constructor(
    private val taskStore: Memoized<out ObservableTaskStore>,
    private val mutator: Function<TaskMutator, TaskMutator>,
    private val diffDetector: Comparator<in Task>,
    private val headerWhenChanged: String,
    private val headerWhenUnchanged: String)
  : ArgumentHandler<T> {

  final override fun handle(arguments: CommonArguments<out T>): Single<Output> {
    // Validate arguments
    if (!arguments.specificArguments().tasks().isPopulated) {
      throw HandlerException("no tasks specified")
    }
    val taskStore = taskStore.value()

    return Observable.fromIterable(arguments.specificArguments().tasks())
        .flatMapSingle { task -> taskStore.mutateTask(task, mutator) }
        .reduceWith<Quadruplet<ImmutableSet.Builder<TaskId>>>(
            {
              Tuplet.of(
                  ImmutableSet.builder(),
                  ImmutableSet.builder(),
                  ImmutableSet.builder(),
                  ImmutableSet.builder())
            },
            { builders, mutationResult ->
              val taskId = mutationResult.third().id()
              val taskBefore = mutationResult.first().lookUpById(taskId).orElseThrow()
              val taskAfter = mutationResult.second().lookUpById(taskId).orElseThrow()
              val didChange = diffDetector.compare(taskBefore, taskAfter) != 0
              (if (didChange) builders.second() else builders.first()).add(taskId)
              if (didChange) {
                taskAfter.blockedTasks().forEach { successorAfter ->
                  val id = successorAfter.id()
                  val isUnblocked = successorAfter.isUnblocked
                  val wasUnblocked = mutationResult.first().lookUpById(id).orElseThrow().isUnblocked
                  (when {
                    isUnblocked && !wasUnblocked -> builders.third()
                    !isUnblocked && wasUnblocked -> builders.fourth()
                    else -> null
                  })?.add(id)
                }
              }
              builders
            })
        .map { groupedTasks -> groupedTasks.map(Function { it.build() }) }
        .map {
          val firstAndSecond = SetAlgorithms.unionOf(it.first(), it.second())
          Tuplet.of(
              it.first(),
              it.second(),
              SetAlgorithms.differenceBetween(it.third(), firstAndSecond),
              SetAlgorithms.differenceBetween(it.fourth(), firstAndSecond))
        }
        .flatMap { groupedTask ->
          taskStore.observe()
              .firstOrError()
              .map { store ->
                groupedTask.map(
                    Function { list ->
                      list.stream()
                          .map { store.lookUpById(it).orElseThrow() }
                          .collect(toImmutableSet())
                    })
              }
        }
        .flatMapObservable { Observable.fromIterable(it) }
        .zipWith(
            Observable.just(
                headerWhenUnchanged,
                headerWhenChanged,
                "task(s) unblocked as a result:",
                "task(s) blocked as a result:")) {
          groupedTasks, header -> HandlerUtil.stringifyIfPopulated(header, groupedTasks)
        }
        .collect(Output.Companion::builder, Output.Builder::append)
        .map(Output.Builder::build)
  }
}