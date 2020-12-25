package tasks.cli.feature.graph;

import static java.util.Objects.requireNonNull;
import static omnia.data.stream.Collectors.toImmutableList;
import static omnia.data.stream.Collectors.toImmutableSet;
import static tasks.util.rx.Observables.incrementingInteger;
import static tasks.util.rx.Observables.toImmutableMap;

import io.reactivex.Observable;
import io.reactivex.Single;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import omnia.algorithm.GraphAlgorithms;
import omnia.cli.out.Output;
import omnia.data.cache.Memoized;
import omnia.data.structure.DirectedGraph;
import omnia.data.structure.DirectedGraph.DirectedNode;
import omnia.data.structure.List;
import omnia.data.structure.Map;
import omnia.data.structure.Set;
import omnia.data.structure.immutable.ImmutableDirectedGraph;
import omnia.data.structure.immutable.ImmutableList;
import omnia.data.structure.immutable.ImmutableMap;
import omnia.data.structure.immutable.ImmutableSet;
import omnia.data.structure.mutable.HashMap;
import omnia.data.structure.mutable.HashSet;
import omnia.data.structure.mutable.MutableMap;
import omnia.data.structure.mutable.MutableSet;
import omnia.data.structure.tuple.Tuple;
import tasks.cli.command.common.CommonArguments;
import tasks.cli.handler.ArgumentHandler;
import tasks.model.Task;
import tasks.model.ObservableTaskStore;
import tasks.model.TaskStore;
import tasks.util.rx.Observables;

/** Business logic for the Graph command. */
public final class GraphHandler implements ArgumentHandler<GraphArguments> {

  // each of these are package-private so that the unit tests can use them for parsing
  static final int CONTINUATION_UP_DOWN = '╎';
  static final int EDGE_UP_DOWN_RIGHT = '├';
  static final int EDGE_UP_DOWN_LEFT = '┤';
  static final int EDGE_UP_DOWN = '│';
  static final int EDGE_UP_DOWN_LEFT_RIGHT = '┼';
  static final int EDGE_UP_RIGHT = '└';
  static final int EDGE_UP_LEFT = '┘';
  static final int EDGE_UP_LEFT_RIGHT = '┴';
  static final int EDGE_DOWN_RIGHT = '┌';
  static final int EDGE_DOWN_LEFT = '┐';
  static final int EDGE_DOWN_LEFT_RIGHT = '┬';
  static final int EDGE_LEFT_RIGHT = '─';
  static final int GAP = ' ';
  static final int NODE_COMPLETED = '☑';
  static final int NODE_OPEN = '☐';

  private final Memoized<? extends ObservableTaskStore> taskStore;
  private final TopologicalSorter sorter;

  public GraphHandler(Memoized<? extends ObservableTaskStore> taskStore) {
    this(taskStore, GraphAlgorithms::topologicallySort);
  }

  GraphHandler(Memoized<? extends ObservableTaskStore> taskStore, TopologicalSorter sorter) {
    this.taskStore = requireNonNull(taskStore);
    this.sorter = requireNonNull(sorter);
  }

  @Override
  public Single<Output> handle(CommonArguments<? extends GraphArguments> arguments) {
    return taskStore.value()
        .observe()
        .firstOrError()
        .map(TaskStore::taskGraph)
        .flatMap(s -> this.doHandle(arguments, s));
  }

  private <T extends Task> Single<Output> doHandle(
      CommonArguments<? extends GraphArguments> arguments,
      ImmutableDirectedGraph<T> taskStoreSingle) {
    return Single.just(taskStoreSingle)
        .flatMap(
            graph ->
                Single.just(sorter.sort(graph))
                    .compose(
                        single ->
                            arguments.specificArguments().isAllSet()
                                ? single
                                : single.flatMapObservable(Observable::fromIterable)
                                    .filter(task -> !task.status().isCompleted())
                                    .to(Observables.toImmutableList()))
                    .map(tasks -> Tuple.<DirectedGraph<T>, List<T>>of(graph, tasks)))
    .map(
        couple -> couple.append(
            assignColumns(couple.first(), couple.second(), arguments.specificArguments())))
    .map(
        triple -> renderGraph(
            triple.first(), triple.second(), triple.third(), arguments.specificArguments()));
  }

