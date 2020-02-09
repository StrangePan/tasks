package tasks.cli.handlers;

import static java.util.stream.Collectors.joining;

import io.reactivex.Observable;
import java.util.EnumMap;
import omnia.data.structure.Collection;
import omnia.data.structure.DirectedGraph;
import omnia.data.structure.Set;
import omnia.data.structure.immutable.ImmutableSet;
import tasks.Task;
import tasks.cli.CliTaskId;
import tasks.io.File;
import tasks.io.TaskStore;
import tasks.model.impl.TaskStoreImpl;

final class HandlerUtil {

  private HandlerUtil() {}

  private static final String FILE_NAME = "asdf";
  private static final Object TASK_NOT_FOUND = new Object();

  private static File file() {
    return File.fromPath(FILE_NAME);
  }

  static DirectedGraph<Task> loadTasks() {
    return new TaskStore(file()).retrieveBlocking();
  }

  static tasks.model.TaskStore loadTaskStore() {
    return new TaskStoreImpl(FILE_NAME);
  }

  static void validateTasksIds(tasks.model.TaskStore store, Iterable<CliTaskId> ids) {
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

  static Observable<tasks.model.Task> toTasks(tasks.model.TaskStore store, Iterable<CliTaskId> ids) {
    return Observable.fromIterable(ids)
        .flatMapMaybe(id -> store.lookUpById(id.asLong()));
  }

  static void writeTasks(DirectedGraph<Task> tasks) {
    new TaskStore(file()).storeBlocking(tasks);
  }

  static String stringifyContents(Collection<?> collection) {
    return collection.stream().map(Object::toString).collect(joining(","));
  }

  static EnumMap<CompletedState, Set<tasks.model.Task>> groupByCompletionState(
      Observable<tasks.model.Task> tasks) {
    return tasks
        .groupBy(task -> task.isCompleted().blockingFirst()
            ? CompletedState.COMPLETE
            : CompletedState.INCOMPLETE)
        .reduce(
            new EnumMap<CompletedState, Set<tasks.model.Task>>(CompletedState.class),
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
