package tasks.cli.handlers;

import static java.util.Objects.requireNonNull;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import omnia.data.cache.Memoized;
import omnia.data.structure.Set;
import tasks.cli.arg.ListArguments;
import tasks.model.Task;
import tasks.model.TaskStore;

public final class ListHandler implements ArgumentHandler<ListArguments> {
  private final Memoized<TaskStore> taskStore;

  ListHandler(Memoized<TaskStore> taskStore) {
    this.taskStore = requireNonNull(taskStore);
  }

  @Override
  public Completable handle(ListArguments arguments) {
    TaskStore taskStore = this.taskStore.value();

    print(
        "unblocked tasks:",
        arguments.isUnblockedSet() ?  stringify(taskStore.allTasksWithoutOpenBlockers()) : "");

    print(
        "blocked tasks:",
        arguments.isBlockedSet() ? stringify(taskStore.allTasksWithOpenBlockers()) : "");

    print(
        "completed tasks:",
        arguments.isCompletedSet() ? stringify(taskStore.completedTasks()) : "");

    return Completable.complete();
  }

  private static String stringify(Flowable<Set<Task>> tasks) {
    return HandlerUtil.stringify(tasks.blockingFirst());
  }

  private static void print(String prefix, String message) {
    if (!message.isEmpty()) {
      System.out.println(prefix + message);
    }
  }
}
