package me.strangepan.tasks.cli.feature.graph

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.util.Comparator
import java.util.Optional
import kotlin.math.max
import kotlin.math.min
import omnia.algorithm.GraphAlgorithms
import omnia.algorithm.SetAlgorithms
import omnia.cli.out.Output
import omnia.data.cache.Memoized
import omnia.data.stream.Collectors.toImmutableList
import omnia.data.stream.Collectors.toImmutableSet
import omnia.data.structure.Collection
import omnia.data.structure.DirectedGraph
import omnia.data.structure.Graph
import omnia.data.structure.List
import omnia.data.structure.Map
import omnia.data.structure.Set
import omnia.data.structure.immutable.ImmutableDirectedGraph
import omnia.data.structure.immutable.ImmutableList
import omnia.data.structure.immutable.ImmutableMap
import omnia.data.structure.immutable.ImmutableMap.Companion.copyOf
import omnia.data.structure.immutable.ImmutableSet
import omnia.data.structure.mutable.ArrayQueue
import omnia.data.structure.mutable.HashMap
import omnia.data.structure.mutable.HashSet
import omnia.data.structure.mutable.MutableMap
import omnia.data.structure.mutable.MutableSet
import omnia.data.structure.tuple.Tuple
import me.strangepan.tasks.cli.command.common.CommonArguments
import me.strangepan.tasks.cli.handler.ArgumentHandler
import me.strangepan.tasks.engine.model.ObservableTaskStore
import me.strangepan.tasks.engine.model.Task
import me.strangepan.tasks.engine.model.TaskStore
import me.strangepan.tasks.engine.util.rx.Observables
import me.strangepan.tasks.engine.util.rx.Observables.incrementingInteger
import me.strangepan.tasks.engine.util.rx.Observables.toImmutableMap
import me.strangepan.tasks.engine.util.rx.Observables.unwrapOptionals