  /**
   * Assigns columns to each graph node for CLI rendering.
   *
   * <p>Columns are assigned eagerly, favoring smaller indexes. Linearly traverses the topological
   * list of nodes and assigns columns to each node's successors if the successor doesn't yet have
   * a column assignment.
   *
   * @param taskGraph The graph containing the edges for each item in the task list. Must contain
   *     every node in {@code taskList}.
   * @param taskList a topologically sorted list of tasks to assign columns for, where each node
   *     precedes its successors
   * @return A mapping of column assignments for each node, where 0 is the first column. Every node
   *     passed into {@code taskList} will have an entry in the result.
   */
  private static <T extends Task> Map<T, Integer> assignColumns(
      DirectedGraph<T> taskGraph, List<T> taskList, GraphArguments arguments) {
    MutableMap<T, Integer> assignedColumns = HashMap.create();
    MutableSet<T> unresolvedSuccessors = HashSet.create();
    ImmutableMap<T, Integer> topologicalIndexes =
        Observable.fromIterable(taskList)
            .zipWith(incrementingInteger(), Tuple::of)
            .to(toImmutableMap(couple -> couple.first(), couple -> couple.second()))
            .blockingGet();

    for (T taskToAssign : taskList) {
      int assignedColumn =
          assignedColumns.putMappingIfAbsent(
              taskToAssign,
              // scan for the first available column not already claimed by a node later in the list
              () -> unresolvedSuccessors.stream().map(assignedColumns::valueOf)
                  .flatMap(Optional::stream)
                  .reduce(0, (candidate, occupied) -> Math.max(candidate, occupied + 1)));

      unresolvedSuccessors.remove(taskToAssign);

      ImmutableList<T> successorsToAssign =
          taskGraph.nodeOf(taskToAssign)
              .map(DirectedNode::successors)
              .orElse(ImmutableSet.empty())
              .stream()
              .map(DirectedNode::item)
              .filter(successor -> assignedColumns.valueOf(successor).isEmpty())
              .filter(successor -> arguments.isAllSet() || !successor.status().isCompleted())
              .sorted(
                  Comparator.<T, Integer>comparing(
                          item -> topologicalIndexes.valueOf(item).orElse(0))
                      .reversed())
              .collect(toImmutableList());

      int successorColumn = assignedColumn;
      for (T successor : successorsToAssign) {
        assignedColumns.putMapping(successor, successorColumn);
        unresolvedSuccessors.add(successor);
        successorColumn++;
      }
    }

    return ImmutableMap.copyOf(assignedColumns);
  }

  private static <T extends Task> Output renderGraph(
      DirectedGraph<T> taskGraph,
      List<T> taskList,
      Map<T, Integer> taskColumns,
      GraphArguments arguments) {
    Output.Builder output = Output.builder();
    MutableSet<Integer> columnsWithEdges = HashSet.create();
    ImmutableMap<T, Integer> topologicalIndexes =
        Observable.fromIterable(taskList)
            .zipWith(incrementingInteger(), Tuple::of)
            .to(toImmutableMap(couple -> couple.first(), couple -> couple.second()))
            .blockingGet();

    for (T task : taskList) {
      taskColumns.valueOf(task).ifPresent(columnsWithEdges::remove);

      output.appendLine(renderTaskLine(taskColumns, columnsWithEdges, task));

      renderEdgeLine(taskGraph, topologicalIndexes, taskColumns, columnsWithEdges, arguments, task)
          .ifPresent(output::appendLine);

      taskGraph.nodeOf(task)
          .map(DirectedNode::successors)
          .orElse(ImmutableSet.empty())
          .stream()
          .map(DirectedNode::item)
          .map(taskColumns::valueOf)
          .flatMap(Optional::stream)
          .forEach(columnsWithEdges::add);
    }

    return output.build();
  }

  private static <T extends Task> Output renderTaskLine(
      Map<T, Integer> taskColumns, Set<Integer> columnsWithEdges, T task) {
    int taskColumn = taskColumns.valueOf(task).orElse(0);

    int maxColumnsWithEdges = columnsWithEdges.stream().reduce(taskColumn, Math::max);

    return incrementingInteger()
        .takeUntil(i -> i == maxColumnsWithEdges)
        .map(
            column -> (column == taskColumn
                ? (task.status().isCompleted() ? NODE_COMPLETED : NODE_OPEN)
                : (columnsWithEdges.contains(column) ? CONTINUATION_UP_DOWN : GAP)))
        .map(Character::toString)
        .collectInto(Output.builder(),  Output.Builder::append)
        .map(builder -> builder.append(Character.toString(GAP)).append(task.render()))
        .map(Output.Builder::build)
        .blockingGet();
  }

