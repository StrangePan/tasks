package tasks.cli.handlers;

import static java.util.stream.Collectors.joining;
import static omnia.algorithm.SetAlgorithms.differenceBetween;
import static omnia.data.stream.Collectors.toSet;

import omnia.data.structure.DirectedGraph;
import omnia.data.structure.Set;
import omnia.data.structure.immutable.ImmutableDirectedGraph;
import omnia.data.structure.immutable.ImmutableSet;
import tasks.Task;
import tasks.cli.CliTaskId;
import tasks.cli.arg.RemoveArguments;

public final class RemoveHandler implements ArgumentHandler<RemoveArguments> {
  @Override
  public void handle(RemoveArguments arguments) {
    Set<CliTaskId> specifiedIds = ImmutableSet.<CliTaskId>builder().addAll(arguments.tasks()).build();

    // Validate arguments
    if (!specifiedIds.isPopulated()) {
      throw new HandlerException("no tasks specified");
    }

    DirectedGraph<Task> taskGraph = HandlerUtil.loadTasks();
    Set<DirectedGraph.Node<Task>> targetTaskNodes =
        taskGraph.nodes()
            .stream()
            .filter(n -> specifiedIds.contains(n.item().id()))
            .collect(toSet());
    Set<Task> targetTasks =
        targetTaskNodes.stream()
            .map(DirectedGraph.Node::item)
            .collect(toSet());

    if (targetTasks.count() != specifiedIds.count()) {
      // likely specified a task ID that doesn't exist. report which ones are wrong.
      Set<CliTaskId> unknownIds =
          differenceBetween(
              specifiedIds,
              targetTasks.stream().map(Task::id).collect(toSet()));
      String listOfUnknownIds =
          unknownIds.stream()
              .map(Object::toString)
              .collect(joining(", "));
      throw new HandlerException("unknown task id(s) specified: " + listOfUnknownIds);
    }

    ImmutableDirectedGraph.Builder<Task> newTaskBuilder =
        ImmutableDirectedGraph.buildUpon(taskGraph);

    // remove the target tasks from the graph
    targetTasks.forEach(newTaskBuilder::removeNode);

    HandlerUtil.writeTasks(newTaskBuilder.build());

    System.out.println(
        "task(s) marked as completed: "
            + targetTasks.stream()
            .map(Task::id)
            .map(Object::toString)
            .collect(joining(", ")));
  }
}
