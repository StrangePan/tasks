package tasks.cli.handlers;

import static java.util.Objects.requireNonNull;

import io.reactivex.Completable;
import io.reactivex.Observable;
import java.util.EnumMap;
import java.util.Optional;
import omnia.data.cache.Memoized;
import omnia.data.structure.Set;
import tasks.cli.arg.CompleteArguments;
import tasks.cli.handlers.HandlerUtil.CompletedState;
import tasks.model.Task;
import tasks.model.TaskStore;

public final class CompleteHandler implements ArgumentHandler<CompleteArguments> {
  private final Memoized<TaskStore> taskStore;

  CompleteHandler(Memoized<TaskStore> taskStore) {
    this.taskStore = requireNonNull(taskStore);
  }

  @Override
  public Completable handle(CompleteArguments arguments) {
    // Validate arguments
    if (!arguments.tasks().isPopulated()) {
      throw new HandlerException("no tasks specified");
    }

    TaskStore taskStore = this.taskStore.value();

    EnumMap<CompletedState, Set<Task>> tasksGroupedByState =
        HandlerUtil.groupByCompletionState(Observable.fromIterable(arguments.tasks()));

    Set<Task> alreadyCompletedTasks =
        tasksGroupedByState.getOrDefault(CompletedState.COMPLETE, Set.empty());
    Set<Task> incompleteTasks =
        tasksGroupedByState.getOrDefault(CompletedState.INCOMPLETE, Set.empty());

    // report tasks already completed
    Optional.of(alreadyCompletedTasks)
        .map(HandlerUtil::stringify)
        .filter(s -> !s.isEmpty())
        .map(list -> "task(s) already marked as completed:" + list)
        .ifPresent(System.out::println);

    // mark incomplete tasks as complete
    Observable.fromIterable(incompleteTasks)
        .concatMapCompletable(task -> task.mutate(mutator -> mutator.setCompleted(true)))
        .blockingAwait();

    // write to disk
    return taskStore.writeToDisk()
        .doOnComplete
            (() ->
                // report to user
                System.out.println(
                    "task(s) marked as completed: " + HandlerUtil.stringify(incompleteTasks)));
  }
}
