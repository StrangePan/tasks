package tasks.cli.handlers;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

import io.reactivex.Completable;
import java.util.Optional;
import omnia.algorithm.SetAlgorithms;
import omnia.data.cache.Memoized;
import omnia.data.structure.Set;
import omnia.data.structure.immutable.ImmutableSet;
import tasks.cli.arg.AmendArguments;
import tasks.model.Task;
import tasks.model.TaskStore;

public final class AmendHandler implements ArgumentHandler<AmendArguments> {
  private final Memoized<TaskStore> taskStore;

  AmendHandler(Memoized<TaskStore> taskStore) {
    this.taskStore = requireNonNull(taskStore);
  }

  @Override
  public Completable handle(AmendArguments arguments) {
    Task targetTask = arguments.targetTask();

    // nifty trick for limiting the scope of variables
    {
      Set<Task> allSpecifiedTasks =
          ImmutableSet.<Task>builder()
              .addAll(arguments.blockedTasks())
              .addAll(arguments.blockedTasksToAdd())
              .addAll(arguments.blockedTasksToRemove())
              .addAll(arguments.blockingTasks())
              .addAll(arguments.blockingTasksToAdd())
              .addAll(arguments.blockingTasksToRemove())
              .build();

      // ensure task isn't connected to itself
      // this is a convenience but is not strictly required because we still need to check if the
      // graph is cyclical
      if (allSpecifiedTasks.contains(targetTask)) {
        throw new HandlerException("target task cannot block or be blocked by itself");
      }
    }

    // ensure the same task isn't added and removed at the same time
    Optional.of(
        SetAlgorithms.intersectionOf(
            ImmutableSet.copyOf(arguments.blockingTasksToAdd()),
            ImmutableSet.copyOf(arguments.blockingTasksToRemove()))
        .stream()
        .map(Object::toString)
        .collect(joining(", ")))
        .filter(s -> !s.isEmpty())
        .map(ambiguousTasks ->
            "ambiguous operation: tasks both added and removed from before: " + ambiguousTasks)
        .ifPresent(message -> {
          throw new HandlerException(message);
        });

    Optional.of(
        SetAlgorithms.intersectionOf(
            ImmutableSet.copyOf(arguments.blockedTasksToAdd()),
            ImmutableSet.copyOf(arguments.blockedTasksToRemove()))
        .stream()
        .map(Object::toString)
        .collect(joining(", ")))
        .filter(s -> !s.isEmpty())
        .map(ambiguousTasks ->
            "ambiguous operation: tasks both added and removed from after: " + ambiguousTasks)
        .ifPresent(message -> {
          throw new HandlerException(message);
        });

    // okay, operation isn't ambiguous, task doesn't reference itself, and task ids are valid
    TaskStore taskStore = this.taskStore.value();
    taskStore.mutateTask(
        targetTask,
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
        }).blockingAwait();

    return taskStore.writeToDisk()
        .doOnComplete(() -> System.out.println("task amended: " + targetTask));
  }
}
