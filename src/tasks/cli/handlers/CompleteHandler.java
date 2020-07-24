package tasks.cli.handlers;

import static java.util.Objects.requireNonNull;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import java.util.EnumMap;
import omnia.data.cache.Memoized;
import omnia.data.structure.Set;
import omnia.data.structure.immutable.ImmutableSet;
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
    return Observable.fromIterable(incompleteTasks)
        .concatMapCompletable(task -> task.mutate(mutator -> mutator.setCompleted(true)))
        .andThen(Completable.defer(taskStore::writeToDisk))
        .andThen(findTasksBlockedBy(incompleteTasks))
        .compose(this::onlyTasksThatAreUnblocked)
        .doOnSuccess(a -> HandlerUtil.printIfPopulated("task(s) marked as completed:", incompleteTasks))
        .doOnSuccess(newlyUnblockedTasks -> HandlerUtil.printIfPopulated("task(s) unblocked as a result:", newlyUnblockedTasks))
        .ignoreElement();
  }

  private Single<Set<Task>> findTasksBlockedBy(Set<Task> tasks) {
    return Flowable.fromIterable(tasks)
        .flatMapSingle(task -> taskStore.value().allTasksBlockedBy(task).firstOrError())
        .<ImmutableSet.Builder<Task>>collect(ImmutableSet::builder, (builder, taskSet) -> builder.addAll(taskSet))
        .map(ImmutableSet.Builder::build);
  }

  private Single<Set<Task>> onlyTasksThatAreUnblocked(Single<Set<Task>> tasks) {
    return tasks.flatMapObservable(Observable::fromIterable)
        .flatMapMaybe(task -> task.isUnblocked().firstOrError().filter(isUnblocked -> isUnblocked).map(isUnblocked -> task))
        .<ImmutableSet.Builder<Task>>collect(ImmutableSet::builder, ImmutableSet.Builder::add)
        .map(ImmutableSet.Builder::build);
  }
}
