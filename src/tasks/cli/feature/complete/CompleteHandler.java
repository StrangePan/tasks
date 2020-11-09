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
import tasks.model.ObservableTask;
import tasks.model.ObservableTaskStore;

/** Business logic for the Complete command. */
public final class CompleteHandler implements ArgumentHandler<CompleteArguments> {
  private final Memoized<? extends ObservableTaskStore> taskStore;

  public CompleteHandler(Memoized<? extends ObservableTaskStore> taskStore) {
    this.taskStore = requireNonNull(taskStore);
  }

  @Override
  public Single<Output> handle(CompleteArguments arguments) {
    // Validate arguments
    if (!arguments.tasks().isPopulated()) {
      throw new HandlerException("no tasks specified");
    }

    ObservableTaskStore taskStore = this.taskStore.value();

    EnumMap<CompletedState, Set<ObservableTask>> tasksGroupedByState =
        groupByCompletionState(Observable.fromIterable(arguments.tasks()));

    Set<ObservableTask> alreadyCompletedTasks =
        tasksGroupedByState.getOrDefault(CompletedState.COMPLETE, Set.empty());
    Set<ObservableTask> incompleteTasks =
        tasksGroupedByState.getOrDefault(CompletedState.INCOMPLETE, Set.empty());

    // report tasks already completed
    printIfPopulated("task(s) already marked as completed:", alreadyCompletedTasks);

    // mark incomplete tasks as complete
    return Observable.fromIterable(incompleteTasks)
        .concatMapCompletable(task -> task.mutate(mutator -> mutator.setCompleted(true)))
        .andThen(findTasksBlockedBy(incompleteTasks))
        .compose(CompleteHandler::onlyTasksThatAreUnblocked)
        .map(
            newlyUnblockedTasks ->
                Output.builder()
                    .append(stringifyIfPopulated("task(s) marked as completed:", incompleteTasks))
                    .append(stringifyIfPopulated("task(s) unblocked as a result:", newlyUnblockedTasks))
                    .build());
  }

  private static Single<Set<ObservableTask>> findTasksBlockedBy(Set<ObservableTask> tasks) {
    return Flowable.fromIterable(tasks)
        .flatMapSingle(task -> task.query().tasksBlockedByThis().firstOrError())
        .<ImmutableSet.Builder<ObservableTask>>collect(
            ImmutableSet::builder, (builder, taskSet) -> builder.addAll(taskSet))
        .map(ImmutableSet.Builder::build);
  }

  private static Single<Set<ObservableTask>> onlyTasksThatAreUnblocked(Single<Set<ObservableTask>> tasks) {
    return tasks.flatMapObservable(Observable::fromIterable)
        .flatMapMaybe(
            task ->
                task.isUnblocked()
                    .firstOrError()
                    .filter(isUnblocked -> isUnblocked)
                    .map(isUnblocked -> task))
        .<ImmutableSet.Builder<ObservableTask>>collect(ImmutableSet::builder, ImmutableSet.Builder::add)
        .map(ImmutableSet.Builder::build);
  }
}
