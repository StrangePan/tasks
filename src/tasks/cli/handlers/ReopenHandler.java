package tasks.cli.handlers;

import static java.util.stream.Collectors.joining;

import io.reactivex.Observable;
import java.util.EnumMap;
import java.util.Optional;
import omnia.data.structure.Set;
import omnia.data.structure.immutable.ImmutableSet;
import tasks.cli.arg.ReopenArguments;
import tasks.model.Task;
import tasks.model.TaskStore;

public final class ReopenHandler implements ArgumentHandler<ReopenArguments> {
  @Override
  public void handle(ReopenArguments arguments) {
    // Validate arguments
    if (!arguments.tasks().isPopulated()) {
      throw new HandlerException("no tasks specified");
    }

    TaskStore taskStore = HandlerUtil.loadTaskStore();
    HandlerUtil.validateTasksIds(taskStore, arguments.tasks());

    Set<Task> specifiedTasks =
        ImmutableSet.copyOf(HandlerUtil.toTasks(taskStore, arguments.tasks()).blockingIterable());

    EnumMap<HandlerUtil.CompletedState, Set<Task>> tasksGroupedByState =
        HandlerUtil.groupByCompletionState(Observable.fromIterable(specifiedTasks));

    Set<Task> completedTasks =
        tasksGroupedByState.getOrDefault(HandlerUtil.CompletedState.COMPLETE, Set.empty());
    Set<Task> alreadyOpenTasks =
        tasksGroupedByState.getOrDefault(HandlerUtil.CompletedState.INCOMPLETE, Set.empty());

    // report tasks already open
    Optional.of(
        alreadyOpenTasks.stream()
            .map(Object::toString)
            .collect(joining(", ")))
        .filter(s -> !s.isEmpty())
        .map(list -> "tasks already open: " + list)
        .ifPresent(System.out::println);

    // mark incomplete tasks as complete
    Observable.fromIterable(completedTasks)
        .flatMapCompletable(task -> task.mutate(mutator -> mutator.setCompleted(false)))
        .blockingAwait();

    // report to user
    System.out.println(
        "task(s) reopened:"
            + completedTasks.stream().map(Object::toString).collect(joining(", ")));

    // write to disk
    taskStore.writeToDisk().blockingAwait();
  }
}
