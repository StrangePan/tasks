package tasks.cli.handlers;

import static java.util.Objects.requireNonNull;

import io.reactivex.Completable;
import omnia.data.cache.Memoized;
import omnia.data.structure.immutable.ImmutableSet;
import tasks.cli.arg.ListArguments;
import tasks.model.TaskStore;

public final class ListHandler implements ArgumentHandler<ListArguments> {
  private final Memoized<TaskStore> taskStore;

  ListHandler(Memoized<TaskStore> taskStore) {
    this.taskStore = requireNonNull(taskStore);
  }

  @Override
  public Completable handle(ListArguments arguments) {
    TaskStore taskStore = this.taskStore.value();

    HandlerUtil.printIfPopulated(
        "unblocked tasks:",
        arguments.isUnblockedSet()
            ? taskStore.allTasksWithoutOpenBlockers().blockingFirst()
            : ImmutableSet.empty());
    HandlerUtil.printIfPopulated(
        "blocked tasks:",
        arguments.isBlockedSet()
            ? taskStore.allTasksWithOpenBlockers().blockingFirst()
            : ImmutableSet.empty());
    HandlerUtil.printIfPopulated(
        "completed tasks:",
        arguments.isCompletedSet()
            ? taskStore.completedTasks().blockingFirst()
            : ImmutableSet.empty());

    return Completable.complete();
  }
}
