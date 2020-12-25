package tasks.cli.feature.list;

import static java.util.Objects.requireNonNull;
import static tasks.cli.handler.HandlerUtil.stringifyIfPopulated;
import static tasks.util.rx.Observables.toImmutableSet;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import omnia.cli.out.Output;
import omnia.data.cache.Memoized;
import omnia.data.structure.immutable.ImmutableSet;
import omnia.data.structure.tuple.Triple;
import omnia.data.structure.tuple.Tuple;
import tasks.cli.command.common.CommonArguments;
import tasks.cli.handler.ArgumentHandler;
import tasks.model.ObservableTaskStore;
import tasks.model.Task;

/** Business logic for the List command. */
public final class ListHandler implements ArgumentHandler<ListArguments> {
  private final Memoized<? extends ObservableTaskStore> taskStore;

  public ListHandler(Memoized<? extends ObservableTaskStore> taskStore) {
    this.taskStore = requireNonNull(taskStore);
  }

  @Override
  public Single<Output> handle(CommonArguments<? extends ListArguments> arguments) {
    return Single.fromCallable(taskStore::value)
        .flatMapObservable(ObservableTaskStore::observe)
        .firstOrError()
        .flatMapObservable(
            store -> Observable.just(
                Tuple.of(
                    arguments.specificArguments().isUnblockedSet(),
                    "unblocked tasks:",
                    Single.fromCallable(store::allOpenTasksWithoutOpenBlockers)),
                Tuple.of(
                    arguments.specificArguments().isBlockedSet(),
                    "blocked tasks:",
                    Single.fromCallable(store::allOpenTasksWithOpenBlockers)),
                Tuple.of(
                    arguments.specificArguments().isCompletedSet(),
                    "completed tasks:",
                    Single.fromCallable(store::allCompletedTasks))))
        .filter(Triple::first)
        .map(Triple::dropFirst)
        .map(
            couple ->
                arguments.specificArguments().isStartedSet()
                    ? couple.mapSecond(ListHandler::filterOutUnstartedTasks)
                    : couple)
        .concatMapEager(
            couple -> couple.second().map(tasks -> Tuple.of(couple.first(), tasks)).toObservable())
        .map(couple -> stringifyIfPopulated(couple.first(), couple.second()))
        .collectInto(Output.builder(), Output.Builder::appendLine)
        .map(Output.Builder::build);
  }

  private static <T extends Task> Single<ImmutableSet<T>> filterOutUnstartedTasks(
      Single<? extends ImmutableSet<? extends T>> tasks) {
    return tasks.flatMapObservable(Observable::fromIterable)
        .filter(task -> task.status().isStarted())
        .map(t -> (T) t)
        .to(toImmutableSet());
  }
}
