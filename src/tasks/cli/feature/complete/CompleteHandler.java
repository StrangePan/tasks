package tasks.cli.feature.complete;

import static java.util.Objects.requireNonNull;
import static omnia.algorithm.SetAlgorithms.differenceBetween;
import static omnia.algorithm.SetAlgorithms.unionOf;
import static tasks.cli.handler.HandlerUtil.stringifyIfPopulated;

import io.reactivex.Observable;
import io.reactivex.Single;
import java.util.Optional;
import omnia.cli.out.Output;
import omnia.data.cache.Memoized;
import omnia.data.stream.Collectors;
import omnia.data.structure.immutable.ImmutableSet;
import omnia.data.structure.tuple.Tuplet;
import tasks.cli.handler.ArgumentHandler;
import tasks.cli.handler.HandlerException;
import tasks.model.Task;
import tasks.model.ObservableTaskStore;
import tasks.model.TaskId;

/** Business logic for the Complete command. */
public final class CompleteHandler implements ArgumentHandler<CompleteArguments> {
  private final Memoized<? extends ObservableTaskStore> taskStore;

  public CompleteHandler(Memoized<? extends ObservableTaskStore> taskStore) {
    this.taskStore = requireNonNull(taskStore);
  }

  @Override
  public Single<Output> handle(CompleteArguments arguments) {
    // Validate arguments
    if (!arguments.tasks().isPopulated()) {
      throw new HandlerException("no tasks specified");
    }

    ObservableTaskStore taskStore = this.taskStore.value();

    return Observable.fromIterable(arguments.tasks())
        .flatMapSingle(task -> taskStore.mutateTask(task, mutator -> mutator.setCompleted(true)))
        .reduce(
            Tuplet.of(
                ImmutableSet.<TaskId>builder(), // tasks that were already completed
                ImmutableSet.<TaskId>builder(), // tasks that were marked as completed
                ImmutableSet.<TaskId>builder()), // tasks that became unblocked
            (builders, mutationResult) -> {
              boolean becameCompleted =
                  mutationResult.first()
                      .lookUpById(mutationResult.third().id())
                      .map(task -> !task.isCompleted())
                      .orElse(false);
              (becameCompleted ? builders.second() : builders.first())
                  .add(mutationResult.third().id());

              if (becameCompleted) {
                mutationResult.third()
                    .blockedTasks()
                    .stream()
                    .filter(Task::isUnblocked)
                    .filter(task -> !task.isCompleted())
                    .map(Task::id)
                    .forEach(builders.third()::add);
              }

              return builders;
            })
        .map(groupedTasks -> groupedTasks.map(ImmutableSet.Builder::build))
        .map(
            groupedTasks -> Tuplet.of(
                groupedTasks.first(),
                groupedTasks.second(),
                    ImmutableSet.copyOf(
                        differenceBetween(
                            groupedTasks.third(),
                            unionOf(groupedTasks.first(), groupedTasks.second())))))
        .flatMap(
            groupedTasks -> taskStore.observe()
                .firstOrError()
                .map(
                    store -> groupedTasks.map(
                        list -> list.stream()
                            .map(store::lookUpById)
                            .map(Optional::orElseThrow)
                            .collect(Collectors.toImmutableSet()))))
        .flatMapObservable(Observable::fromIterable)
        .zipWith(
            Observable.just(
                "task(s) already completed:",
                "task(s) completed:",
                "task(s) unblocked as a result:"),
            (groupedTasks, header) -> stringifyIfPopulated(header, groupedTasks))
        .collectInto(Output.builder(), Output.Builder::append)
        .map(Output.Builder::build);
  }
}