/** Business logic for the Graph command.  */
class GraphHandler internal constructor(
  private val taskStore: Memoized<out ObservableTaskStore>, private val sorter: TopologicalSorter)
  : ArgumentHandler<GraphArguments> {

  constructor(taskStore: Memoized<out ObservableTaskStore>)
      : this(taskStore, object : TopologicalSorter {
    override fun <T : Task> sort(graph: DirectedGraph<T>): ImmutableList<T> {
      return GraphAlgorithms.topologicallySort(graph)
    }
  })

  override fun handle(arguments: CommonArguments<out GraphArguments>): Single<Output> {
    return taskStore.value()
      .observe()
      .firstOrError()
      .map(TaskStore::taskGraph)
      .flatMap { processAndRenderGraph(arguments, it) }
  }

  private fun <T : Task> processAndRenderGraph(
    arguments: CommonArguments<out GraphArguments>,
    taskGraph: ImmutableDirectedGraph<T>): Single<Output> {
    // a separate method to facilitate generic type safety
    return Single.just(taskGraph)
      .map { maybeStripIrrelevantSubgraphs(it, arguments.specificArguments()) }
      .map { maybeStripCompletedTasks(it, arguments.specificArguments()) }
      .map { Tuple.of(it, sorter.sort(it)) }
      .map { it.append(assignColumns(it.first(), it.second(), arguments.specificArguments())) }
      .map { renderGraph(it.first(), it.second(), it.third(), arguments.specificArguments()) }
  }

  internal interface TopologicalSorter {
    fun <T : Task> sort(graph: DirectedGraph<T>): ImmutableList<T>
  }

  companion object {
    // each of these are package-private so that the unit tests can use them for parsing
    const val CONTINUATION_UP_DOWN: Char = '╎'
    const val EDGE_UP_DOWN_RIGHT: Char = '├'
    const val EDGE_UP_DOWN_LEFT: Char = '┤'
    const val EDGE_UP_DOWN: Char = '│'
    const val EDGE_UP_DOWN_LEFT_RIGHT: Char = '┼'
    const val EDGE_UP_RIGHT: Char = '└'
    const val EDGE_UP_LEFT: Char = '┘'
    const val EDGE_UP_LEFT_RIGHT: Char = '┴'
    const val EDGE_DOWN_RIGHT: Char = '┌'
    const val EDGE_DOWN_LEFT: Char = '┐'
    const val EDGE_DOWN_LEFT_RIGHT: Char = '┬'
    private const val EDGE_LEFT_RIGHT: Char = '─'
    const val GAP: Char = ' '
    const val NODE_COMPLETED: Char = '☑'
    const val NODE_OPEN: Char = '☐'

    private fun <T : Task> maybeStripIrrelevantSubgraphs(
        taskGraph: DirectedGraph<T>, arguments: GraphArguments): DirectedGraph<T> {
      return if (arguments.tasksToRelateTo.isPopulated || arguments.tasksToGetBlockersOf.isPopulated)
        Observable.merge(
            Observable.fromIterable(arguments.tasksToRelateTo)
              .map(taskGraph::nodeOfUnknownType)
              .compose(unwrapOptionals())
              .flatMapIterable(GraphAlgorithms::findOtherNodesInSubgraphContaining),
            Observable.fromIterable(arguments.tasksToGetBlockersOf)
              .map(taskGraph::nodeOfUnknownType)
              .compose(unwrapOptionals())
              .to(Observables.toImmutableSet())
              .map(::findAllBlockersOf)
              .flatMapObservable { Observable.fromIterable(it) })
          .to(Observables.toImmutableSet())
          .map { SetAlgorithms.differenceBetween(taskGraph.nodes(), it) }
          .flatMapObservable { Observable.fromIterable(it) }
          .map(Graph.Node<T>::item)
          .collect(
            { ImmutableDirectedGraph.buildUpon(taskGraph) },
            ImmutableDirectedGraph.Builder<T>::removeNode)
          .map(ImmutableDirectedGraph.Builder<T>::build)
          .blockingGet()
      else
        taskGraph
    }

    private fun <T : DirectedGraph.DirectedNode<*>> findAllBlockersOf(nodes: Collection<T>):
        ImmutableSet<T> {
      val seenNodes = HashSet.copyOf(nodes)
      val queue = ArrayQueue.create<T>()
      for (node in nodes) {
        queue.enqueue(node)
      }
      while (queue.isPopulated) {
        val node = queue.dequeue().orElseThrow()
        for (predecessor in node.predecessors()) {
          @Suppress("UNCHECKED_CAST")
          if (predecessor as T !in seenNodes) {
            seenNodes.add(predecessor)
            queue.enqueue(predecessor)
          }
        }
      }
      return ImmutableSet.copyOf(seenNodes)
    }

    private fun <T : Task> maybeStripCompletedTasks(
        taskGraph: DirectedGraph<T>, arguments: GraphArguments): DirectedGraph<T> {
      return if (arguments.isAllSet) taskGraph else {
        Observable.fromIterable(taskGraph.contents())
          .filter { it.status.isCompleted }
          .collect(
            { ImmutableDirectedGraph.buildUpon(taskGraph) },
            ImmutableDirectedGraph.Builder<T>::removeNode)
          .map(ImmutableDirectedGraph.Builder<T>::build)
          .blockingGet()
      }
    }

    /**
     * Assigns columns to each graph node for CLI rendering.
     *
     *
     * Columns are assigned eagerly, favoring smaller indexes. Linearly traverses the topological
     * list of nodes and assigns columns to each node's successors if the successor doesn't yet have
     * a column assignment.
     *
     * @param taskGraph The graph containing the edges for each item in the task list. Must contain
     * every node in `taskList`.
     * @param taskList a topologically sorted list of tasks to assign columns for, where each node
     * precedes its successors
     * @return A mapping of column assignments for each node, where 0 is the first column. Every node
     * passed into `taskList` will have an entry in the result.
     */
    private fun <T : Task> assignColumns(
      taskGraph: DirectedGraph<T>, taskList: List<T>, arguments: GraphArguments): Map<T, Int> {
      val assignedColumns: MutableMap<T, Int> = HashMap.create()
      val unresolvedSuccessors: MutableSet<T> = HashSet.create()
      val topologicalIndexes: ImmutableMap<T, Int> =
        Observable.fromIterable(taskList)
          .zipWith(incrementingInteger(), Tuple::of)
          .to(toImmutableMap())
          .blockingGet()
      for (taskToAssign in taskList) {
        // scan for the first available column not already claimed by a node later in the list
        val assignedColumn = assignedColumns.putMappingIfAbsent(
          taskToAssign,
          {
            unresolvedSuccessors.stream().map(assignedColumns::valueOf)
              .flatMap(Optional<Int>::stream)
              .reduce(0) { candidate, occupied -> max(candidate, occupied + 1) }
          })
        unresolvedSuccessors.remove(taskToAssign)
        val successorsToAssign: ImmutableList<T> = taskGraph.nodeOf(taskToAssign)
          .map(DirectedGraph.DirectedNode<T>::successors)
          .orElse(ImmutableSet.empty())
          .stream()
          .map(Graph.Node<T>::item)
          .filter { assignedColumns.valueOf(it).isEmpty }
          .filter { arguments.isAllSet || !it.status.isCompleted }
          .sorted(
            Comparator.comparing { item: T -> topologicalIndexes.valueOf(item).orElse(0) }
              .reversed())
          .collect(toImmutableList())
        var successorColumn = assignedColumn
        for (successor in successorsToAssign) {
          assignedColumns.putMapping(successor, successorColumn)
          unresolvedSuccessors.add(successor)
          successorColumn++
        }
      }
      return copyOf(assignedColumns)
    }

    private fun <T : Task> renderGraph(
      taskGraph: DirectedGraph<T>,
      taskList: List<T>,
      taskColumns: Map<T, Int>,
      arguments: GraphArguments): Output {
      val output = Output.builder()
      val columnsWithEdges: MutableSet<Int> = HashSet.create()
      val topologicalIndexes: ImmutableMap<T, Int> =
        Observable.fromIterable(taskList)
          .zipWith(incrementingInteger(), Tuple::of)
          .to(toImmutableMap())
          .blockingGet()
      for (task in taskList) {
        taskColumns.valueOf(task).ifPresent(columnsWithEdges::remove)
        output.appendLine(renderTaskLine(taskColumns, columnsWithEdges, task))
        renderEdgeLine(
          taskGraph, topologicalIndexes, taskColumns, columnsWithEdges, arguments, task)
          .ifPresent(output::appendLine)
        taskGraph.nodeOf(task)
          .map(DirectedGraph.DirectedNode<T>::successors)
          .orElse(ImmutableSet.empty())
          .stream()
          .map(Graph.Node<T>::item)
          .map(taskColumns::valueOf)
          .flatMap(Optional<Int>::stream)
          .forEach(columnsWithEdges::add)
      }
      return output.build()
    }

    private fun <T : Task> renderTaskLine(
      taskColumns: Map<T, Int>, columnsWithEdges: Set<Int>, task: T): Output {
      val taskColumn = taskColumns.valueOf(task).orElse(0)
      val maxColumnsWithEdges = columnsWithEdges.stream().reduce(taskColumn, ::max)
      return incrementingInteger()
        .takeUntil { it == maxColumnsWithEdges }
        .map {
          when {
            it == taskColumn -> (if (task.status.isCompleted) NODE_COMPLETED else NODE_OPEN)
            columnsWithEdges.contains(it) -> CONTINUATION_UP_DOWN
            else -> GAP
          }
        }
        .map(Char::toString)
        .collect(Output::builder, Output.Builder::append)
        .map { it.append(GAP.toString()).append(task.render()) }
        .map(Output.Builder::build)
        .blockingGet()
    }

    private fun <T : Task> renderEdgeLine(
      taskGraph: DirectedGraph<T>,
      topologicalIndexes: Map<T, Int>,
      taskColumns: Map<T, Int>,
      previousColumnsWithEdges: Set<Int>,
      arguments: GraphArguments,
      task: T): Optional<Output> {
      val successorColumns = taskGraph.nodeOf(task)
        .map(DirectedGraph.DirectedNode<T>::successors)
        .orElse(ImmutableSet.empty())
        .stream()
        .map(Graph.Node<T>::item)
        .filter { arguments.isAllSet || !it.status.isCompleted }
        .map(taskColumns::valueOf)
        .flatMap(Optional<Int>::stream)
        .collect(toImmutableSet())
      if (!successorColumns.isPopulated) {
        return Optional.empty()
      }
      val taskColumn = taskColumns.valueOf(task).orElse(0)

      // if there's only one successor and it's not on the next row, but it's in the same column,
      // skip drawing the edge line
      if (successorColumns.count() == 1) {
        val successor: Optional<T> = taskGraph.nodeOf(task)
          .map(DirectedGraph.DirectedNode<T>::successors)
          .orElse(ImmutableSet.empty())
          .stream()
          .findFirst()
          .map(DirectedGraph.DirectedNode<T>::item)
        if (successor.flatMap(taskColumns::valueOf).orElse(0) == taskColumn
          && topologicalIndexes.valueOf(task).map { it + 1 }
          != successor.flatMap(topologicalIndexes::valueOf)) {
          return Optional.empty()
        }
      }
      val lateralEdgeMax = successorColumns.stream().reduce(taskColumn, ::max)
      val lateralEdgeMin = successorColumns.stream().reduce(taskColumn, ::min)
      val maxColumn = previousColumnsWithEdges.stream().reduce(lateralEdgeMax, ::max)
      return incrementingInteger()
        .takeUntil { it == maxColumn }
        .map { columnInteger ->
          when {
            isInRange(lateralEdgeMin, columnInteger, lateralEdgeMax) -> {
              when {
                columnInteger == taskColumn -> {
                  when {
                    successorColumns.contains(columnInteger) -> {
                      when {
                        columnInteger == lateralEdgeMin && columnInteger != lateralEdgeMax ->
                          EDGE_UP_DOWN_RIGHT
                        columnInteger != lateralEdgeMin && columnInteger == lateralEdgeMax ->
                          EDGE_UP_DOWN_LEFT
                        columnInteger == lateralEdgeMin -> EDGE_UP_DOWN
                        else -> EDGE_UP_DOWN_LEFT_RIGHT
                      }
                    }
                    else -> {
                      when {
                        columnInteger == lateralEdgeMin && columnInteger != lateralEdgeMax ->
                          EDGE_UP_RIGHT
                        columnInteger != lateralEdgeMin && columnInteger == lateralEdgeMax ->
                          EDGE_UP_LEFT
                        else -> EDGE_UP_LEFT_RIGHT
                      }
                    }
                  }
                }
                previousColumnsWithEdges.contains(columnInteger) -> {
                  when {
                    successorColumns.contains(columnInteger) -> {
                      when (columnInteger) {
                        lateralEdgeMin -> EDGE_DOWN_RIGHT
                        lateralEdgeMax -> EDGE_DOWN_LEFT
                        else -> EDGE_DOWN_LEFT_RIGHT
                      }
                    }
                    else -> EDGE_LEFT_RIGHT
                  }
                }
                successorColumns.contains(columnInteger) -> {
                  when (columnInteger) {
                    lateralEdgeMin -> EDGE_DOWN_RIGHT
                    lateralEdgeMax -> EDGE_DOWN_LEFT
                    else -> EDGE_DOWN_LEFT_RIGHT
                  }
                }
                else -> EDGE_LEFT_RIGHT
              }
            }
            previousColumnsWithEdges.contains(columnInteger) -> CONTINUATION_UP_DOWN
            else -> GAP
          }
        }
        .map(Char::toString)
        .collect(Output.Companion::builder, Output.Builder::append)
        .map(Output.Builder::build)
        .map { Optional.of(it) }
        .blockingGet()
    }

    private fun isInRange(min: Int, mid: Int, max: Int): Boolean {
      return mid in min..max
    }
  }

}