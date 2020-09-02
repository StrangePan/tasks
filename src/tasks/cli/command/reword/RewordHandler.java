package tasks.cli.command.reword;

import static java.util.Objects.requireNonNull;

import io.reactivex.Completable;
import io.reactivex.Single;
import omnia.data.cache.Memoized;
import tasks.cli.handlers.ArgumentHandler;
import tasks.cli.handlers.HandlerException;
import tasks.model.TaskStore;

public final class RewordHandler implements ArgumentHandler<RewordArguments> {
  private final Memoized<TaskStore> taskStore;

  public RewordHandler(Memoized<TaskStore> taskStore) {
    this.taskStore = requireNonNull(taskStore);
  }

  @Override
  public Completable handle(RewordArguments arguments) {
    String description = arguments.description().trim();
    if (description.isEmpty()) {
      throw new HandlerException("description cannot be empty or whitespace only");
    }

    return Single.fromCallable(taskStore::value)
        .flatMapCompletable(
            store ->
                store.mutateTask(
                    arguments.targetTask(),
                    mutator -> mutator.setLabel(arguments.description())))
        .andThen(taskStore.value().writeToDisk())
        .doOnComplete(
            () ->
                System.out.println(
                    "Updated description: " + arguments.targetTask().render().render()));
  }
}
