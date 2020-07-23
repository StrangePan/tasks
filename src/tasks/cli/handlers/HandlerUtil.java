package tasks.cli.handlers;

import io.reactivex.Observable;
import java.util.EnumMap;
import omnia.cli.out.Output;
import omnia.data.structure.Collection;
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

  static void printIfPopulated(String prefix, Collection<Task> tasks) {
    if (tasks.isPopulated()) {
      System.out.print(stringifyIfPopulated(prefix, tasks).renderForTerminal());
    }
  }

  static Output stringifyIfPopulated(String prefix, Collection<Task> tasks) {
    return tasks.isPopulated()
        ? Output.builder()
            .color(Output.Color16.LIGHT_MAGENTA)
            .appendLine(prefix)
            .defaultColor()
            .appendLine(stringify(tasks), 2)
            .build()
        : Output.empty();
  }

  private static Output stringify(Iterable<? extends Task> tasks) {
    return Observable.fromIterable(tasks)
        .map(Task::render)
        .collectInto(Output.builder(), Output.Builder::appendLine)
        .map(Output.Builder::build)
        .blockingGet();
  }

  enum CompletedState {
    COMPLETE,
    INCOMPLETE,
  }
}
