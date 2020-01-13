package tasks.cli.handlers;

import static java.util.stream.Collectors.joining;
import static omnia.algorithm.SetAlgorithms.differenceBetween;
import static omnia.data.stream.Collectors.toSet;

import omnia.data.structure.DirectedGraph;
import omnia.data.structure.Pair;
import omnia.data.structure.Set;
import omnia.data.structure.immutable.ImmutableDirectedGraph;
import omnia.data.structure.immutable.ImmutableSet;
import tasks.Task;
import tasks.cli.arg.CompleteArguments;

public final class CompleteHandler implements ArgumentHandler<CompleteArguments> {
  @Override
  public void handle(CompleteArguments arguments) {
    Set<Task.Id> specifiedIds = ImmutableSet.<Task.Id>builder().addAll(arguments.tasks()).build();

    // Validate arguments
    if (!specifiedIds.isPopulated()) {
      throw new HandlerException("no tasks specified");
    }

    DirectedGraph<Task> taskGraph = HandlerUtil.loadTasks();
    Set<DirectedGraph.DirectedNode<Task>> targetTaskNodes =
        taskGraph.nodes()
            .stream()
            .filter(n -> specifiedIds.contains(n.item().id()))
            .collect(toSet());
    Set<Task> targetTasks =
        targetTaskNodes.stream()
            .map(DirectedGraph.DirectedNode::item)
            .collect(toSet());

    if (targetTasks.count() != specifiedIds.count()) {
      // likely specified a task ID that doesn't exist. report which ones are wrong.
      Set<Task.Id> unknownIds =
          differenceBetween(
              specifiedIds,
              targetTasks.stream().map(Task::id).collect(toSet()));
      String listOfUnknownIds =
          unknownIds.stream()
              .map(Object::toString)
              .collect(joining(", "));
      throw new HandlerException("unknown task id(s) specified: " + listOfUnknownIds);
    }

    Set<Task> alreadyCompletedTasks =
        targetTasks.stream().filter(Task::isCompleted).collect(toSet());
    Set<DirectedGraph.DirectedNode<Task>> uncompletedTaskNodes =
        targetTaskNodes.stream().filter(n -> !n.item().isCompleted()).collect(toSet());
    Set<Task> uncompletedTasks =
        uncompletedTaskNodes.stream().map(DirectedGraph.Node::item).collect(toSet());
    Set<Task.Id> uncompletedTaskIds = uncompletedTasks.stream().map(Task::id).collect(toSet());

    if (alreadyCompletedTasks.isPopulated()) {
      System.out.println(
          "task(s) already marked as completed: "
              + alreadyCompletedTasks.stream()
                  .map(Task::id)
                  .map(Object::toString)
                  .collect(joining(", ")));
    }

    if (!uncompletedTasks.isPopulated()) {
      return;
    }

    ImmutableDirectedGraph.Builder<Task> newTaskGraphBuilder =
        ImmutableDirectedGraph.buildUpon(taskGraph);

    // replace existing nodes with new nodes
    uncompletedTasks.stream()
        .map(task -> Pair.of(task, task.toBuilder().isCompleted(true).build()))
        .forEach(pair -> newTaskGraphBuilder.replaceNode(pair.first(), pair.second()));

    HandlerUtil.writeTasks(newTaskGraphBuilder.build());

    System.out.println(
        "task(s) marked as completed: "
            + uncompletedTaskIds.stream().map(Object::toString).collect(joining(", ")));
  }
}
