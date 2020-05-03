package tasks.cli.handlers;

import io.reactivex.Observable;
import java.util.EnumMap;
import omnia.data.structure.Pair;
import omnia.data.structure.Set;
import omnia.data.structure.immutable.ImmutableSet;
import omnia.data.structure.mutable.HashSet;
import omnia.data.structure.mutable.MutableSet;
import tasks.model.Task;

final class HandlerUtil {

  private HandlerUtil() {}

  static EnumMap<CompletedState, Set<Task>> groupByCompletionState(
      Observable<Task> tasks) {
    return tasks
        .map(task -> Pair.of(task.isCompleted().blockingFirst() ? CompletedState.COMPLETE : CompletedState.INCOMPLETE, task))
        .collectInto(
            new EnumMap<CompletedState, MutableSet<Task>>(CompletedState.class),
            (map, taskPair) -> {
              map.computeIfAbsent(taskPair.first(), c -> HashSet.create());
              map.computeIfPresent(taskPair.first(), (state, set) -> {
                set.add(taskPair.second());
                return set;
              });
            })
        .map(map -> {
          EnumMap<CompletedState, Set<Task>> newMap = new EnumMap<>(CompletedState.class);
          map.forEach((state, set) -> newMap.put(state, ImmutableSet.copyOf(set)));
          return newMap;
        })
        .blockingGet();
  }

  static String stringify(Iterable<? extends Task> tasks) {
    return Observable.fromIterable(tasks)
        .map(Object::toString)
        .flatMap(stringRep -> Observable.just("\n  ", stringRep))
        .collectInto(new StringBuilder(), StringBuilder::append)
        .map(Object::toString)
        .blockingGet();
  }

  enum CompletedState {
    COMPLETE,
    INCOMPLETE,
  }
}
