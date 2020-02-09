package tasks.cli.handlers;

import static java.util.stream.Collectors.joining;
import static omnia.data.stream.Collectors.toSet;

import java.util.Optional;
import java.util.stream.Stream;
import omnia.algorithm.GraphAlgorithms;
import omnia.algorithm.SetAlgorithms;
import omnia.data.structure.DirectedGraph;
import omnia.data.structure.Pair;
import omnia.data.structure.Set;
import omnia.data.structure.immutable.ImmutableDirectedGraph;
import omnia.data.structure.immutable.ImmutableSet;
import tasks.Task;
import tasks.cli.CliTaskId;
import tasks.cli.arg.AmendArguments;

public final class AmendHandler implements ArgumentHandler<AmendArguments> {
  @Override
  public void handle(AmendArguments arguments) {
    DirectedGraph<Task> taskGraph = HandlerUtil.loadTasks();

    Optional<? extends DirectedGraph.Node<Task>> targetTaskOptional =
        taskGraph.nodes().stream()
            .filter(n -> n.item().id().equals(arguments.targetTask()))
            .findFirst();

    DirectedGraph.Node<Task> targetTaskNode =
        targetTaskOptional.orElseThrow(
            () -> new HandlerException("unknown task id specified: " + arguments.targetTask()));

    Task targetTask = targetTaskNode.item();

    Set<CliTaskId> existingTaskIds = taskGraph.contents().stream().map(Task::id).collect(toSet());

    Set<CliTaskId> allSpecifiedIds =
        ImmutableSet.<CliTaskId>builder()
            .addAll(arguments.blockedTasks())
            .addAll(arguments.blockedTasksToAdd())
            .addAll(arguments.blockedTasksToRemove())
            .addAll(arguments.blockingTasks())
            .addAll(arguments.blockingTasksToAdd())
            .addAll(arguments.blockingTasksToRemove())
            .build();

    // validate that all parameters refer to existing tasks
    Set<CliTaskId> undefinedTasks =
        SetAlgorithms.differenceBetween(allSpecifiedIds, existingTaskIds);

    if (undefinedTasks.isPopulated()) {
      String listOfUnknownIds =
          undefinedTasks.stream()
              .map(Object::toString)
              .collect(joining(", "));
      throw new HandlerException("unknown task id(s) specified: " + listOfUnknownIds);
    }

    // ensure task isn't connected to itself
    // this is a convenience but is not strictly required because we still need to check if the
    // graph is cyclical
    if (allSpecifiedIds.contains(arguments.targetTask())) {
      throw new HandlerException("target task cannot block or be blocked by itself");
    }

    // ensure the same task isn't added and removed at the same time
    Set<CliTaskId> ambiguousBlockedTasks =
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
    Set<CliTaskId> ambiguousBlockingTasks =
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
    Set<CliTaskId> blockingTaskIds =
        arguments.blockingTasks().isPopulated()
            ? ImmutableSet.copyOf(arguments.blockingTasks())
            : SetAlgorithms.unionOf(
                ImmutableSet.copyOf(arguments.blockingTasksToAdd()),
                SetAlgorithms.differenceBetween(
                    targetTaskNode.outgoingEdges().stream()
                        .map(DirectedGraph.Edge::end)
                        .map(DirectedGraph.Node::item)
                        .map(Task::id)
                        .collect(toSet()),
                    ImmutableSet.copyOf(arguments.blockingTasksToRemove())));

    Set<CliTaskId> blockedTaskIds =
        arguments.blockedTasks().isPopulated()
            ? ImmutableSet.copyOf(arguments.blockedTasks())
            : SetAlgorithms.unionOf(
            ImmutableSet.copyOf(arguments.blockedTasksToAdd()),
            SetAlgorithms.differenceBetween(
                targetTaskNode.incomingEdges().stream()
                    .map(DirectedGraph.Edge::start)
                    .map(DirectedGraph.Node::item)
                    .map(Task::id)
                    .collect(toSet()),
                ImmutableSet.copyOf(arguments.blockedTasksToRemove())));

    String label = arguments.description().orElse(targetTask.label());

    Set<Task> blockingTasks =
        taskGraph.nodes().stream()
            .map(DirectedGraph.Node::item)
            .filter(task -> blockingTaskIds.contains(task.id()))
            .collect(toSet());
    Set<Task> blockedTasks =
        taskGraph.nodes().stream()
            .map(DirectedGraph.Node::item)
            .filter(task -> blockedTaskIds.contains(task.id()))
            .collect(toSet());

    Task newTask =
        targetTask.toBuilder()
            .label(label)
            .build();

    // build mutated graph
    ImmutableDirectedGraph.Builder<Task> newTaskGraphBuilder =
        ImmutableDirectedGraph.buildUpon(taskGraph);

    // eradicate traces empty original task
    targetTaskNode.edges()
        .forEach(
            edge -> newTaskGraphBuilder.removeEdge(edge.start().item(), edge.end().item()));
    newTaskGraphBuilder.removeNode(targetTask);

    // insert new task into graph
    newTaskGraphBuilder.addNode(newTask);
    Stream.concat(
        blockedTasks.stream().map(blocked -> Pair.of(blocked, newTask)),
        blockingTasks.stream().map(blocking -> Pair.of(newTask, blocking)))
        .forEach(pair -> newTaskGraphBuilder.addEdge(pair.first(), pair.second()));

    DirectedGraph<Task> newTaskGraph = newTaskGraphBuilder.build();

    if (GraphAlgorithms.isCyclical(newTaskGraph)) {
      throw new HandlerException("amend failed: circular dependency introduced");
    }

    HandlerUtil.writeTasks(newTaskGraphBuilder.build());

    System.out.println("task amended: " + newTask);
  }
}
