package tasks.cli.command.reword;

import static java.util.Objects.requireNonNull;

import io.reactivex.Single;
import omnia.cli.out.Output;
import omnia.data.cache.Memoized;
import tasks.cli.handlers.ArgumentHandler;
import tasks.cli.handlers.HandlerException;
import tasks.model.Task;
import tasks.model.TaskStore;

public final class RewordHandler implements ArgumentHandler<RewordArguments> {
  private final Memoized<TaskStore> taskStore;

  public RewordHandler(Memoized<TaskStore> taskStore) {
    this.taskStore = requireNonNull(taskStore);
  }

  @Override
  public Single<Output> handle(RewordArguments arguments) {
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
        .andThen(Single.just(arguments.targetTask()))
        .map(Task::render)
        .map(
            taskOutput ->
                Output.builder().append("Updated description: ").append(taskOutput).build());
  }
}
