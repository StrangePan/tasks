package tasks.cli.command.remove;

import static java.util.Objects.requireNonNull;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import omnia.cli.out.Output;
import omnia.data.cache.Memoized;
import omnia.data.structure.Collection;
import omnia.data.structure.tuple.Tuple;
import tasks.cli.handlers.ArgumentHandler;
import tasks.cli.handlers.HandlerException;
import tasks.cli.handlers.HandlerUtil;
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

    return Single.just(tasksToDelete)
        .map(tasks -> Tuple.of(tasks, HandlerUtil.stringifyIfPopulated("tasks deleted:", tasks)))
        .flatMap(
            tasksAndReport ->
                Observable.fromIterable(tasksAndReport.first())
                    .concatMapCompletable(taskStore::deleteTask)
                    .andThen(taskStore.writeToDisk())
                    .andThen(Single.just(tasksAndReport.second())))
        .filter(Output::isPopulated)
        .map(Output::renderForTerminal)
        .doOnSuccess(System.out::print)
        .ignoreElement();
  }
}
