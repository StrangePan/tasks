package tasks.cli.feature.blockers;

import static java.util.Objects.requireNonNull;
import static tasks.cli.handler.HandlerUtil.stringifyIfPopulated;
import static tasks.cli.handler.HandlerUtil.verifyTasksAreMutuallyExclusive;

import io.reactivex.Single;
import omnia.algorithm.SetAlgorithms;
import omnia.cli.out.Output;
import omnia.data.cache.Memoized;
import omnia.data.structure.Set;
import omnia.data.structure.immutable.ImmutableSet;
import omnia.data.structure.tuple.Couple;
import omnia.data.structure.tuple.Couplet;
import omnia.data.structure.tuple.Triple;
import omnia.data.structure.tuple.Tuplet;
import tasks.cli.handler.ArgumentHandler;
import tasks.cli.handler.HandlerException;
import tasks.model.Task;
import tasks.model.ObservableTaskStore;
import tasks.model.TaskStore;

/** Business logic for the Blockers command. */
public final class BlockersHandler implements ArgumentHandler<BlockersArguments> {
  private final Memoized<? extends ObservableTaskStore> taskStore;

  public BlockersHandler(Memoized<? extends ObservableTaskStore> taskStore) {
    this.taskStore = requireNonNull(taskStore);
  }

  @Override
  public Single<Output> handle(BlockersArguments arguments) {
    /*
     * Ensure task isn't connected to itself.
     * This is a short-circuit, but is not strictly required because we still need to check if the
     * task graph is cyclical.
     */
    if (arguments.blockingTasksToAdd().contains(arguments.targetTask())) {
      throw new HandlerException("target task cannot block or be blocked by itself");
    }

    verifyTasksAreMutuallyExclusive(
        "ambiguous operation: blockers both added and removed: ",
        arguments.blockingTasksToAdd(),
        arguments.blockingTasksToRemove());

    return mutateAndProduceBeforeAfterSnapshot(arguments)
        .map(BlockersHandler::stringifyResults)
        .map(
            results ->
                Output.builder()
                    .appendLine(arguments.targetTask().render())
                    .appendLine(results)
                    .build());
  }

  private Single<? extends Couplet<? extends Set<? extends Task>>>
      mutateAndProduceBeforeAfterSnapshot(BlockersArguments arguments) {
    return mutateTaskStore(arguments)
        .map(
            couplet -> couplet.map(
                store -> store.lookUpById(arguments.targetTask().id())
                    .map(Task::blockingTasks)
                    .orElse(ImmutableSet.empty())));
  }

  private Single<Couplet<TaskStore>> mutateTaskStore(
      BlockersArguments arguments) {
    return taskStore.value().mutateTask(
        arguments.targetTask(),
        mutator -> {
          if (arguments.clearAllBlockers()) {
            mutator.setBlockingTasks(arguments.blockingTasksToAdd());
          } else {
            arguments.blockingTasksToRemove().forEach(mutator::removeBlockingTask);
            arguments.blockingTasksToAdd().forEach(mutator::addBlockingTask);
          }
          return mutator;
        })
        .map(Triple::dropThird)
        .map(Tuplet::copyOf);
  }

  private static Set<? extends Task> getTasksBlocking(Task task) {
    return task.blockingTasks();
  }

  private static Output stringifyResults(
      Couple<? extends Set<? extends Task>, ? extends Set<? extends Task>> beforeAfterSnapshots) {
    return Output.builder()
        .appendLine(stringifyIfPopulated("current blockers:", beforeAfterSnapshots.second()))
        .appendLine(
            stringifyIfPopulated(
                "removed blockers:",
                getRemovedBlockers(beforeAfterSnapshots.first(), beforeAfterSnapshots.second())))
        .build();
  }

  private static Set<? extends Task> getRemovedBlockers(Set<? extends Task> before, Set<?> after) {
    return SetAlgorithms.differenceBetween(before, after);
  }
}
