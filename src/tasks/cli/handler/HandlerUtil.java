package tasks.cli.handler;

import static java.util.stream.Collectors.joining;
import static omnia.data.stream.Collectors.toImmutableSet;

import io.reactivex.Observable;
import java.util.EnumMap;
import java.util.Optional;
import omnia.algorithm.SetAlgorithms;
import omnia.cli.out.Output;
import omnia.contract.Countable;
import omnia.data.structure.Collection;
import omnia.data.structure.Set;
import omnia.data.structure.immutable.ImmutableSet;
import omnia.data.structure.mutable.HashSet;
import omnia.data.structure.mutable.MutableSet;
import omnia.data.structure.tuple.Tuple;
import tasks.model.Task;

public final class HandlerUtil {

  private HandlerUtil() {}

  public static EnumMap<CompletedState, Set<Task>> groupByCompletionState(
      Observable<Task> tasks) {
    return tasks
        .map(task ->
            Tuple.of(
                task.isCompleted().blockingFirst()
                    ? CompletedState.COMPLETE
                    : CompletedState.INCOMPLETE,
                task))
        .collectInto(
            new EnumMap<CompletedState, MutableSet<Task>>(CompletedState.class),
            (map, taskCouple) -> {
              map.computeIfAbsent(taskCouple.first(), c -> HashSet.create());
              map.computeIfPresent(taskCouple.first(), (state, set) -> {
                set.add(taskCouple.second());
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

  public static void printIfPopulated(String prefix, Collection<Task> tasks) {
    Optional.of(stringifyIfPopulated(prefix, tasks))
        .filter(Output::isPopulated)
        .map(Output::render)
        .ifPresent(System.out::print);
  }

  public static Output stringifyIfPopulated(String prefix, Collection<Task> tasks) {
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

  public static void verifyTasksAreMutuallyExclusive(
      String failurePrefix, Collection<? extends Task> first, Collection<? extends Task> second) {
    Optional.of(
        SetAlgorithms.intersectionOf(ImmutableSet.copyOf(first), ImmutableSet.copyOf(second))
            .stream()
            .collect(toImmutableSet()))
        .filter(Countable::isPopulated)
        .map(ambiguousTasks -> ambiguousTasks.stream().map(Object::toString).collect(joining(", ")))
        .map(ambiguousTasks -> failurePrefix + ambiguousTasks)
        .ifPresent(message -> {
          throw new HandlerException(message);
        });
  }

  public enum CompletedState {
    COMPLETE,
    INCOMPLETE,
  }
}
