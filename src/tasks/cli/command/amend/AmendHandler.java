package tasks.cli.command.amend;

import static java.util.Objects.requireNonNull;
import static tasks.cli.handlers.HandlerUtil.verifyTasksAreMutuallyExclusive;

import io.reactivex.Completable;
import omnia.data.cache.Memoized;
import omnia.data.structure.immutable.ImmutableSet;
import tasks.cli.handlers.ArgumentHandler;
import tasks.cli.handlers.HandlerException;
import tasks.model.Task;
import tasks.model.TaskStore;

public final class AmendHandler implements ArgumentHandler<AmendArguments> {
  private final Memoized<TaskStore> taskStore;

  public AmendHandler(Memoized<TaskStore> taskStore) {
    this.taskStore = requireNonNull(taskStore);
  }

  @Override
  public Completable handle(AmendArguments arguments) {
    /*
    Ensure task isn't connected to itself.
    This is a short-circuit, but is not strictly required because we still need to check if the task
    graph is cyclical.
    */
    if (ImmutableSet.<Task>builder()
        .addAll(arguments.blockedTasks())
        .addAll(arguments.blockedTasksToAdd())
        .addAll(arguments.blockedTasksToRemove())
        .addAll(arguments.blockingTasks())
        .addAll(arguments.blockingTasksToAdd())
        .addAll(arguments.blockingTasksToRemove())
        .build()
        .contains(arguments.targetTask())) {
      throw new HandlerException("target task cannot block or be blocked by itself");
    }

    // ensure the same task isn't added and removed at the same time
    verifyTasksAreMutuallyExclusive(
        "ambiguous operation: tasks both added and removed from before: ",
        arguments.blockingTasksToAdd(),
        arguments.blockingTasksToRemove());

    verifyTasksAreMutuallyExclusive(
        "ambiguous operation: tasks both added and removed from after: ",
        arguments.blockedTasksToAdd(),
        arguments.blockedTasksToRemove());

    // okay, operation isn't ambiguous, task doesn't reference itself, and task ids are valid
    TaskStore taskStore = this.taskStore.value();
    return taskStore.mutateTask(
        arguments.targetTask(),
        mutator -> {
          arguments.description().ifPresent(mutator::setLabel);

          if (arguments.blockingTasks().isPopulated()) {
            mutator.setBlockingTasks(arguments.blockingTasks());
          }
          arguments.blockingTasksToAdd().forEach(mutator::addBlockingTask);
          arguments.blockingTasksToRemove().forEach(mutator::removeBlockingTask);

          if (arguments.blockedTasks().isPopulated()) {
            mutator.setBlockedTasks(arguments.blockedTasks());
          }
          arguments.blockedTasksToAdd().forEach(mutator::addBlockedTask);
          arguments.blockedTasksToRemove().forEach(mutator::removeBlockedTask);

          return mutator;
        })
        .andThen(taskStore.writeToDisk())
        .doOnComplete(() -> System.out.println("task amended: " + arguments.targetTask()));
  }
}
