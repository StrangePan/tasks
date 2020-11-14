package tasks.cli.feature.start;

import static java.util.Objects.requireNonNull;
import static omnia.data.stream.Collectors.toImmutableSet;
import static tasks.cli.handler.HandlerUtil.stringifyIfPopulated;

import io.reactivex.Observable;
import io.reactivex.Single;
import java.util.Optional;
import omnia.cli.out.Output;
import omnia.data.cache.Memoized;
import omnia.data.structure.immutable.ImmutableSet;
import omnia.data.structure.tuple.Tuplet;
import tasks.cli.handler.ArgumentHandler;
import tasks.cli.handler.HandlerException;
import tasks.model.ObservableTaskStore;
import tasks.model.Task;
import tasks.model.TaskId;
import tasks.model.TaskMutator;

/** Business logic for the Start command. */
public final class StartHandler implements ArgumentHandler<StartArguments> {
  private final Memoized<? extends ObservableTaskStore> taskStore;

  public StartHandler(Memoized<? extends ObservableTaskStore> taskStore) {
    this.taskStore = requireNonNull(taskStore);
  }

  @Override
  public Single<Output> handle(StartArguments arguments) {
    // Validate arguments
    if (!arguments.tasks().isPopulated()) {
      throw new HandlerException("no tasks specified");
    }

    ObservableTaskStore taskStore = this.taskStore.value();

    return Observable.fromIterable(arguments.tasks())
        .flatMapSingle(task -> taskStore.mutateTask(task, TaskMutator::start))
        .reduce(
            Tuplet.of(
                ImmutableSet.<TaskId>builder(), // tasks that were already started
                ImmutableSet.<TaskId>builder(), // tasks that were marked as started
                ImmutableSet.<TaskId>builder()), // tasks that became blocked
            (builders, mutationResult) -> {
              Task.Status beforeStatus =
                  mutationResult.first()
                      .lookUpById(mutationResult.third().id())
                      .map(Task::status)
                      .orElseThrow();
              boolean becameStarted = !beforeStatus.isStarted();
              (becameStarted ? builders.second() : builders.first())
                  .add(mutationResult.third().id());

              if (becameStarted && beforeStatus.isCompleted()) {
                mutationResult.third()
                    .blockedTasks()
                    .stream()
                    .map(Task::id)
                    .forEach(builders.third()::add);
              }

              return builders;
            })
        .map(groupedTasks -> groupedTasks.map(ImmutableSet.Builder::build))
        .flatMap(
            groupedTasks -> taskStore.observe()
                .firstOrError()
                .map(
                    store -> groupedTasks.map(
                        list -> list.stream()
                            .map(store::lookUpById)
                            .map(Optional::orElseThrow)
                            .collect(toImmutableSet()))))
        .map(
            groupedTasks -> Tuplet.of(
                groupedTasks.first(),
                groupedTasks.second(),
                groupedTasks.third()
                    .stream()
                    .filter(task -> !task.status().isCompleted())
                    .collect(toImmutableSet())))
        .flatMapObservable(Observable::fromIterable)
        .zipWith(
            Observable.just(
                "task(s) already started:",
                "task(s) started:",
                "task(s) blocked as a result:"),
            (groupedTasks, header) -> stringifyIfPopulated(header, groupedTasks))
        .collectInto(Output.builder(), Output.Builder::append)
        .map(Output.Builder::build);
  }
}
