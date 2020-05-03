package tasks.cli.handlers;

import static java.util.Objects.requireNonNull;

import io.reactivex.Completable;
import io.reactivex.Observable;
import java.util.EnumMap;
import java.util.Optional;
import omnia.data.cache.Memoized;
import omnia.data.structure.Set;
import tasks.cli.arg.ReopenArguments;
import tasks.model.Task;
import tasks.model.TaskStore;

public final class ReopenHandler implements ArgumentHandler<ReopenArguments> {
  private final Memoized<TaskStore> taskStore;

  ReopenHandler(Memoized<TaskStore> taskStore) {
    this.taskStore = requireNonNull(taskStore);
  }

  @Override
  public Completable handle(ReopenArguments arguments) {
    // Validate arguments
    if (!arguments.tasks().isPopulated()) {
      throw new HandlerException("no tasks specified");
    }

    TaskStore taskStore = this.taskStore.value();

    EnumMap<HandlerUtil.CompletedState, Set<Task>> tasksGroupedByState =
        HandlerUtil.groupByCompletionState(Observable.fromIterable(arguments.tasks()));

    Set<Task> completedTasks =
        tasksGroupedByState.getOrDefault(HandlerUtil.CompletedState.COMPLETE, Set.empty());
    Set<Task> alreadyOpenTasks =
        tasksGroupedByState.getOrDefault(HandlerUtil.CompletedState.INCOMPLETE, Set.empty());

    // report tasks already open
    Optional.of(alreadyOpenTasks)
        .map(HandlerUtil::stringify)
        .filter(s -> !s.isEmpty())
        .map(list -> "tasks already open: " + list)
        .ifPresent(System.out::println);

    // mark incomplete tasks as complete
    Observable.fromIterable(completedTasks)
        .flatMapCompletable(task -> task.mutate(mutator -> mutator.setCompleted(false)))
        .blockingAwait();

    // write to disk
    return taskStore.writeToDisk()
        .doOnComplete(
            () ->
                // report to user
                System.out.println("task(s) reopened:" + HandlerUtil.stringify(completedTasks)));
  }
}
