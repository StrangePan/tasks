package tasks.cli.command.list;

import static java.util.Objects.requireNonNull;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import omnia.data.cache.Memoized;
import omnia.data.structure.tuple.Triple;
import omnia.data.structure.tuple.Tuple;
import tasks.cli.handlers.ArgumentHandler;
import tasks.cli.handlers.HandlerUtil;
import tasks.model.TaskStore;

/** Business logic for the List command. */
public final class ListHandler implements ArgumentHandler<ListArguments> {
  private final Memoized<TaskStore> taskStore;

  public ListHandler(Memoized<TaskStore> taskStore) {
    this.taskStore = requireNonNull(taskStore);
  }

  @Override
  public Completable handle(ListArguments arguments) {
    return Single.fromCallable(taskStore::value)
        .flatMapObservable(
            store -> Observable.just(
                Tuple.of(
                    arguments.isUnblockedSet(),
                    "unblocked tasks:",
                    store.allTasksWithoutOpenBlockers().firstOrError()),
                Tuple.of(
                    arguments.isBlockedSet(),
                    "blocked tasks:",
                    store.allTasksWithOpenBlockers().firstOrError()),
                Tuple.of(
                    arguments.isCompletedSet(),
                    "completed tasks:",
                    store.completedTasks().firstOrError())))
        .filter(Triple::first)
        .map(Triple::dropFirst)
        .concatMapEager(
            couple -> couple.second().map(tasks -> Tuple.of(couple.first(), tasks)).toObservable())
        .doOnNext(couple -> HandlerUtil.printIfPopulated(couple.first(), couple.second()))
        .ignoreElements();
  }
}
