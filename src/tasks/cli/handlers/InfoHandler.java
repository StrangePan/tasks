package tasks.cli.handlers;

import static java.util.stream.Collectors.joining;
import static omnia.algorithm.SetAlgorithms.differenceBetween;
import static omnia.data.stream.Collectors.toSet;

import omnia.data.structure.DirectedGraph;
import omnia.data.structure.Set;
import omnia.data.structure.immutable.ImmutableSet;
import tasks.Task;
import tasks.cli.arg.InfoArguments;

public final class InfoHandler implements ArgumentHandler<InfoArguments> {
  @Override
  public void handle(InfoArguments arguments) {
    Set<Task.Id> specifiedIds = ImmutableSet.<Task.Id>builder().addAll(arguments.tasks()).build();

    if (!specifiedIds.isPopulated()) {
      throw new HandlerException("no tasks specified");
    }

    DirectedGraph<Task> taskGraph = HandlerUtil.loadTasks();
    Set<DirectedGraph.Node<Task>> targetTaskNodes =
        taskGraph.nodes()
            .stream()
            .filter(n -> specifiedIds.contains(n.element().id()))
            .collect(toSet());
    Set<Task> targetTasks =
        targetTaskNodes.stream()
            .map(DirectedGraph.Node::element)
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

    System.out.println(
        targetTaskNodes.stream()
            .map(InfoHandler::stringify)
            .collect(joining("\n\n")));
  }

  private static String stringify(DirectedGraph.Node<Task> node) {
    String dependencies =
        node.successors().stream().map(n -> "\n  " + n.element()).collect(joining());
    String dependents =
        node.predecessors().stream().map(n -> "\n  " + n.element()).collect(joining());

    return node.element()
        + (!dependencies.isEmpty() ? "Blocked by" + dependencies : "")
        + (!dependents.isEmpty() ? "Blocking" + dependencies : "");
  }
}
