package tasks.cli.feature.reopen;

import static java.util.Objects.requireNonNull;
import static omnia.data.stream.Collectors.toImmutableSet;
import static tasks.cli.handler.HandlerUtil.stringifyIfPopulated;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import java.util.Optional;
import omnia.cli.out.Output;
import omnia.data.cache.Memoized;
import omnia.data.structure.immutable.ImmutableSet;
import omnia.data.structure.tuple.Tuplet;
import tasks.cli.command.common.CommonArguments;
import tasks.cli.handler.ArgumentHandler;
import tasks.cli.handler.HandlerException;
import tasks.model.Task;
import tasks.model.ObservableTaskStore;
import tasks.model.TaskId;
import tasks.model.TaskMutator;

/** Business logic for the Reopen command. */
public final class ReopenHandler implements ArgumentHandler<ReopenArguments> {
  private final Memoized<? extends ObservableTaskStore> taskStore;

  public ReopenHandler(Memoized<? extends ObservableTaskStore> taskStore) {
    this.taskStore = requireNonNull(taskStore);
  }

  @Override
  public Single<Output> handle(CommonArguments<? extends ReopenArguments> arguments) {
    // Validate arguments
    if (!arguments.specificArguments().tasks().isPopulated()) {
      throw new HandlerException("no tasks specified");
    }

    ObservableTaskStore taskStore = this.taskStore.value();

    return Observable.fromIterable(arguments.specificArguments().tasks())
        .flatMapSingle(task -> taskStore.mutateTask(task, TaskMutator::reopen))
        .reduce(
            Tuplet.of(
                ImmutableSet.<TaskId>builder(), // tasks that were already open
                ImmutableSet.<TaskId>builder(), // tasks that were reopened
                ImmutableSet.<TaskId>builder()), // tasks that became blocked
            (builders, mutationResult) -> {
              boolean becameOpen =
                  mutationResult.third().status().isOpen()
                  && mutationResult.first()
                      .lookUpById(mutationResult.third().id())
                      .map(task -> task.status().isCompleted())
                      .orElse(false);
              (becameOpen ? builders.second() : builders.first())
                  .add(mutationResult.third().id());

              if (becameOpen) {
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
                "task(s) already open:",
                "task(s) reopened:",
                "task(s) blocked as a result:"),
            (groupedTasks, header) -> stringifyIfPopulated(header, groupedTasks))
        .collectInto(Output.builder(), Output.Builder::append)
        .map(Output.Builder::build);
  }
}