  private static <T extends Task> Optional<Output> renderEdgeLine(
      DirectedGraph<T> taskGraph,
      Map<T, Integer> topologicalIndexes,
      Map<T, Integer> taskColumns,
      Set<Integer> previousColumnsWithEdges,
      GraphArguments arguments,
      T task) {
    ImmutableSet<Integer> successorColumns =
        taskGraph.nodeOf(task)
            .map(DirectedNode::successors)
            .orElse(ImmutableSet.empty())
            .stream()
            .map(DirectedNode::item)
            .filter(item -> arguments.isAllSet() || !item.status().isCompleted())
            .map(taskColumns::valueOf)
            .flatMap(Optional::stream)
            .collect(toImmutableSet());

    if (!successorColumns.isPopulated()) {
      return Optional.empty();
    }

    int taskColumn = taskColumns.valueOf(task).orElse(0);

    // if there's only one successor and it's not on the next row, but it's in the same column,
    // skip drawing the edge line
    if (successorColumns.count() == 1){
      Optional<T> successor =
          taskGraph.nodeOf(task)
              .map(DirectedNode::successors)
              .orElse(ImmutableSet.empty())
              .stream()
              .findFirst()
              .map(DirectedNode::item);
      if (successor.flatMap(taskColumns::valueOf).orElse(0) == taskColumn
          && !Objects.equals(
              topologicalIndexes.valueOf(task).map(row -> row + 1),
              successor.flatMap(topologicalIndexes::valueOf))) {
        return Optional.empty();
      }
    }

    int lateralEdgeMax = successorColumns.stream().reduce(taskColumn, Math::max);
    int lateralEdgeMin = successorColumns.stream().reduce(taskColumn, Math::min);
    int maxColumn = previousColumnsWithEdges.stream().reduce(lateralEdgeMax, Math::max);

    return incrementingInteger()
        .takeUntil(column -> column == maxColumn)
        .map(
            columnInteger -> {
              int column = columnInteger;
              if (isInRange(lateralEdgeMin, column, lateralEdgeMax)) {
                if (column == taskColumn) {
                  if (successorColumns.contains(column)) {
                    if (column == lateralEdgeMin && column != lateralEdgeMax) {
                      return EDGE_UP_DOWN_RIGHT;
                    } else if (column != lateralEdgeMin && column == lateralEdgeMax) {
                      return EDGE_UP_DOWN_LEFT;
                    } else if (column == lateralEdgeMin) {
                      return EDGE_UP_DOWN;
                    } else {
                      return EDGE_UP_DOWN_LEFT_RIGHT;
                    }
                  } else {
                    if (column == lateralEdgeMin && column != lateralEdgeMax) {
                      return EDGE_UP_RIGHT;
                    } else if (column != lateralEdgeMin && column == lateralEdgeMax) {
                      return EDGE_UP_LEFT;
                    } else {
                      return EDGE_UP_LEFT_RIGHT;
                    }
                  }
                } else if (previousColumnsWithEdges.contains(column)) {
                  if (successorColumns.contains(column)) {
                    if (column == lateralEdgeMin) {
                      return EDGE_DOWN_RIGHT;
                    } else if (column == lateralEdgeMax) {
                      return EDGE_DOWN_LEFT;
                    } else {
                      return EDGE_DOWN_LEFT_RIGHT;
                    }
                  } else {
                    return EDGE_LEFT_RIGHT;
                  }
                } else if (successorColumns.contains(column)) {
                  if (column == lateralEdgeMin) {
                    return EDGE_DOWN_RIGHT;
                  } else if (column == lateralEdgeMax) {
                    return EDGE_DOWN_LEFT;
                  } else {
                    return EDGE_DOWN_LEFT_RIGHT;
                  }
                } else {
                  return EDGE_LEFT_RIGHT;
                }
              } else if (previousColumnsWithEdges.contains(column)) {
                return CONTINUATION_UP_DOWN;
              } else {
                return GAP;
              }
            })
        .map(Character::toString)
        .collectInto(Output.builder(),  Output.Builder::append)
        .map(Output.Builder::build)
        .map(Optional::of)
        .blockingGet();
  }

  private static boolean isInRange(int min, int mid, int max) {
    return min <= mid && mid <= max;
  }

  interface TopologicalSorter {
    <T extends Task> ImmutableList<T> sort(DirectedGraph<T> graph);
  }
}
