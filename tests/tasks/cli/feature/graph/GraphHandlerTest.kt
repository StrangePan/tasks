package tasks.cli.feature.graph

import com.google.common.truth.Truth
import com.google.common.truth.Truth8
import io.reactivex.rxjava3.core.Observable
import java.lang.System.lineSeparator
import java.util.Objects
import java.util.OptionalInt
import java.util.regex.Pattern
import omnia.algorithm.ListAlgorithms.binarySearch
import omnia.data.cache.Memoized.Companion.just
import omnia.data.stream.Collectors
import omnia.data.stream.Collectors.toImmutableList
import omnia.data.structure.DirectedGraph
import omnia.data.structure.IntRange
import omnia.data.structure.IntRange.Companion.just
import omnia.data.structure.IntRange.Companion.startingAt
import omnia.data.structure.Set
import omnia.data.structure.immutable.ImmutableDirectedGraph
import omnia.data.structure.immutable.ImmutableList
import omnia.data.structure.immutable.ImmutableMap
import omnia.data.structure.mutable.ArrayList
import omnia.data.structure.mutable.HashMap
import omnia.data.structure.mutable.HashSet
import omnia.data.structure.mutable.MutableList
import omnia.data.structure.mutable.MutableMap
import omnia.data.structure.mutable.MutableSet
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import tasks.cli.command.common.CommonArguments
import tasks.cli.handler.testing.HandlerTestUtils
import tasks.model.ObservableTaskStore
import tasks.model.Task
import tasks.model.TaskBuilder
import tasks.model.impl.ObservableTaskStoreImpl.Companion.createInMemoryStorage
import tasks.util.rx.Observables

@RunWith(JUnit4::class)
class GraphHandlerTest {
  private val taskStore: ObservableTaskStore = createInMemoryStorage()
  private val underTest = GraphHandler(just(taskStore))

  @Test
  fun handle_whenEmpty_outputsNothing() {
    val output = underTest.handle(graphArgsAll()).blockingGet().renderWithoutCodes()
    Truth.assertThat(output).isEmpty()
  }

  @Test
  fun handle_withOneItem_printsGraph() {
    createTask("example task")
    val output = underTest.handle(graphArgsAll()).blockingGet().renderWithoutCodes()
    assertThatOutputRepresentsCurrentTaskGraph(output)
  }

  @Test
  fun handle_withTwoDisconnectedItem_printsGraph() {
    createTask("example task 1")
    createTask("example task 2")
    val output = underTest.handle(graphArgsAll()).blockingGet().renderWithoutCodes()
    assertThatOutputRepresentsCurrentTaskGraph(output)
  }

  @Test
  fun handle_withTwoConnectedItems_printsGraph() {
    val task1 = createTask("example task 1")
    createTask("example task 2") { b: TaskBuilder -> b.addBlockingTask(task1) }
    val output = underTest.handle(graphArgsAll()).blockingGet().renderWithoutCodes()
    assertThatOutputRepresentsCurrentTaskGraph(output)
  }

  @Test
  fun handle_whenThreeConnectedItems_printsGraph() {
    val task1 = createTask("example task 1")
    createTask("example task 2") { b: TaskBuilder -> b.addBlockingTask(task1) }
    createTask("example task 3") { b: TaskBuilder -> b.addBlockingTask(task1) }
    val output = underTest.handle(graphArgsAll()).blockingGet().renderWithoutCodes()
    assertThatOutputRepresentsCurrentTaskGraph(output)
  }

  @Test
  fun handle_whenDiamond_printsGraph() {
    val task1 = createTask("example task 1")
    val task2 = createTask("example task 2") { b: TaskBuilder -> b.addBlockingTask(task1) }
    val task3 = createTask("example task 3") { b: TaskBuilder -> b.addBlockingTask(task1) }
    createTask("example task 4") { b: TaskBuilder -> b.addBlockingTask(task2).addBlockingTask(task3) }
    val output = underTest.handle(graphArgsAll()).blockingGet().renderWithoutCodes()
    assertThatOutputRepresentsCurrentTaskGraph(output)
  }

