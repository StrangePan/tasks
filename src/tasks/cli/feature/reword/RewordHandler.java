package tasks.cli.feature.reword;

import static java.util.Objects.requireNonNull;

import io.reactivex.Single;
import omnia.cli.out.Output;
import omnia.data.cache.Memoized;
import omnia.data.structure.tuple.Triple;
import tasks.cli.handler.ArgumentHandler;
import tasks.cli.handler.HandlerException;
import tasks.model.Task;
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
        .flatMap(
            store -> store
                .mutateTask(
                    arguments.targetTask(), mutator -> mutator.setLabel(arguments.description()))
                .map(Triple::third))
        .map(Task::render)
        .map(
            taskOutput ->
                Output.builder()
                    .append("Updated description: ")
                    .append(taskOutput)
                    .appendLine()
                    .build());
  }
}
