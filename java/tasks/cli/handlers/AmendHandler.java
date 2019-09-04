package tasks.cli.handlers;

import static java.util.stream.Collectors.joining;
import static omnia.data.stream.Collectors.toSet;

import java.util.Optional;
import java.util.stream.Stream;
import omnia.algorithm.SetAlgorithms;
import omnia.data.structure.DirectedGraph;
import omnia.data.structure.Pair;
import omnia.data.structure.Set;
import omnia.data.structure.immutable.ImmutableDirectedGraph;
import omnia.data.structure.immutable.ImmutableSet;
import tasks.Task;
import tasks.cli.arg.AmendArguments;

public final class AmendHandler implements ArgumentHandler<AmendArguments> {
  @Override
  public void handle(AmendArguments arguments) {
    DirectedGraph<Task> taskGraph = HandlerUtil.loadTasks();

    Optional<? extends DirectedGraph.Node<Task>> targetTaskOptional =
        taskGraph.nodes().stream()
            .filter(n -> n.element().id().equals(arguments.targetTask()))
            .findFirst();

    DirectedGraph.Node<Task> targetTaskNode =
        targetTaskOptional.orElseThrow(
            () -> new HandlerException("unknown task id specified: " + arguments.targetTask()));

    Task targetTask = targetTaskNode.element();

    Set<Task.Id> existingTaskIds =
        taskGraph.nodes().stream()
            .map(DirectedGraph.Node::element)
            .map(Task::id)
            .collect(toSet());

    Set<Task.Id> allParameterizedIds =
        ImmutableSet.<Task.Id>builder()
            .addAll(arguments.blockedTasks())
            .addAll(arguments.blockedTasksToAdd())
            .addAll(arguments.blockedTasksToRemove())
            .addAll(arguments.blockingTasks())
            .addAll(arguments.blockingTasksToAdd())
            .addAll(arguments.blockingTasksToRemove())
            .build();

    // validate that all parameters refer to valid tasks
    Set<Task.Id> undefinedTasks =
        SetAlgorithms.differenceBetween(allParameterizedIds, existingTaskIds);

    if (undefinedTasks.isPopulated()) {
      String listOfUnknownIds =
          undefinedTasks.stream()
              .map(Object::toString)
              .collect(joining(", "));
      throw new HandlerException("unknown task id(s) specified: " + listOfUnknownIds);
    }

    // ensure task isn't connected to itself
    if (allParameterizedIds.contains(arguments.targetTask())) {
      throw new HandlerException("target task cannot block or be blocked by itself");
    }

    // ensure the same task isn't added and removed at the same time
    Set<Task.Id> ambiguousBlockedTasks =
        SetAlgorithms.intersectionOf(
            ImmutableSet.copyOf(arguments.blockedTasksToAdd()),
            ImmutableSet.copyOf(arguments.blockedTasksToRemove()));
    if (ambiguousBlockedTasks.isPopulated()) {
      String listOfAmbiguousTasks =
          ambiguousBlockedTasks.stream()
              .map(Object::toString)
              .collect(joining(", "));
      throw new HandlerException("ambiguous operation: tasks both added and removed from before: " + listOfAmbiguousTasks);
    }
    Set<Task.Id> ambiguousBlockingTasks =
        SetAlgorithms.intersectionOf(
            ImmutableSet.copyOf(arguments.blockedTasksToAdd()),
            ImmutableSet.copyOf(arguments.blockedTasksToRemove()));
    if (ambiguousBlockingTasks.isPopulated()) {
      String listOfAmbiguousTasks =
          ambiguousBlockingTasks.stream()
              .map(Object::toString)
              .collect(joining(", "));
      throw new HandlerException("ambiguous operation: tasks both added and removed from after: " + listOfAmbiguousTasks);
    }

    // okay, operation isn't ambiguous, task doesn't reference itself, and task ids are valid
    Set<Task.Id> blockingTaskIds =
        arguments.blockingTasks().isPopulated()
            ? ImmutableSet.copyOf(arguments.blockingTasks())
            : SetAlgorithms.unionOf(
                ImmutableSet.copyOf(arguments.blockingTasksToAdd()),
                SetAlgorithms.differenceBetween(
                    targetTaskNode.outgoingEdges().stream()
                        .map(DirectedGraph.Edge::end)
                        .map(DirectedGraph.Node::element)
                        .map(Task::id)
                        .collect(toSet()),
                    ImmutableSet.copyOf(arguments.blockingTasksToRemove())));

    Set<Task.Id> blockedTaskIds =
        arguments.blockedTasks().isPopulated()
            ? ImmutableSet.copyOf(arguments.blockedTasks())
            : SetAlgorithms.unionOf(
            ImmutableSet.copyOf(arguments.blockedTasksToAdd()),
            SetAlgorithms.differenceBetween(
                targetTaskNode.incomingEdges().stream()
                    .map(DirectedGraph.Edge::start)
                    .map(DirectedGraph.Node::element)
                    .map(Task::id)
                    .collect(toSet()),
                ImmutableSet.copyOf(arguments.blockedTasksToRemove())));

    String label = arguments.description().orElse(targetTask.label());

    Set<Task> blockingTasks =
        taskGraph.nodes().stream()
            .map(DirectedGraph.Node::element)
            .filter(task -> blockingTaskIds.contains(task.id()))
            .collect(toSet());
    Set<Task> blockedTasks =
        taskGraph.nodes().stream()
            .map(DirectedGraph.Node::element)
            .filter(task -> blockedTaskIds.contains(task.id()))
            .collect(toSet());

    Task newTask =
        Task.buildUpon(targetTask)
            .label(label)
            .dependencies(blockingTasks)
            .build();

    // build mutated graph
    ImmutableDirectedGraph.Builder<Task> newTaskGraphBuilder =
        ImmutableDirectedGraph.buildUpon(taskGraph);

    // eradicate traces of original task
    targetTaskNode.edges()
        .forEach(
            edge -> newTaskGraphBuilder.removeEdge(edge.start().element(), edge.end().element()));
    newTaskGraphBuilder.removeNode(targetTask);

    // insert new task into graph
    newTaskGraphBuilder.addNode(newTask);
    Stream.concat(
        blockedTasks.stream().map(blocked -> Pair.of(blocked, newTask)),
        blockingTasks.stream().map(blocking -> Pair.of(newTask, blocking)))
        .forEach(pair -> newTaskGraphBuilder.addEdge(pair.first(), pair.second()));

    HandlerUtil.writeTasks(newTaskGraphBuilder.build());

    System.out.println("task amended: " + newTask);
  }
}