  @Test
  fun handle_whenWideDiamond_printsGraph() {
    val task1 = createTask("example task 1")
    val task7 = createTask("example task 7")
    createTask("example task 2") { b: TaskBuilder -> b.addBlockingTask(task1).addBlockedTask(task7) }
    createTask("example task 3") { b: TaskBuilder -> b.addBlockingTask(task1).addBlockedTask(task7) }
    createTask("example task 4") { b: TaskBuilder -> b.addBlockingTask(task1).addBlockedTask(task7) }
    createTask("example task 5") { b: TaskBuilder -> b.addBlockingTask(task1).addBlockedTask(task7) }
    createTask("example task 6") { b: TaskBuilder -> b.addBlockingTask(task1).addBlockedTask(task7) }
    val output = underTest.handle(graphArgsAll()).blockingGet().renderWithoutCodes()
    assertThatOutputRepresentsCurrentTaskGraph(output)
  }

  @Test
  fun handle_whenFork_printsGraph() {
    val task1 = createTask("example task 1")
    createTask("example task 2") { b: TaskBuilder -> b.addBlockingTask(task1) }
    createTask("example task 3") { b: TaskBuilder -> b.addBlockingTask(task1) }
    createTask("example task 4") { b: TaskBuilder -> b.addBlockingTask(task1) }
    createTask("example task 5") { b: TaskBuilder -> b.addBlockingTask(task1) }
    createTask("example task 6") { b: TaskBuilder -> b.addBlockingTask(task1) }
    val output = underTest.handle(graphArgsAll()).blockingGet().renderWithoutCodes()
    assertThatOutputRepresentsCurrentTaskGraph(output)
  }

  @Test
  fun handle_whenReverseFork_printsGraph() {
    val task1 = createTask("example task 1")
    createTask("example task 2") { b: TaskBuilder -> b.addBlockedTask(task1) }
    createTask("example task 3") { b: TaskBuilder -> b.addBlockedTask(task1) }
    createTask("example task 4") { b: TaskBuilder -> b.addBlockedTask(task1) }
    createTask("example task 5") { b: TaskBuilder -> b.addBlockedTask(task1) }
    createTask("example task 6") { b: TaskBuilder -> b.addBlockedTask(task1) }
    val output = underTest.handle(graphArgsAll()).blockingGet().renderWithoutCodes()
    assertThatOutputRepresentsCurrentTaskGraph(output)
  }

  @Test
  fun handle_whenChain_whenAll_withVariousStatus_printsGraph() {
    val task1 = createTask("example task 1")
    val task2 = createTask("example task 2") { b: TaskBuilder -> b.addBlockingTask(task1) }
    val task3 = createTask(
        "example task 3") { b: TaskBuilder -> b.addBlockingTask(task2).setStatus(Task.Status.COMPLETED) }
    val task4 = createTask("example task 4") { b: TaskBuilder -> b.addBlockingTask(task3).setStatus(Task.Status.STARTED) }
    createTask("example task 5") { b: TaskBuilder -> b.addBlockingTask(task4) }
    val output = underTest.handle(graphArgsAll()).blockingGet().renderWithoutCodes()
    assertThatOutputRepresentsCurrentTaskGraph(output)
  }

  @Test
  fun handle_whenChain_withSomeCompleted_printsGraph() {
    val task1 = createTask("example task 1")
    val task2 = createTask("example task 2") { b: TaskBuilder -> b.addBlockingTask(task1) }
    val task3 = createTask(
        "example task 3") { b: TaskBuilder -> b.addBlockingTask(task2).setStatus(Task.Status.COMPLETED) }
    val task4 = createTask("example task 4") { b: TaskBuilder -> b.addBlockingTask(task3) }
    createTask("example task 5") { b: TaskBuilder -> b.addBlockingTask(task4) }
    val output = underTest.handle(graphArgs()).blockingGet().renderWithoutCodes()
    assertThatOutputRepresentsGraph(output, stripCompletedTasksFromCurrentGraph())
  }

  @Test
  fun handle_whenWideDiamond_withSomeCompleted_printsGraph() {
    val task1 = createTask("example task 1")
    val task7 = createTask("example task 7")
    createTask("example task 2") { b: TaskBuilder -> b.addBlockingTask(task1).addBlockedTask(task7) }
    createTask(
        "example task 3"
    ) { b: TaskBuilder -> b.addBlockingTask(task1).addBlockedTask(task7).setStatus(Task.Status.COMPLETED) }
    createTask("example task 4") { b: TaskBuilder -> b.addBlockingTask(task1).addBlockedTask(task7) }
    createTask(
        "example task 5"
    ) { b: TaskBuilder -> b.addBlockingTask(task1).addBlockedTask(task7).setStatus(Task.Status.COMPLETED) }
    createTask("example task 6") { b: TaskBuilder -> b.addBlockingTask(task1).addBlockedTask(task7) }
    val output = underTest.handle(graphArgs()).blockingGet().renderWithoutCodes()
    assertThatOutputRepresentsGraph(output, stripCompletedTasksFromCurrentGraph())
  }

