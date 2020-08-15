package tasks.cli.command.remove;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import omnia.data.cache.Memoized;
import omnia.data.structure.Collection;
import tasks.cli.handlers.ArgumentHandler;
import tasks.cli.handlers.HandlerException;
import tasks.model.Task;
import tasks.model.TaskStore;

/** Business logic for the Remove command. */
public final class RemoveHandler implements ArgumentHandler<RemoveArguments> {
  private final Memoized<TaskStore> taskStore;

  public RemoveHandler(Memoized<TaskStore> taskStore) {
    this.taskStore = requireNonNull(taskStore);
  }

  @Override
  public Completable handle(RemoveArguments arguments) {
    Collection<Task> tasksToDelete = arguments.tasks();

    // Validate arguments
    if (!tasksToDelete.isPopulated()) {
      throw new HandlerException("no tasks specified");
    }

    TaskStore taskStore = this.taskStore.value();

    return Observable.fromIterable(tasksToDelete)
        .concatMapCompletable(taskStore::deleteTask)
        .andThen(taskStore.writeToDisk())
        .andThen(Single.just("tasks removed:" + stringify(tasksToDelete)))
        .flatMapCompletable(report -> Completable.fromAction(() -> System.out.println(report)));
  }

  private static String stringify(Collection<Task> tasks) {
    return tasks.stream().map(Task::toString).map(line -> "\n  " + line).collect(joining());
  }
}
