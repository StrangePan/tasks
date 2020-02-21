package tasks.cli.handlers;

import static java.util.stream.Collectors.joining;

import io.reactivex.Completable;
import java.util.Optional;
import omnia.algorithm.SetAlgorithms;
import omnia.data.structure.Set;
import omnia.data.structure.immutable.ImmutableSet;
import tasks.cli.CliTaskId;
import tasks.cli.arg.AmendArguments;
import tasks.model.Task;
import tasks.model.TaskStore;

public final class AmendHandler implements ArgumentHandler<AmendArguments> {
  @Override
  public Completable handle(AmendArguments arguments) {
    TaskStore taskStore = HandlerUtil.loadTaskStore();

    Task targetTask =
        taskStore.lookUpById(arguments.targetTask().asLong())
            .map(Optional::of)
            .defaultIfEmpty(Optional.empty())
            .blockingGet()
            .orElseThrow(() ->
                new HandlerException("unknown task id specified: " + arguments.targetTask()));

    // nifty trick for limiting the scope of variables
    {
      Set<CliTaskId> allSpecifiedIds =
          ImmutableSet.<CliTaskId>builder()
              .addAll(arguments.blockedTasks())
              .addAll(arguments.blockedTasksToAdd())
              .addAll(arguments.blockedTasksToRemove())
              .addAll(arguments.blockingTasks())
              .addAll(arguments.blockingTasksToAdd())
              .addAll(arguments.blockingTasksToRemove())
              .build();

      HandlerUtil.validateTasksIds(taskStore, allSpecifiedIds);

      // ensure task isn't connected to itself
      // this is a convenience but is not strictly required because we still need to check if the
      // graph is cyclical
      if (allSpecifiedIds.contains(arguments.targetTask())) {
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
    taskStore.mutateTask(
        targetTask,
        mutator -> {
          arguments.description().ifPresent(mutator::setLabel);

          if (arguments.blockingTasks().isPopulated()) {
            mutator.setBlockingTasks(HandlerUtil.toTasks(taskStore, arguments.blockingTasks()).blockingIterable());
          }
          HandlerUtil.toTasks(taskStore, arguments.blockingTasksToAdd())
              .blockingForEach(mutator::addBlockingTask);
          HandlerUtil.toTasks(taskStore, arguments.blockingTasksToRemove())
              .blockingForEach(mutator::removeBlockingTask);

          if (arguments.blockedTasks().isPopulated()) {
            mutator.setBlockedTasks(HandlerUtil.toTasks(taskStore, arguments.blockedTasks()).blockingIterable());
          }
          HandlerUtil.toTasks(taskStore, arguments.blockedTasksToAdd())
              .blockingForEach(mutator::addBlockedTask);
          HandlerUtil.toTasks(taskStore, arguments.blockedTasksToRemove())
              .blockingForEach(mutator::removeBlockedTask);
          return mutator;
        }).blockingAwait();

    return taskStore.writeToDisk()
        .doOnComplete(() -> System.out.println("task amended: " + targetTask));
  }
}
