package tasks.cli.feature.reword;

import static java.util.Objects.requireNonNull;

import io.reactivex.rxjava3.core.Single;
import omnia.cli.out.Output;
import omnia.data.cache.Memoized;
import omnia.data.structure.tuple.Triple;
import tasks.cli.command.common.CommonArguments;
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
  public Single<Output> handle(CommonArguments<? extends RewordArguments> arguments) {
    String description = arguments.specificArguments().description().trim();
    if (description.isEmpty()) {
      throw new HandlerException("description cannot be empty or whitespace only");
    }

    return Single.fromCallable(taskStore::value)
        .flatMap(
            store -> store
                .mutateTask(
                    arguments.specificArguments().targetTask(),
                    mutator -> mutator.setLabel(arguments.specificArguments().description()))
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
