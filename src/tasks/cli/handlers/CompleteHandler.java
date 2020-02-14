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

    System.out.println("prepared to load");

    TaskStore taskStore = HandlerUtil.loadTaskStore();

    System.out.println("loaded from file");

    HandlerUtil.validateTasksIds(taskStore, arguments.tasks());

    System.out.println("validate tasks against store");

    Set<Task> specifiedTasks =
        ImmutableSet.copyOf(HandlerUtil.toTasks(taskStore, arguments.tasks()).blockingIterable());

    System.out.println("prepared to group tasks by completed state");

    EnumMap<CompletedState, Set<Task>> tasksGroupedByState =
        HandlerUtil.groupByCompletionState(Observable.fromIterable(specifiedTasks));

    System.out.println("grouped tasks by completed state");

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

    System.out.println("prepared to mutate incomplete tasks");

    // mark incomplete tasks as complete
    Observable.fromIterable(incompleteTasks)
        .concatMapCompletable(task -> task.mutate(mutator -> mutator.setCompleted(true)))
        .blockingAwait();

    System.out.println("applied mutations to incomplete tasks");

    // report to user
    System.out.println(
        "task(s) marked as completed: "
            + incompleteTasks.stream().map(Object::toString).collect(joining(", ")));

    // write to disk
    taskStore.writeToDisk().blockingAwait();
  }
}
