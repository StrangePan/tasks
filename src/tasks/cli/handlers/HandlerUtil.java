package tasks.cli.handlers;

import static java.util.stream.Collectors.joining;

import omnia.data.structure.Collection;
import omnia.data.structure.DirectedGraph;
import tasks.Task;
import tasks.io.File;
import tasks.io.TaskStore;

final class HandlerUtil {

  private HandlerUtil() {}

  private static File file() {
    return File.fromPath("asdf");
  }

  static DirectedGraph<Task> loadTasks() {
    return new TaskStore(file()).retrieveBlocking();
  }

  static void writeTasks(DirectedGraph<Task> tasks) {
    new TaskStore(file()).storeBlocking(tasks);
  }

  static String stringifyContents(Collection<?> collection) {
    return collection.stream().map(Object::toString).collect(joining(","));
  }
}
