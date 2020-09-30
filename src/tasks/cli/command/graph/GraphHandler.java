package tasks.cli.command.graph;

import static java.util.Objects.requireNonNull;
import static omnia.algorithm.GraphAlgorithms.topologicallySort;

import io.reactivex.Observable;
import io.reactivex.Single;
import omnia.cli.out.Output;
import omnia.data.cache.Memoized;
import omnia.data.structure.DirectedGraph;
import omnia.data.structure.DirectedGraph.DirectedNode;
import omnia.data.structure.List;
import omnia.data.structure.Map;
import omnia.data.structure.Set;
import omnia.data.structure.immutable.ImmutableMap;
import omnia.data.structure.mutable.HashMap;
import omnia.data.structure.mutable.MutableMap;
import omnia.data.structure.tuple.Tuple;
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
        .map(couple -> Tuple.of(couple.second(), assignColumns(couple.first(), couple.second())))
        .firstOrError()
        .flatMap(couple -> renderGraph(couple.first(), couple.second()));
  }

  private Map<Task, Integer> assignColumns(DirectedGraph<Task> taskGraph, List<Task> taskList) {
    MutableMap<Task, Integer> assignedColumns = HashMap.create();
    int unresolvedEdges = 0;

    for (Task taskToAssign : taskList) {
      Set<? extends DirectedNode<Task>> predecessors =
          taskGraph.nodeOf(taskToAssign).get().predecessors();

      if (predecessors.isPopulated()) {

        Task predecessorWithMinColumn =
            predecessors.stream()
                .map(DirectedNode::item)
                .reduce(
                    (p1, p2) ->
                        assignedColumns.valueOf(p1).get() < assignedColumns.valueOf(p2).get()
                            ? p1
                            : p2)
                .get();

        int columnOfPredecessor = assignedColumns.valueOf(predecessorWithMinColumn).get();

        int resolvedEdgesOfPredecessor =
            taskGraph.nodeOf(predecessorWithMinColumn).get()
                .successors()
                .stream()
                .map(DirectedNode::item)
                .reduce(0,
                    (sum, successor) ->
                        sum + (assignedColumns.valueOf(successor).isPresent() ? 1 : 0),
                    Integer::sum);

        int assignedColumn = columnOfPredecessor + resolvedEdgesOfPredecessor;

        unresolvedEdges--;
        assignedColumns.putMapping(taskToAssign, assignedColumn);

      } else {
        int assignedColumn = unresolvedEdges;

        unresolvedEdges--;
        assignedColumns.putMapping(taskToAssign, assignedColumn);
      }
    }

    return ImmutableMap.copyOf(assignedColumns);
  }

  private Single<Output> renderGraph(List<Task> tasks, Map<Task, Integer> columns) {
    return Observable.fromIterable(tasks)
        .map(Task::render)
        .collectInto(Output.builder(), Output.Builder::appendLine)
        .map(Output.Builder::build);
  }
}
