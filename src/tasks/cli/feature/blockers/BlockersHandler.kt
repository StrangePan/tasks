package tasks.cli.feature.blockers

import io.reactivex.rxjava3.core.Single
import java.util.function.Consumer
import java.util.function.Function
import omnia.cli.out.Output
import omnia.cli.out.Output.Companion.empty
import omnia.data.cache.Memoized
import omnia.data.stream.Collectors.toImmutableSet
import omnia.data.structure.Set
import omnia.data.structure.immutable.ImmutableSet
import omnia.data.structure.tuple.Couple
import omnia.data.structure.tuple.Couplet
import omnia.data.structure.tuple.Tuplet
import tasks.cli.command.common.CommonArguments
import tasks.cli.handler.ArgumentHandler
import tasks.cli.handler.HandlerException
import tasks.cli.handler.HandlerUtil
import tasks.cli.handler.HandlerUtil.stringifyIfPopulated
import tasks.model.ObservableTaskStore
import tasks.model.Task
import tasks.model.TaskMutator
import tasks.model.TaskStore

/** Business logic for the Blockers command.  */
class BlockersHandler(private val taskStore: Memoized<out ObservableTaskStore>) : ArgumentHandler<BlockersArguments> {

  override fun handle(arguments: CommonArguments<out BlockersArguments>): Single<Output> {
    /*
     * Ensure task isn't connected to itself.
     * This is a short-circuit, but is not strictly required because we still need to check if the
     * task graph is cyclical.
     */
    if (arguments.specificArguments()
            .blockingTasksToAdd()
            .contains(arguments.specificArguments().targetTask())) {
      throw HandlerException("target task cannot block or be blocked by itself")
    }
    HandlerUtil.verifyTasksAreMutuallyExclusive(
        "ambiguous operation: blockers both added and removed: ",
        arguments.specificArguments().blockingTasksToAdd(),
        arguments.specificArguments().blockingTasksToRemove())
    return if (!arguments.specificArguments().blockingTasksToAdd().isPopulated
        && !arguments.specificArguments().blockingTasksToRemove().isPopulated
        && !arguments.specificArguments().clearAllBlockers()) {
      Single.just(empty())
    } else mutateAndProduceBeforeAfterSnapshot(arguments.specificArguments())
        .map(::stringifyResults)
        .map { results ->
          Output.builder()
              .appendLine(arguments.specificArguments().targetTask().render())
              .appendLine(results)
              .build()
        }
  }

  private fun mutateAndProduceBeforeAfterSnapshot(arguments: BlockersArguments): Single<out Couplet<out Set<out Task>>> {
    return mutateTaskStore(arguments)
        .map { couplet ->
          couplet.map(Function { store ->
            store.lookUpById(arguments.targetTask().id())
                .map(Task::blockingTasks)
                .orElse(ImmutableSet.empty())
          })
        }
  }

  private fun mutateTaskStore(arguments: BlockersArguments): Single<Couplet<TaskStore>> {
    return taskStore.value().mutateTask(
        arguments.targetTask()
    ) { mutator: TaskMutator ->
      if (arguments.clearAllBlockers()) {
        mutator.setBlockingTasks(arguments.blockingTasksToAdd())
      } else {
        arguments.blockingTasksToRemove().forEach(Consumer { task: Task -> mutator.removeBlockingTask(task) })
        arguments.blockingTasksToAdd().forEach(Consumer { task: Task -> mutator.addBlockingTask(task) })
      }
      mutator
    }
        .map { it.dropThird() }
        .map(Tuplet.Companion::copyOf)
  }

  companion object {
    private fun stringifyResults(
        beforeAfterSnapshots: Couple<out Set<out Task>, out Set<out Task>>): Output {
      return Output.builder()
          .appendLine(stringifyIfPopulated("current blockers:", beforeAfterSnapshots.second()))
          .appendLine(
              stringifyIfPopulated(
                  "removed blockers:",
                  getRemovedBlockers(beforeAfterSnapshots.first(), beforeAfterSnapshots.second())))
          .build()
    }

    private fun getRemovedBlockers(before: Set<out Task>, after: Set<out Task>): Set<out Task> {
      val afterIds = after.stream().map { obj: Task -> obj.id() }.collect(toImmutableSet())
      return before.stream().filter { task: Task -> !afterIds.contains(task.id()) }.collect(toImmutableSet())
    }
  }
}