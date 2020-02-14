package tasks.cli.handlers;

import static java.util.stream.Collectors.joining;

import io.reactivex.Observable;
import java.util.EnumMap;
import java.util.Optional;
import omnia.data.structure.Set;
import omnia.data.structure.immutable.ImmutableSet;
import tasks.cli.arg.CompleteArguments;
import tasks.cli.handlers.HandlerUtil.CompletedState;
import tasks.model.Task;
import tasks.model.TaskStore;

public final class CompleteHandler implements ArgumentHandler<CompleteArguments> {
  @Override
  public void handle(CompleteArguments arguments) {
    // Validate arguments
    if (!arguments.tasks().isPopulated()) {
      throw new HandlerException("no tasks specified");
    }

    TaskStore taskStore = HandlerUtil.loadTaskStore();
    HandlerUtil.validateTasksIds(taskStore, arguments.tasks());

    Set<Task> specifiedTasks =
        ImmutableSet.copyOf(HandlerUtil.toTasks(taskStore, arguments.tasks()).blockingIterable());

    EnumMap<CompletedState, Set<Task>> tasksGroupedByState =
        HandlerUtil.groupByCompletionState(Observable.fromIterable(specifiedTasks));

    Set<Task> alreadyCompletedTasks =
        tasksGroupedByState.getOrDefault(CompletedState.COMPLETE, Set.empty());
    Set<Task> incompleteTasks =
        tasksGroupedByState.getOrDefault(CompletedState.INCOMPLETE, Set.empty());

    // report tasks already completed
    Optional.of(
        alreadyCompletedTasks.stream()
            .map(Object::toString)
            .collect(joining(", ")))
        .filter(s -> !s.isEmpty())
        .map(list -> "task(s) already marked as completed: " + list)
        .ifPresent(System.out::println);

    // mark incomplete tasks as complete
    Observable.fromIterable(incompleteTasks)
        .concatMapCompletable(task -> task.mutate(mutator -> mutator.setCompleted(true)))
        .blockingAwait();

    // report to user
    System.out.println(
        "task(s) marked as completed: "
            + incompleteTasks.stream().map(Object::toString).collect(joining(", ")));

    // write to disk
    taskStore.writeToDisk().blockingAwait();
  }
}
