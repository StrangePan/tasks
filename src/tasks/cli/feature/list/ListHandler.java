package tasks.cli.feature.list;

import static java.util.Objects.requireNonNull;
import static tasks.cli.handler.HandlerUtil.stringifyIfPopulated;

import io.reactivex.Observable;
import io.reactivex.Single;
import omnia.cli.out.Output;
import omnia.data.cache.Memoized;
import omnia.data.structure.tuple.Triple;
import omnia.data.structure.tuple.Tuple;
import tasks.cli.handler.ArgumentHandler;
import tasks.model.ObservableTaskStore;

/** Business logic for the List command. */
public final class ListHandler implements ArgumentHandler<ListArguments> {
  private final Memoized<? extends ObservableTaskStore> taskStore;

  public ListHandler(Memoized<? extends ObservableTaskStore> taskStore) {
    this.taskStore = requireNonNull(taskStore);
  }

  @Override
  public Single<Output> handle(ListArguments arguments) {
    return Single.fromCallable(taskStore::value)
        .flatMapObservable(ObservableTaskStore::observe)
        .firstOrError()
        .flatMapObservable(
            store -> Observable.just(
                Tuple.of(
                    arguments.isUnblockedSet(),
                    "unblocked tasks:",
                    Single.fromCallable(store::allOpenTasksWithoutOpenBlockers)),
                Tuple.of(
                    arguments.isBlockedSet(),
                    "blocked tasks:",
                    Single.fromCallable(store::allOpenTasksWithOpenBlockers)),
                Tuple.of(
                    arguments.isCompletedSet(),
                    "completed tasks:",
                    Single.fromCallable(store::allCompletedTasks))))
        .filter(Triple::first)
        .map(Triple::dropFirst)
        .concatMapEager(
            couple -> couple.second().map(tasks -> Tuple.of(couple.first(), tasks)).toObservable())
        .map(couple -> stringifyIfPopulated(couple.first(), couple.second()))
        .collectInto(Output.builder(), Output.Builder::appendLine)
        .map(Output.Builder::build);
  }
}
