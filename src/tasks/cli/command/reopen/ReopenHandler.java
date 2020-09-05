package tasks.cli.command.reopen;

import static java.util.Objects.requireNonNull;
import static tasks.cli.handler.HandlerUtil.printIfPopulated;
import static tasks.cli.handler.HandlerUtil.stringifyIfPopulated;

import io.reactivex.Observable;
import io.reactivex.Single;
import java.util.EnumMap;
import omnia.cli.out.Output;
import omnia.data.cache.Memoized;
import omnia.data.structure.Set;
import tasks.cli.handler.ArgumentHandler;
import tasks.cli.handler.HandlerException;
import tasks.cli.handler.HandlerUtil;
import tasks.model.Task;
import tasks.model.TaskStore;

/** Business logic for the Reopen command. */
public final class ReopenHandler implements ArgumentHandler<ReopenArguments> {
  private final Memoized<TaskStore> taskStore;

  public ReopenHandler(Memoized<TaskStore> taskStore) {
    this.taskStore = requireNonNull(taskStore);
  }

  @Override
  public Single<Output> handle(ReopenArguments arguments) {
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
    printIfPopulated("tasks already open:", alreadyOpenTasks);

    // mark incomplete tasks as complete and commit to disk
    return Observable.fromIterable(completedTasks)
        .flatMapCompletable(task -> task.mutate(mutator -> mutator.setCompleted(false)))
        .andThen(taskStore.writeToDisk())
        .andThen(Single.just(stringifyIfPopulated("task(s) reopened:", completedTasks)));
  }
}
