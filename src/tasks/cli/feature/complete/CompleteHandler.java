package tasks.cli.feature.complete;

import static java.util.Objects.requireNonNull;
import static tasks.cli.handler.HandlerUtil.groupByCompletionState;
import static tasks.cli.handler.HandlerUtil.printIfPopulated;
import static tasks.cli.handler.HandlerUtil.stringifyIfPopulated;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import java.util.EnumMap;
import omnia.cli.out.Output;
import omnia.data.cache.Memoized;
import omnia.data.structure.Set;
import omnia.data.structure.immutable.ImmutableSet;
import tasks.cli.handler.ArgumentHandler;
import tasks.cli.handler.HandlerException;
import tasks.cli.handler.HandlerUtil.CompletedState;
import tasks.model.Task;
import tasks.model.TaskStore;

/** Business logic for the Complete command. */
public final class CompleteHandler implements ArgumentHandler<CompleteArguments> {
  private final Memoized<? extends TaskStore> taskStore;

  public CompleteHandler(Memoized<? extends TaskStore> taskStore) {
    this.taskStore = requireNonNull(taskStore);
  }

  @Override
  public Single<Output> handle(CompleteArguments arguments) {
    // Validate arguments
    if (!arguments.tasks().isPopulated()) {
      throw new HandlerException("no tasks specified");
    }

    TaskStore taskStore = this.taskStore.value();

    EnumMap<CompletedState, Set<Task>> tasksGroupedByState =
        groupByCompletionState(Observable.fromIterable(arguments.tasks()));

    Set<Task> alreadyCompletedTasks =
        tasksGroupedByState.getOrDefault(CompletedState.COMPLETE, Set.empty());
    Set<Task> incompleteTasks =
        tasksGroupedByState.getOrDefault(CompletedState.INCOMPLETE, Set.empty());

    // report tasks already completed
    printIfPopulated("task(s) already marked as completed:", alreadyCompletedTasks);

    // mark incomplete tasks as complete
    return Observable.fromIterable(incompleteTasks)
        .concatMapCompletable(task -> task.mutate(mutator -> mutator.setCompleted(true)))
        .andThen(taskStore.writeToDisk())
        .andThen(findTasksBlockedBy(incompleteTasks))
        .compose(CompleteHandler::onlyTasksThatAreUnblocked)
        .map(
            newlyUnblockedTasks ->
                Output.builder()
                    .append(stringifyIfPopulated("task(s) marked as completed:", incompleteTasks))
                    .append(stringifyIfPopulated("task(s) unblocked as a result:", newlyUnblockedTasks))
                    .build());
  }

  private static Single<Set<Task>> findTasksBlockedBy(Set<Task> tasks) {
    return Flowable.fromIterable(tasks)
        .flatMapSingle(task -> task.query().tasksBlockedByThis().firstOrError())
        .<ImmutableSet.Builder<Task>>collect(
            ImmutableSet::builder, (builder, taskSet) -> builder.addAll(taskSet))
        .map(ImmutableSet.Builder::build);
  }

  private static Single<Set<Task>> onlyTasksThatAreUnblocked(Single<Set<Task>> tasks) {
    return tasks.flatMapObservable(Observable::fromIterable)
        .flatMapMaybe(
            task ->
                task.isUnblocked()
                    .firstOrError()
                    .filter(isUnblocked -> isUnblocked)
                    .map(isUnblocked -> task))
        .<ImmutableSet.Builder<Task>>collect(ImmutableSet::builder, ImmutableSet.Builder::add)
        .map(ImmutableSet.Builder::build);
  }
}
