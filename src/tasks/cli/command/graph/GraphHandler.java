package tasks.cli.command.graph;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.Objects.requireNonNull;
import static omnia.algorithm.GraphAlgorithms.topologicallySort;
import static omnia.algorithm.ListAlgorithms.reverse;
import static omnia.data.stream.Collectors.toImmutableList;
import static omnia.data.stream.Collectors.toImmutableSet;

import io.reactivex.Single;
import java.util.Optional;
import omnia.cli.out.Output;
import omnia.data.cache.Memoized;
import omnia.data.structure.DirectedGraph;
import omnia.data.structure.DirectedGraph.DirectedNode;
import omnia.data.structure.List;
import omnia.data.structure.Map;
import omnia.data.structure.Set;
import omnia.data.structure.immutable.ImmutableMap;
import omnia.data.structure.immutable.ImmutableSet;
import omnia.data.structure.mutable.HashMap;
import omnia.data.structure.mutable.HashSet;
import omnia.data.structure.mutable.MutableMap;
import omnia.data.structure.mutable.MutableSet;
import omnia.data.structure.tuple.Couple;
import omnia.data.structure.tuple.Tuple;
import omnia.data.structure.tuple.Tuplet;
import tasks.cli.handler.ArgumentHandler;
import tasks.model.Task;
import tasks.model.TaskStore;

/** Business logic for the Graph command. */
public final class GraphHandler implements ArgumentHandler<GraphArguments> {
  private final Memoized<? extends TaskStore> taskStore;

  public GraphHandler(Memoized<? extends TaskStore> taskStore) {
    this.taskStore = requireNonNull(taskStore);
  }

  @Override
  public Single<Output> handle(GraphArguments arguments) {
    return taskStore.value()
        .taskGraph()
        .map(graph -> Tuple.of(graph, topologicallySort(graph)))
        .map(couple -> couple.append(assignColumns(couple.first(), couple.second())))
        .firstOrError()
        .map(triple -> renderGraph(triple.first(), triple.second(), triple.third()));
  }

  /**
   * Assigns columns to each graph node for CLI rendering. Does so by linearly traversing the
   * topologically sorted list of nodes, looking ahead at the number of outgoing edges for each
   * node, tracking the number of unresolved edges, and assigning columns so that edges do not
   * overlap any nodes.
   *
   * @param taskGraph The graph containing the edges for each item in the task list. Must contain
   *     every node in {@code taskList}.
   * @param taskList a topologically sorted list of tasks to assign columns for, where each node
   *     precedes its successors
   * @return A mapping of column assignments for each node, where 0 is the first column. Every node
   *     passed into {@code taskList} will have an entry in the result.
   */
  private <T> Map<T, Integer> assignColumns(DirectedGraph<T> taskGraph, List<T> taskList) {
    MutableMap<T, Integer> assignedColumns = HashMap.create();
    MutableSet<T> unresolvedSuccessors = HashSet.create();

    for (T taskToAssign : taskList) {
      int assignedColumn =
          assignedColumns.putMappingIfAbsent(
              taskToAssign,
              // scan for the first available column not already claimed by a node later in the list
              () ->
                  unresolvedSuccessors.stream().map(assignedColumns::valueOf)
                      .flatMap(Optional::stream)
                      .reduce(0, (candidate, occupied) -> Math.max(candidate, occupied + 1)));

      unresolvedSuccessors.remove(taskToAssign);

      ImmutableSet<T> successorsToAssign =
          taskGraph.nodeOf(taskToAssign)
              .map(DirectedNode::successors)
              .orElse(ImmutableSet.empty())
              .stream()
              .map(DirectedNode::item)
              .filter(successor -> assignedColumns.valueOf(successor).isEmpty())
              .collect(toImmutableSet());

      int successorColumn = assignedColumn;
      for (T successor : successorsToAssign) {
        assignedColumns.putMapping(successor, successorColumn);
        unresolvedSuccessors.add(successor);
        successorColumn++;
      }
    }

    return ImmutableMap.copyOf(assignedColumns);
  }

  private Output renderGraph(
      DirectedGraph<Task> taskGraph, List<Task> taskList, Map<Task, Integer> taskColumns) {

    // NOTE: taskList has blockers at the start / top and blockees at the end / bottom.

    Output.Builder output = Output.builder();
    int unresolvedEdges = 0;

    for (Task task : reverse(taskList)) {
      int taskColumn = taskColumns.valueOf(task).get();

      // the line containing the task info
      {
        Output.Builder line = Output.builder();
        for (int col = 0; col < max(unresolvedEdges, taskColumn + 1); col++) {
          line.append(col == taskColumn ? task.isCompleted().blockingFirst() ? "☑" : "☐" : "│");
        }
        line.append("  ").append(task.render());
        output.appendLine(line.build());
      }

      // print the line containing edge connections
      {
        Output.Builder line = Output.builder();
        Set<Integer> successorColumns =
            taskGraph.nodeOf(task)
                .get()
                .successors()
                .stream()
                .map(DirectedNode::item)
                .map(successor -> taskColumns.valueOf(successor).get())
                .collect(toImmutableSet());

        Optional<Couple<Integer, Integer>> lateralEdgeRange =
            successorColumns.stream()
                .<Optional<Couple<Integer, Integer>>>reduce(
                    Optional.empty(),
                    (minMax, col) ->
                        Optional.of(
                            minMax.map(
                                    c -> c.mapFirst(min -> min(min, col))
                                        .mapSecond(max -> max(max, col)))
                                .orElse(Tuplet.of(col, col))),
                    (a, b) ->
                        a.isPresent() && b.isPresent()
                            ? Optional.of(
                                Tuple.of(
                                    min(a.get().first(), b.get().first()),
                                    max(a.get().second(), b.get().second())))
                            : a.isPresent() ? a : b)
                .map(
                    minMax ->
                        minMax.mapFirst(min -> min(min, taskColumn))
                            .mapSecond(max -> max(max, taskColumn)));

        for (int col = 0; col < max(unresolvedEdges, taskColumn + 1); col++) {
          boolean isTaskCol = col == taskColumn;
          boolean isSuccessorCol = successorColumns.contains(col);
          boolean isInLateralEdge = isInRange(col, lateralEdgeRange);
          boolean isAtLateralEdgeMin =
              lateralEdgeRange.isPresent() && lateralEdgeRange.get().first() == col;
          boolean isAtLateralEdgeMax =
              lateralEdgeRange.isPresent() && lateralEdgeRange.get().second() == col;

          line.append(
              isAtLateralEdgeMin && isAtLateralEdgeMax
                  ? "│"
              : isAtLateralEdgeMin
                  ? isTaskCol
                      ? isSuccessorCol
                          ? "├"
                          : "└"
                      : "┌"
              : isAtLateralEdgeMax
                  ? isTaskCol
                      ? isSuccessorCol
                          ? "┤"
                          : "┘"
                      : "┐"
              : isInLateralEdge
                  ? "╪"
                  : !isTaskCol
                      ? "│"
                      : "");
        }
        output.appendLine(line.build());
      }
    }

    return output.build();
  }

  private static boolean isInRange(int val, Optional<Couple<Integer, Integer>> range) {
    return range.map(
        minMax ->
            val >= min(minMax.first(), val)
                && val <= max(minMax.second(), val))
        .orElse(false);
  }
}
