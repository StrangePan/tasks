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
import tasks.cli.CliTaskId;
import tasks.cli.arg.ReopenArguments;

public final class ReopenHandler implements ArgumentHandler<ReopenArguments> {
  @Override
  public void handle(ReopenArguments arguments) {
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

    Set<Task> alreadyOpenTasks =
        targetTasks.stream().filter(t -> !t.isCompleted()).collect(toSet());
    Set<DirectedGraph.Node<Task>> completedTaskNodes =
        targetTaskNodes.stream().filter(n -> n.item().isCompleted()).collect(toSet());
    Set<Task> completedTasks =
        completedTaskNodes.stream().map(DirectedGraph.Node::item).collect(toSet());
    Set<CliTaskId> completedTaskIds = completedTasks.stream().map(Task::id).collect(toSet());

    if (alreadyOpenTasks.isPopulated()) {
      System.out.println(
          "task(s) already open: "
              + alreadyOpenTasks.stream()
                  .map(Task::id)
                  .map(Object::toString)
                  .collect(joining(", ")));
    }

    if (!completedTasks.isPopulated()) {
      return;
    }

    ImmutableDirectedGraph.Builder<Task> newTaskBuilder =
        ImmutableDirectedGraph.buildUpon(taskGraph);

    // replace existing nodes with new nodes
    completedTasks.stream()
        .map(task -> Pair.of(task, task.toBuilder().isCompleted(false).build()))
        .forEach(pair -> newTaskBuilder.replaceNode(pair.first(), pair.second()));

    HandlerUtil.writeTasks(newTaskBuilder.build());

    System.out.println(
        "task(s) reopened: "
            + completedTaskIds.stream().map(Object::toString).collect(joining(", ")));
  }
}
