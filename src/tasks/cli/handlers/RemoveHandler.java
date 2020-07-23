package tasks.cli.handlers;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

import io.reactivex.Completable;
import io.reactivex.Observable;
import omnia.data.cache.Memoized;
import omnia.data.structure.Collection;
import tasks.cli.arg.RemoveArguments;
import tasks.model.Task;
import tasks.model.TaskStore;

public final class RemoveHandler implements ArgumentHandler<RemoveArguments> {
  private final Memoized<TaskStore> taskStore;

  RemoveHandler(Memoized<TaskStore> taskStore) {
    this.taskStore = requireNonNull(taskStore);
  }

  @Override
  public Completable handle(RemoveArguments arguments) {
    Collection<Task> tasksToDelete = arguments.tasks();

    // Validate arguments
    if (!tasksToDelete.isPopulated()) {
      throw new HandlerException("no tasks specified");
    }

    String report = "tasks removed:" + stringify(tasksToDelete);

    TaskStore taskStore = this.taskStore.value();

    Observable.fromIterable(tasksToDelete)
        .concatMapCompletable(taskStore::deleteTask)
        .blockingAwait();

    taskStore.writeToDisk().blockingAwait();

    System.out.println(report);
    return Completable.complete();
  }

  private static String stringify(Collection<Task> tasks) {
    return tasks.stream().map(Task::toString).map(line -> "\n  " + line).collect(joining());
  }
}