  @Test
  fun handle_whenD_printsGraph() {
    val task1 = createTask("example task 1")
    val task2 = createTask("example task 2") { b: TaskBuilder -> b.addBlockingTask(task1) }
    val task3 = createTask("example task 3") { b: TaskBuilder -> b.addBlockingTask(task2) }
    val task4 = createTask("example task 4") { b: TaskBuilder -> b.addBlockingTask(task3) }
    val task5 = createTask("example task 5") { b: TaskBuilder -> b.addBlockingTask(task4) }
    createTask("example task 6") { b: TaskBuilder -> b.addBlockingTask(task1).addBlockingTask(task5) }
    val output = underTest.handle(graphArgsAll()).blockingGet().renderWithoutCodes()
    assertThatOutputRepresentsCurrentTaskGraph(output)
  }

  @Test
  fun handle_whenD_wheSomeCompleted_printsGraph() {
    val task1 = createTask("example task 1")
    val task2 = createTask("example task 2") { b: TaskBuilder -> b.addBlockingTask(task1) }
    val task3 = createTask("example task 3") { b: TaskBuilder -> b.addBlockingTask(task2) }
    val task4 = createTask(
        "example task 4") { b: TaskBuilder -> b.addBlockingTask(task3).setStatus(Task.Status.COMPLETED) }
    val task5 = createTask("example task 5") { b: TaskBuilder -> b.addBlockingTask(task4) }
    createTask("example task 6") { b: TaskBuilder -> b.addBlockingTask(task1).addBlockingTask(task5) }
    val output = underTest.handle(graphArgs()).blockingGet().renderWithoutCodes()
    assertThatOutputRepresentsGraph(output, stripCompletedTasksFromCurrentGraph())
  }

  private fun createTask(label: String): Task {
    return HandlerTestUtils.createTask(taskStore, label)
  }

  private fun createTask(label: String, builderFunction: java.util.function.Function<TaskBuilder, TaskBuilder>): Task {
    return HandlerTestUtils.createTask(taskStore, label, builderFunction)
  }

  private fun stripCompletedTasksFromCurrentGraph(): ImmutableDirectedGraph<out Task> {
    val currentGraph = taskStore.observe().blockingFirst().taskGraph()
    val builder = currentGraph.toBuilder()
    currentGraph.contents()
        .stream()
        .filter { t: Task -> t.status().isCompleted }.forEach { element: Any -> builder.removeUnknownTypedNode(element) }
    return builder.build()
  }

  private fun getUpdatedVersionOf(task: Task): Task {
    return HandlerTestUtils.getUpdatedVersionOf(taskStore, task)
  }

  private fun assertThatOutputRepresentsCurrentTaskGraph(output: String) {
    assertThatOutputRepresentsGraph(output, taskStore.observe().blockingFirst().taskGraph())
  }

  private class Cursor(private val source: String) {

    private val lines: ImmutableList<String> = splitLines(source)
    private val cumulativeLineRanges: ImmutableList<IntRange>
    private var row = 0
    private var col = 0
    fun col(): Int {
      return col
    }

    fun col(col: Int): Cursor {
      this.col = col
      return this
    }

    fun row(): Int {
      return row
    }

    fun row(row: Int): Cursor {
      this.row = row
      return this
    }

    fun up(): Cursor {
      return row(row - 1)
    }

    fun down(): Cursor {
      return row(row + 1)
    }

    fun left(): Cursor {
      return col(col - 1)
    }

    fun right(): Cursor {
      return col(col + 1)
    }

    fun splitLines(output: String): ImmutableList<String> {
      val lines = output.lines().stream().collect(toImmutableList())
      return if (lines.itemAt(lines.count() - 1).isEmpty()) lines.sublistStartingAt(0).length(lines.count() - 1) else lines
    }

    @JvmOverloads
    fun line(offset: Int = 0): String {
      val row = row + offset
      try {
        Objects.checkIndex(row, lines.count())
      } catch (ex: ArrayIndexOutOfBoundsException) {
        throw RuntimeException("row out of bounds. row=$row\n$source", ex)
      }
      return lines.itemAt(row)
    }

