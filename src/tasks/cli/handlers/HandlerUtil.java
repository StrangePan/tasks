package tasks.cli.handlers;

import static java.util.stream.Collectors.joining;

import io.reactivex.Observable;
import java.util.EnumMap;
import omnia.data.structure.Collection;
import omnia.data.structure.Set;
import omnia.data.structure.immutable.ImmutableSet;
import tasks.cli.CliTaskId;
import tasks.model.Task;
import tasks.model.TaskStore;
import tasks.model.impl.TaskStoreImpl;

final class HandlerUtil {

  private HandlerUtil() {}

  private static final String FILE_NAME = "asdf";
  private static final Object TASK_NOT_FOUND = new Object();

  static TaskStore loadTaskStore() {
    return new TaskStoreImpl(FILE_NAME);
  }

  static void validateTasksIds(TaskStore store, Iterable<CliTaskId> ids) {
    Set<CliTaskId> invalidTaskIds =
        ImmutableSet.copyOf(
            Observable.fromIterable(ids)
                .flatMapMaybe(id ->
                    store.lookUpById(id.asLong())
                        .cast(Object.class)
                        .defaultIfEmpty(TASK_NOT_FOUND)
                        .filter(o -> o == TASK_NOT_FOUND)
                        .map(unused -> id))
                .blockingIterable());

    if (invalidTaskIds.isPopulated()) {
      throw new HandlerException(
          "unrecognized tasks specified: " + stringifyContents(invalidTaskIds));
    }
  }

  static Observable<Task> toTasks(TaskStore store, Iterable<CliTaskId> ids) {
    return Observable.fromIterable(ids)
        .flatMapMaybe(id -> store.lookUpById(id.asLong()));
  }

  static String stringifyContents(Collection<?> collection) {
    return collection.stream().map(Object::toString).collect(joining(","));
  }

  static EnumMap<CompletedState, Set<Task>> groupByCompletionState(
      Observable<Task> tasks) {
    return tasks
        .groupBy(task -> task.isCompleted().blockingFirst()
            ? CompletedState.COMPLETE
            : CompletedState.INCOMPLETE)
        .reduce(
            new EnumMap<CompletedState, Set<Task>>(CompletedState.class),
            (map, taskSet) -> {
              map.put(taskSet.getKey(), ImmutableSet.copyOf(taskSet.blockingIterable()));
              return map;
            })
        .blockingGet();
  }

  enum CompletedState {
    COMPLETE,
    INCOMPLETE,
  }
}
