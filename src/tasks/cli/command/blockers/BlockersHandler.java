package tasks.cli.command.blockers;

import static java.util.Objects.requireNonNull;
import static tasks.cli.handlers.HandlerUtil.stringifyIfPopulated;
import static tasks.cli.handlers.HandlerUtil.verifyTasksAreMutuallyExclusive;

import io.reactivex.Completable;
import io.reactivex.Single;
import omnia.algorithm.SetAlgorithms;
import omnia.cli.out.Output;
import omnia.data.cache.Memoized;
import omnia.data.structure.Set;
import omnia.data.structure.tuple.Couple;
import omnia.data.structure.tuple.Tuple;
import tasks.cli.handlers.ArgumentHandler;
import tasks.cli.handlers.HandlerException;
import tasks.model.Task;
import tasks.model.TaskStore;

/** Business logic for the Blockers command. */
public final class BlockersHandler implements ArgumentHandler<BlockersArguments> {
  private final Memoized<TaskStore> taskStore;

  public BlockersHandler(Memoized<TaskStore> taskStore) {
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

  private Single<Couple<Set<Task>, Set<Task>>> mutateAndProduceBeforeAfterSnapshot(
      BlockersArguments arguments) {
    return getTasksBlocking(arguments.targetTask())
        .flatMap(
            blockingTasksBeforeMutation ->
                mutateTaskStore(arguments)
                    .andThen(getTasksBlocking(arguments.targetTask()))
                    .map(blockingTasksAfterMutation ->
                        Tuple.of(blockingTasksBeforeMutation, blockingTasksAfterMutation)));
  }

  private Completable mutateTaskStore(BlockersArguments arguments) {
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
        .andThen(taskStore.value().writeToDisk());
  }

  private static Single<Set<Task>> getTasksBlocking(Task task) {
    return task.query().tasksBlockingThis().firstOrError();
  }

  private static Output stringifyResults(Couple<Set<Task>, Set<Task>> beforeAfterSnapshots) {
    return Output.builder()
        .appendLine(stringifyIfPopulated("current blockers:", beforeAfterSnapshots.second()))
        .appendLine(
            stringifyIfPopulated(
                "removed blockers:",
                getRemovedBlockers(beforeAfterSnapshots.first(), beforeAfterSnapshots.second())))
        .build();
  }

  private static Set<Task> getRemovedBlockers(Set<Task> before, Set<Task> after) {
    return SetAlgorithms.differenceBetween(before, after);
  }
}