    fun lines(): ImmutableList<String> {
      return lines
    }

    @JvmOverloads
    fun codePoint(offsetRow: Int = 0, offsetCol: Int = 0): Char {
      val line = line(offsetRow)
      val col = col + offsetCol
      try {
        Objects.checkIndex(col, line.length)
      } catch (ex: ArrayIndexOutOfBoundsException) {
        throw RuntimeException(
            "cursor out of bounds. row=" +
                row +
                offsetRow +
                " col=" +
                col +
                lineSeparator() +
                source,
            ex)
      }
      return line[col]
    }

    fun jumpTo(substring: String): Cursor {
      val index = source.indexOf(substring)
      if (index == -1) {
        throw RuntimeException(
            "substring not found in source"
                + lineSeparator()
                + "substring="
                + substring
                + lineSeparator()
                + "source="
                + source)
      }
      row = binarySearch(
          cumulativeLineRanges,
          just(index),
          { a: IntRange, b: IntRange -> if (a.end() <= b.start()) -1 else if (a.start() > b.start()) 1 else 0 })
          .orElseThrow()
      col = index - cumulativeLineRanges.itemAt(row).start()
      return this
    }

    init {
      cumulativeLineRanges = Observable.fromIterable(lines)
          .scan(
              startingAt(0).withLength(0),
              { previousRange: IntRange, line: String ->
                startingAt(previousRange.end())
                    .withLength(line.length + lineSeparator().length)
              })
          .skip(1)
          .to(Observables.toImmutableList())
          .blockingGet()
    }
  }

