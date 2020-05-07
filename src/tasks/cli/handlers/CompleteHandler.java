package tasks.cli.handlers;

import static java.util.Objects.requireNonNull;

import io.reactivex.Completable;
import io.reactivex.Observable;
import java.util.EnumMap;
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
    HandlerUtil.printIfPopulated("task(s) already marked as completed:", alreadyCompletedTasks);

    // mark incomplete tasks as complete
    Observable.fromIterable(incompleteTasks)
        .concatMapCompletable(task -> task.mutate(mutator -> mutator.setCompleted(true)))
        .blockingAwait();

    // write to disk
    return taskStore.writeToDisk()
        .doOnComplete(
            () -> HandlerUtil.printIfPopulated("task(s) marked as completed:", incompleteTasks));
  }

}
