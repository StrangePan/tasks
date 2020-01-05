package tasks.cli.handlers;

import static java.util.stream.Collectors.joining;
import static omnia.data.stream.Collectors.toSet;

import omnia.data.structure.DirectedGraph;
import omnia.data.structure.Set;
import tasks.Task;
import tasks.cli.arg.ListArguments;

public final class ListHandler implements ArgumentHandler<ListArguments> {
  @Override
  public void handle(ListArguments arguments) {
    DirectedGraph<Task> taskGraph = HandlerUtil.loadTasks();

    boolean showBlocked = arguments.isBlockedSet();
    boolean showCompleted = arguments.isCompletedSet();

    Set<Task> unblockedTasks = findUnblockedTasksIn(taskGraph);
    Set<Task> blockedTasks = showBlocked ? findBlockedIncompleteTasksIn(taskGraph) : Set.empty();
    Set<Task> completedTasks = showCompleted ? findCompletedTasksIn(taskGraph) : Set.empty();

    if (unblockedTasks.isPopulated()) {
      System.out.println("unblocked tasks:" + stringify(unblockedTasks));
    }
    if (blockedTasks.isPopulated()) {
      System.out.println("blocked tasks:" + stringify(blockedTasks));
    }
    if (completedTasks.isPopulated()) {
      System.out.println("completed tasks:" + stringify(completedTasks));
    }
  }

  private static Set<Task> findUnblockedTasksIn(DirectedGraph<Task> taskGraph) {
    return taskGraph.nodes().stream()
        .filter(n -> n.predecessors().stream().allMatch(m -> m.item().isCompleted()))
        .map(DirectedGraph.Node::item)
        .collect(toSet());
  }

  private static Set<Task> findBlockedIncompleteTasksIn(DirectedGraph<Task> taskGraph) {
    return taskGraph.nodes().stream()
        .filter(n -> n.predecessors().stream().anyMatch(m -> !m.item().isCompleted()))
        .map(DirectedGraph.Node::item)
        .collect(toSet());
  }

  private static Set<Task> findCompletedTasksIn(DirectedGraph<Task> taskGraph) {
    return taskGraph.nodes().stream()
        .map(DirectedGraph.Node::item)
        .filter(Task::isCompleted)
        .collect(toSet());
  }

  private static String stringify(Set<Task> tasks) {
    return tasks.stream().map(Object::toString).map(s -> "\n  " + s).collect(joining());
  }
}
