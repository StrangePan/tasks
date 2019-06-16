package tasks.cli.handlers;

import static java.util.stream.Collectors.joining;
import static omnia.data.stream.Collectors.toList;

import omnia.data.structure.Collection;
import omnia.data.structure.DirectedGraph;
import omnia.data.structure.DirectedGraph.Node;
import omnia.data.structure.immutable.ImmutableDirectedGraph;
import tasks.Task;
import tasks.io.File;
import tasks.io.TaskStore;

final class HandlerUtil {

  private HandlerUtil() {}

  private static File file() {
    return File.fromPath("asdf");
  }

  static DirectedGraph<Task> loadTasks() {
    TaskStore taskStore = new TaskStore(file());
    Collection<Task> tasks = taskStore.retrieveBlocking();
    ImmutableDirectedGraph.Builder<Task> graphBuilder = ImmutableDirectedGraph.builder();

    // First, pull out the tasks themselves
    for (Task task : tasks) {
      for (Task dependency : task.dependencies()) {
        graphBuilder.addEdge(task, dependency);
      }
    }

    return graphBuilder.build();
  }

  static void writeTasks(DirectedGraph<Task> tasks) {
    TaskStore taskStore = new TaskStore(file());
    Collection<Task> preparedTasks = tasks.nodes().stream().map(Node::element).collect(toList());
    taskStore.storeBlocking(preparedTasks);
  }

  static String stringifyContents(Collection<?> collection) {
    return collection.stream().map(Object::toString).collect(joining(","));
  }
}
