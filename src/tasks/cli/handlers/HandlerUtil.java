package tasks.cli.handlers;

import static java.util.stream.Collectors.joining;

import omnia.data.structure.Collection;
import omnia.data.structure.DirectedGraph;
import tasks.Task;
import tasks.io.File;
import tasks.io.TaskStore;
import tasks.model.impl.TaskStoreImpl;

final class HandlerUtil {

  private HandlerUtil() {}

  private static final String FILE_NAME = "asdf";

  private static File file() {
    return File.fromPath(FILE_NAME);
  }

  static DirectedGraph<Task> loadTasks() {
    return new TaskStore(file()).retrieveBlocking();
  }

  static tasks.model.TaskStore loadTaskStore() {
    return new TaskStoreImpl(FILE_NAME);
  }

  static void writeTasks(DirectedGraph<Task> tasks) {
    new TaskStore(file()).storeBlocking(tasks);
  }

  static String stringifyContents(Collection<?> collection) {
    return collection.stream().map(Object::toString).collect(joining(","));
  }
}
