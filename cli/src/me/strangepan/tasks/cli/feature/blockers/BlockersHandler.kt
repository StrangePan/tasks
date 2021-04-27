package me.strangepan.tasks.cli.feature.blockers

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.util.function.Consumer
import java.util.function.Function
import omnia.cli.out.Output
import omnia.cli.out.Output.Companion.empty
import omnia.data.cache.Memoized
import omnia.data.stream.Collectors.toImmutableSet
import omnia.data.structure.Set
import omnia.data.structure.immutable.ImmutableSet
import omnia.data.structure.tuple.Couplet
import omnia.data.structure.tuple.Triple
import omnia.data.structure.tuple.Tuple
import omnia.data.structure.tuple.Tuplet
import me.strangepan.tasks.cli.command.common.CommonArguments
import me.strangepan.tasks.cli.handler.ArgumentHandler
import me.strangepan.tasks.cli.handler.HandlerUtil
import me.strangepan.tasks.cli.handler.HandlerUtil.stringifyIfPopulated
import me.strangepan.tasks.engine.model.ObservableTaskStore
import me.strangepan.tasks.engine.model.Task
import me.strangepan.tasks.engine.model.TaskStore

/** Business logic for the Blockers command.  */
class BlockersHandler(private val taskStore: Memoized<out ObservableTaskStore>) : ArgumentHandler<BlockersArguments> {

  override fun handle(arguments: CommonArguments<out BlockersArguments>): Single<Output> {
    /*
     * Ensure task isn't connected to itself.
     * This is a short-circuit, but is not strictly required because we still need to check if the
     * task graph is cyclical.
     */
    HandlerUtil.verifyTasksAreMutuallyExclusive(
        "me.strangepan.tasks.engine.tasks cannot block or be blocked by themselves:",
        arguments.specificArguments().blockingTasksToAdd(),
        arguments.specificArguments().targetTasks())
    HandlerUtil.verifyTasksAreMutuallyExclusive(
        "ambiguous operation: blockers both added and removed: ",
        arguments.specificArguments().blockingTasksToAdd(),
        arguments.specificArguments().blockingTasksToRemove())
    return if (!arguments.specificArguments().blockingTasksToAdd().isPopulated
        && !arguments.specificArguments().blockingTasksToRemove().isPopulated
        && !arguments.specificArguments().clearAllBlockers()) {
      Single.just(empty())
    } else mutateAndProduceBeforeAfterSnapshots(arguments.specificArguments())
        .map(::stringifyResults)
        .collect(Output::builder, Output.Builder::appendLine)
        .map(Output.Builder::build)
  }

  private fun mutateAndProduceBeforeAfterSnapshots(arguments: BlockersArguments):
      Observable<out Triple<Task, Set<out Task>, Set<out Task>>> {
    return mutateTaskStore(arguments)
        .flatMapObservable { couplet ->
          Observable.fromIterable(arguments.targetTasks())
              .map { task ->
                val blockingTasksBeforeAfter = couplet.map(Function { store ->
                  store.lookUpById(task.id())
                      .map(Task::blockingTasks)
                      .orElse(ImmutableSet.empty())
                })
                Tuple.of(task, blockingTasksBeforeAfter.first(), blockingTasksBeforeAfter.second())
              }
        }
  }

  private fun mutateTaskStore(arguments: BlockersArguments): Single<Couplet<TaskStore>> {
    return Observable.fromIterable(arguments.targetTasks())
        .flatMapSingle { task ->
          taskStore.value().mutateTask(task) { mutator ->
            if (arguments.clearAllBlockers()) {
              mutator.setBlockingTasks(arguments.blockingTasksToAdd())
            } else {
              arguments.blockingTasksToRemove().forEach(Consumer(mutator::removeBlockingTask))
              arguments.blockingTasksToAdd().forEach(Consumer(mutator::addBlockingTask))
            }
            mutator
          }
        }
        .map { it.dropThird() }
        .reduce { firstCouple, secondCouple -> firstCouple.mapSecond { secondCouple.second() }}
        .toSingle()
        .map(Tuplet.Companion::copyOf)
  }

  companion object {
    private fun stringifyResults(
        beforeAfterSnapshots: Triple<out Task, out Set<out Task>, out Set<out Task>>): Output {
      return Output.builder()
          .appendLine(beforeAfterSnapshots.first().render())
          .appendLine(stringifyIfPopulated("current blockers:", beforeAfterSnapshots.third()))
          .appendLine(
              stringifyIfPopulated(
                  "removed blockers:",
                  getRemovedBlockers(beforeAfterSnapshots.second(), beforeAfterSnapshots.third())))
          .build()
    }

    private fun getRemovedBlockers(before: Set<out Task>, after: Set<out Task>): Set<out Task> {
      val afterIds = after.stream().map(Task::id).collect(toImmutableSet())
      return before.stream().filter { !afterIds.contains(it.id()) }.collect(toImmutableSet())
    }
  }
}