  companion object {
    private fun graphArgs(): CommonArguments<GraphArguments> {
      return HandlerTestUtils.commonArgs<GraphArguments>(GraphArguments( /* isAllSet= */false))
    }

    private fun graphArgsAll(): CommonArguments<GraphArguments> {
      return HandlerTestUtils.commonArgs<GraphArguments>(GraphArguments( /* isAllSet= */true))
    }

    private fun <T : Task> assertThatOutputRepresentsGraph(
        output: String, graph: DirectedGraph<T>) {
      val cursor = Cursor(output)
      val nodePattern = Pattern.compile(
          Pattern.quote(GraphHandler.NODE_OPEN.toString())
              + "|"
              + Pattern.quote(GraphHandler.NODE_COMPLETED.toString()))
      for (expectedTask in graph.contents()) {
        Truth.assertThat(output).contains(expectedTask.render().renderWithoutCodes())
      }
      val taskRows: ImmutableMap<T, Int> = Observable.fromIterable(graph.nodes())
          .map { it.item() }
          .to(Observables.toImmutableMap({ it }, { cursor.jumpTo(it.render().renderWithoutCodes()).row() }))
          .blockingGet()
      val tasksByRow = taskRows.entries().stream().collect(
          Collectors.toImmutableMap({ it.value() }, { it.key() }))
      cursor.row(0).col(0)
      val parsedGraphBuilder: ImmutableDirectedGraph.Builder<T> = ImmutableDirectedGraph.builder()
      val unfinishedEdges: MutableMap<Int, MutableSet<T>> = HashMap.create()
      while (cursor.row() < cursor.lines().count()) {
        val row = cursor.row()
        val line = cursor.line()
        val nodeMatcher = nodePattern.matcher(line)
        val colOfNodeOptional = if (nodeMatcher.find()) OptionalInt.of(nodeMatcher.start()) else OptionalInt.empty()
        colOfNodeOptional.ifPresentOrElse(
            { colOfNode: Int ->
              // this line should contain a task AND continuations of any unfinished edges
              val taskAtCurrentRow = tasksByRow.valueOf(row).orElseThrow()
              parsedGraphBuilder.addNode(taskAtCurrentRow)
              for (predecessor in unfinishedEdges.valueOf(colOfNode)
                  .map { it as Set<T> }
                  .orElse(Set.empty())) {
                parsedGraphBuilder.addEdge(predecessor, taskAtCurrentRow)
                unfinishedEdges.removeKey(colOfNode)
              }
              val columnTaskRenderingStarts = line.indexOf(taskAtCurrentRow.render().renderWithoutCodes())
              for (c in 0 until columnTaskRenderingStarts) {
                val codePoint = cursor.col(c).codePoint()
                if (codePoint == GraphHandler.CONTINUATION_UP_DOWN && (cursor.codePoint(-1, 0) == GraphHandler.NODE_OPEN || cursor.codePoint(-1, 0) == GraphHandler.NODE_COMPLETED)) {
                  unfinishedEdges.putMappingIfAbsent(c) { HashSet.create() }
                      .add(tasksByRow.valueOf(row - 1).orElseThrow())
                }
                Truth.assertWithMessage("mismatch at row ${cursor.row()} col ${cursor.col()}${lineSeparator()}$output")
                    .that(codePoint)
                    .isEqualTo(if (unfinishedEdges.keys().contains(c)) GraphHandler.CONTINUATION_UP_DOWN else if (c == colOfNode) if (taskAtCurrentRow.status().isCompleted) GraphHandler.NODE_COMPLETED else GraphHandler.NODE_OPEN else GraphHandler.GAP)
              }
            }
        ) {

          // this is an edge-only line
          // see what columns the previous task maps too (necessary for this line to exist)
          var incomingColumn = OptionalInt.empty()
          val outgoingColumns: MutableList<Int> = ArrayList.create()
          for (c in line.indices) {
            val codePoint = cursor.col(c).codePoint()
            if (codePoint == GraphHandler.EDGE_UP_DOWN_LEFT_RIGHT
                || codePoint == GraphHandler.EDGE_UP_DOWN_LEFT
                || codePoint == GraphHandler.EDGE_UP_DOWN_RIGHT
                || codePoint == GraphHandler.EDGE_UP_DOWN
                || codePoint == GraphHandler.EDGE_UP_LEFT
                || codePoint == GraphHandler.EDGE_UP_RIGHT) {
              Truth8.assertThat(incomingColumn).isEmpty()
              incomingColumn = OptionalInt.of(c)
            }
            if (codePoint == GraphHandler.EDGE_UP_DOWN_LEFT_RIGHT
                || codePoint == GraphHandler.EDGE_UP_DOWN_LEFT
                || codePoint == GraphHandler.EDGE_UP_DOWN_RIGHT
                || codePoint == GraphHandler.EDGE_UP_DOWN
                || codePoint == GraphHandler.EDGE_DOWN_LEFT_RIGHT
                || codePoint == GraphHandler.EDGE_DOWN_LEFT
                || codePoint == GraphHandler.EDGE_DOWN_RIGHT) {
              outgoingColumns.add(c)
            }
            if (codePoint == GraphHandler.CONTINUATION_UP_DOWN) {
              Truth.assertThat(unfinishedEdges.keys().contains(c)).isTrue()
            }
            if (codePoint == GraphHandler.GAP) {
              Truth.assertThat(unfinishedEdges.keys().contains(c)).isFalse()
            }
          }
          incomingColumn.orElseThrow { AssertionError("expected to be present. line=${cursor.row()}${lineSeparator()}$output") }
          Truth.assertThat(outgoingColumns.isPopulated).isTrue()
          val codePointOfNode = cursor.col(incomingColumn.orElseThrow()).codePoint(-1, 0)
          Truth.assertThat(codePointOfNode)
              .isAnyOf(GraphHandler.NODE_OPEN, GraphHandler.NODE_COMPLETED)
          val taskAtPreviousLine = tasksByRow.valueOf(cursor.row() - 1).orElseThrow()
          for (outgoingColumn in outgoingColumns) {
            unfinishedEdges.putMappingIfAbsent(outgoingColumn) { HashSet.create() }
                .add(taskAtPreviousLine)
          }
        }
        cursor.down()
      }
      Truth.assertThat(unfinishedEdges.entries().isPopulated).isFalse()
      val parsedGraph = parsedGraphBuilder.build()
      for (originalEdge in graph.edges()) {
        Truth.assertWithMessage(String.format(
            "Rendered graph doesn't contain edge %s%s%s",
            originalEdge,
            lineSeparator(),
            output))
            .that(
                parsedGraph.edgeOf(
                    originalEdge.start().item(), originalEdge.end().item()).isPresent)
            .isTrue()
      }
      for (originalNode in graph.nodes()) {
        Truth.assertWithMessage(String.format(
            "Rendered graph doesn't contain task %s%s%s",
            originalNode,
            lineSeparator(),
            output))
            .that(parsedGraph.nodeOf(originalNode.item()).isPresent).isTrue()
      }
      Truth.assertThat(parsedGraph.edges().count()).isEqualTo(graph.edges().count())
      Truth.assertThat(parsedGraph.nodes().count()).isEqualTo(graph.nodes().count())
    }
  }
}