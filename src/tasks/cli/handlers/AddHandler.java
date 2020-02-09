package tasks.cli.handlers;

import static omnia.algorithm.SetAlgorithms.differenceBetween;
import static omnia.algorithm.SetAlgorithms.unionOf;
import static omnia.data.stream.Collectors.toSet;
import static tasks.cli.handlers.HandlerUtil.stringifyContents;

import java.util.stream.Stream;
import omnia.algorithm.GraphAlgorithms;
import omnia.data.structure.DirectedGraph;
import omnia.data.structure.Pair;
import omnia.data.structure.Set;
import omnia.data.structure.immutable.ImmutableDirectedGraph;
import omnia.data.structure.immutable.ImmutableSet;
import tasks.cli.arg.AddArguments;
import tasks.model.Task;
import tasks.model.TaskStore;

public final class AddHandler implements ArgumentHandler<AddArguments> {

  @Override
  public void handle(AddArguments arguments) {
    // Validate arguments
    String label = arguments.description().trim();
    if (label.isEmpty()) {
      throw new HandlerException("description cannot be empty or whitespace only");
    }

    TaskStore taskStore = HandlerUtil.loadTaskStore();
    Set<Task> tasks = taskStore.allTasks().blockingFirst();
    Set<Task.Id> taskIds = tasks.stream().map(Task::id).collect(toSet());
    Set<Task.Id> blockedIds = ImmutableSet.copyOf(arguments.blockedTasks());
    Set<Task.Id> blockingIds = ImmutableSet.copyOf(arguments.blockingTasks());

    Set<Task.Id> invalidTaskIds = differenceBetween(unionOf(blockedIds, blockingIds), taskIds);
    if (invalidTaskIds.isPopulated()) {
      throw new HandlerException(
          "unrecognized tasks specified: " + stringifyContents(invalidTaskIds));
    }

    // Collect the dependencies and dependents
    Set<Task> nextDependencies =
        tasks.stream().filter(task -> blockingIds.contains(task.id())).collect(toSet());
    Set<Task> nextDependents =
        tasks.stream().filter(task -> blockedIds.contains(task.id())).collect(toSet());

    // Construct the new task
    Task.Id nextId = Task.Id.after(taskIds);
    Task nextTask = Task.builder().id(nextId).label(label).isCompleted(false).build();

    // Construct a new task graph with the new task inserted and the new edges assembled
    ImmutableDirectedGraph.Builder<Task> newTasksBuilder =
        ImmutableDirectedGraph.buildUpon(taskGraph).addNode(nextTask);
    Stream.concat(
        nextDependencies.stream().map(task -> Pair.of(nextTask, task)),
        nextDependents.stream().map(task -> Pair.of(task, nextTask)))
        .forEach(pair -> newTasksBuilder.addEdge(pair.first(), pair.second()));
    DirectedGraph<Task> newTasks = newTasksBuilder.build();

    if (!GraphAlgorithms.isAcyclical(newTasks)) {
      throw new HandlerException("task graph contains a dependency cycle");
    }

    HandlerUtil.writeTasks(newTasks);
  }
}
