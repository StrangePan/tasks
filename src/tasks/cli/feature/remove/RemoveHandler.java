package tasks.cli.feature.remove;

import static java.util.Objects.requireNonNull;

import io.reactivex.Observable;
import io.reactivex.Single;
import omnia.cli.out.Output;
import omnia.data.cache.Memoized;
import omnia.data.structure.Collection;
import omnia.data.structure.tuple.Tuple;
import tasks.cli.handler.ArgumentHandler;
import tasks.cli.handler.HandlerException;
import tasks.cli.handler.HandlerUtil;
import tasks.model.Task;
import tasks.model.ObservableTaskStore;

/** Business logic for the Remove command. */
public final class RemoveHandler implements ArgumentHandler<RemoveArguments> {
  private final Memoized<? extends ObservableTaskStore> taskStore;

  public RemoveHandler(Memoized<? extends ObservableTaskStore> taskStore) {
    this.taskStore = requireNonNull(taskStore);
  }

  @Override
  public Single<Output> handle(RemoveArguments arguments) {
    Collection<Task> tasksToDelete = arguments.tasks();

    // Validate arguments
    if (!tasksToDelete.isPopulated()) {
      throw new HandlerException("no tasks specified");
    }

    return Single.just(tasksToDelete)
        .map(tasks -> Tuple.of(tasks, HandlerUtil.stringifyIfPopulated("tasks deleted:", tasks)))
        .flatMap(
            tasksAndReport ->
                Observable.fromIterable(tasksAndReport.first())
                    .concatMapCompletable(task -> taskStore.value().deleteTask(task))
                    .andThen(Single.just(tasksAndReport.second())));
  }
}
