package tasks.cli.command.reopen;

import static java.util.Objects.requireNonNull;

import io.reactivex.Completable;
import io.reactivex.Observable;
import java.util.EnumMap;
import omnia.data.cache.Memoized;
import omnia.data.structure.Set;
import tasks.cli.handlers.ArgumentHandler;
import tasks.cli.handlers.HandlerException;
import tasks.cli.handlers.HandlerUtil;
import tasks.model.Task;
import tasks.model.TaskStore;

public final class ReopenHandler implements ArgumentHandler<ReopenArguments> {
  private final Memoized<TaskStore> taskStore;

  public ReopenHandler(Memoized<TaskStore> taskStore) {
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
    HandlerUtil.printIfPopulated("tasks already open:", alreadyOpenTasks);

    // mark incomplete tasks as complete and commit to disk
    return Observable.fromIterable(completedTasks)
        .flatMapCompletable(task -> task.mutate(mutator -> mutator.setCompleted(false)))
        .andThen(taskStore.writeToDisk())
        .andThen(
            Completable.fromAction(
                () -> HandlerUtil.printIfPopulated("task(s) reopened:", completedTasks)));
  }
}
