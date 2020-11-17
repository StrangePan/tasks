package tasks.cli.feature.stop;

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
import tasks.cli.command.common.CommonArguments;
import tasks.cli.handler.ArgumentHandler;
import tasks.cli.handler.HandlerException;
import tasks.model.ObservableTaskStore;
import tasks.model.TaskId;
import tasks.model.TaskMutator;

/** Business logic for the Stop command. */
public final class StopHandler implements ArgumentHandler<StopArguments> {
  private final Memoized<? extends ObservableTaskStore> taskStore;

  public StopHandler(Memoized<? extends ObservableTaskStore> taskStore) {
    this.taskStore = requireNonNull(taskStore);
  }

  @Override
  public Single<Output> handle(CommonArguments<? extends StopArguments> arguments) {
    // Validate arguments
    if (!arguments.specificArguments().tasks().isPopulated()) {
      throw new HandlerException("no tasks specified");
    }

    ObservableTaskStore taskStore = this.taskStore.value();

    return Observable.fromIterable(arguments.specificArguments().tasks())
        .flatMapSingle(task -> taskStore.mutateTask(task, TaskMutator::stop))
        .reduce(
            Tuplet.of(
                ImmutableSet.<TaskId>builder(), // tasks that could not be stopped
                ImmutableSet.<TaskId>builder()), // tasks that were stopped
            (builders, mutationResult) -> {
              boolean becameStopped =
                  mutationResult.third().status().isOpen()
                  && mutationResult.first()
                      .lookUpById(mutationResult.third().id())
                      .map(task -> !task.status().isOpen())
                      .orElse(false);
              (becameStopped ? builders.second() : builders.first())
                  .add(mutationResult.third().id());

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
        .flatMapObservable(Observable::fromIterable)
        .zipWith(
            Observable.just(
                "task(s) already stopped:",
                "task(s) stopped:"),
            (groupedTasks, header) -> stringifyIfPopulated(header, groupedTasks))
        .collectInto(Output.builder(), Output.Builder::append)
        .map(Output.Builder::build);
  }
}
