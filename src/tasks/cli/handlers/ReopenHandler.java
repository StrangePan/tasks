package tasks.cli.handlers;

import static java.util.stream.Collectors.joining;
import static omnia.algorithm.SetAlgorithms.differenceBetween;
import static omnia.data.stream.Collectors.toSet;

import java.util.stream.Stream;
import omnia.data.structure.Collection;
import omnia.data.structure.DirectedGraph;
import omnia.data.structure.HomogeneousPair;
import omnia.data.structure.Set;
import omnia.data.structure.immutable.ImmutableDirectedGraph;
import omnia.data.structure.immutable.ImmutableSet;
import tasks.Task;
import tasks.cli.arg.ReopenArguments;

public final class ReopenHandler implements ArgumentHandler<ReopenArguments> {
  @Override
  public void handle(ReopenArguments arguments) {
    Set<Task.Id> specifiedIds = ImmutableSet.<Task.Id>builder().addAll(arguments.tasks()).build();

    // Validate arguments
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

    Set<Task> alreadyOpenTasks =
        targetTasks.stream().filter(t -> !t.isCompleted()).collect(toSet());
    Set<DirectedGraph.Node<Task>> completedTaskNodes =
        targetTaskNodes.stream().filter(n -> n.element().isCompleted()).collect(toSet());
    Set<Task> completedTasks =
        completedTaskNodes.stream().map(DirectedGraph.Node::element).collect(toSet());
    Set<Task> newlyCompletedTasks =
        completedTasks.stream()
            .map(t -> Task.buildUpon(t).isCompleted(false).build())
            .collect(toSet());

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

    // add new nodes to graph
    newlyCompletedTasks.forEach(newTaskBuilder::addNode);

    // copy over dependencies and dependants
    Stream.concat(
        newlyCompletedTasks.stream()
            .flatMap(
                newTask -> completedTaskNodes.stream()
                    .filter(on -> on.element().id().equals(newTask.id()))
                    .map(DirectedGraph.Node::successors)
                    .flatMap(Collection::stream)
                    .map(DirectedGraph.Node::element)
                    .map(dependency -> HomogeneousPair.of(newTask, dependency))),
        newlyCompletedTasks.stream()
            .flatMap(
                newTask -> completedTaskNodes.stream()
                    .filter(on -> on.element().id().equals(newTask.id()))
                    .map(DirectedGraph.Node::predecessors)
                    .flatMap(Collection::stream)
                    .map(DirectedGraph.Node::element)
                    .map(dependant -> HomogeneousPair.of(dependant, newTask))))
        .forEach(p -> newTaskBuilder.addEdge(p.first(), p.second()));

    // remove old nodes
    completedTasks.forEach(newTaskBuilder::removeNode);

    HandlerUtil.writeTasks(newTaskBuilder.build());

    System.out.println(
        "task(s) reopened: "
            + newlyCompletedTasks.stream()
                .map(Task::id)
                .map(Object::toString)
                .collect(joining(", ")));
  }
}
