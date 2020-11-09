package tasks.cli.feature.reword;

import static java.util.Objects.requireNonNull;

import io.reactivex.Single;
import omnia.cli.out.Output;
import omnia.data.cache.Memoized;
import tasks.cli.handler.ArgumentHandler;
import tasks.cli.handler.HandlerException;
import tasks.model.ObservableTask;
import tasks.model.ObservableTaskStore;

public final class RewordHandler implements ArgumentHandler<RewordArguments> {
  private final Memoized<? extends ObservableTaskStore> taskStore;

  public RewordHandler(Memoized<? extends ObservableTaskStore> taskStore) {
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
        .andThen(Single.just(arguments.targetTask()))
        .map(ObservableTask::render)
        .map(
            taskOutput ->
                Output.builder().append("Updated description: ").append(taskOutput).build());
  }
}
