package tasks.cli.feature.graph;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static com.google.common.truth.Truth8.assertThat;
import static java.lang.System.lineSeparator;
import static java.util.Objects.checkIndex;
import static tasks.cli.handler.testing.HandlerTestUtils.commonArgs;
import static tasks.util.rx.Observables.toImmutableMap;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import java.util.OptionalInt;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import omnia.algorithm.ListAlgorithms;
import omnia.data.cache.Memoized;
import omnia.data.stream.Collectors;
import omnia.data.structure.DirectedGraph;
import omnia.data.structure.Graph;
import omnia.data.structure.IntRange;
import omnia.data.structure.Map;
import omnia.data.structure.Set;
import omnia.data.structure.immutable.ImmutableDirectedGraph;
import omnia.data.structure.immutable.ImmutableList;
import omnia.data.structure.immutable.ImmutableMap;
import omnia.data.structure.mutable.ArrayList;
import omnia.data.structure.mutable.HashMap;
import omnia.data.structure.mutable.HashSet;
import omnia.data.structure.mutable.MutableList;
import omnia.data.structure.mutable.MutableMap;
import omnia.data.structure.mutable.MutableSet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import tasks.cli.command.common.CommonArguments;
import tasks.cli.handler.testing.HandlerTestUtils;
import tasks.model.ObservableTaskStore;
import tasks.model.Task;
import tasks.model.TaskBuilder;
import tasks.model.impl.ObservableTaskStoreImpl;
import tasks.util.rx.Observables;

@RunWith(JUnit4.class)
public final class GraphHandlerTest {

  private final ObservableTaskStore taskStore = ObservableTaskStoreImpl.createInMemoryStorage();

  private final GraphHandler underTest =
      new GraphHandler(Memoized.just(taskStore));

  @Test
  public void handle_whenEmpty_outputsNothing() {
    String output = underTest.handle(graphArgsAll()).blockingGet().renderWithoutCodes();

    assertThat(output).isEmpty();
  }

  @Test
  public void handle_withOneItem_printsGraph() {
    createTask("example task");

    String output = underTest.handle(graphArgsAll()).blockingGet().renderWithoutCodes();


    assertThatOutputRepresentsCurrentTaskGraph(output);
  }

  @Test
  public void handle_withTwoDisconnectedItem_printsGraph() {
    createTask("example task 1");
    createTask("example task 2");

    String output = underTest.handle(graphArgsAll()).blockingGet().renderWithoutCodes();


    assertThatOutputRepresentsCurrentTaskGraph(output);
  }

  @Test
  public void handle_withTwoConnectedItems_printsGraph() {
    Task task1 = createTask("example task 1");
    createTask("example task 2", b -> b.addBlockingTask(task1));

    String output = underTest.handle(graphArgsAll()).blockingGet().renderWithoutCodes();

    assertThatOutputRepresentsCurrentTaskGraph(output);
  }

  @Test
  public void handle_whenThreeConnectedItems_printsGraph() {
    Task task1 = createTask("example task 1");
    createTask("example task 2", b -> b.addBlockingTask(task1));
    createTask("example task 3", b -> b.addBlockingTask(task1));

    String output = underTest.handle(graphArgsAll()).blockingGet().renderWithoutCodes();

    assertThatOutputRepresentsCurrentTaskGraph(output);
  }

  @Test
  public void handle_whenDiamond_printsGraph() {
    Task task1 = createTask("example task 1");
    Task task2 = createTask("example task 2", b -> b.addBlockingTask(task1));
    Task task3 = createTask("example task 3", b -> b.addBlockingTask(task1));
    createTask("example task 4", b -> b.addBlockingTask(task2).addBlockingTask(task3));

    String output = underTest.handle(graphArgsAll()).blockingGet().renderWithoutCodes();

    assertThatOutputRepresentsCurrentTaskGraph(output);
  }

  @Test
  public void handle_whenWideDiamond_printsGraph() {
    Task task1 = createTask("example task 1");
    Task task7 = createTask("example task 7");
    createTask("example task 2", b -> b.addBlockingTask(task1).addBlockedTask(task7));
    createTask("example task 3", b -> b.addBlockingTask(task1).addBlockedTask(task7));
    createTask("example task 4", b -> b.addBlockingTask(task1).addBlockedTask(task7));
    createTask("example task 5", b -> b.addBlockingTask(task1).addBlockedTask(task7));
    createTask("example task 6", b -> b.addBlockingTask(task1).addBlockedTask(task7));

    String output = underTest.handle(graphArgsAll()).blockingGet().renderWithoutCodes();

    assertThatOutputRepresentsCurrentTaskGraph(output);
  }

  @Test
  public void handle_whenFork_printsGraph() {
    Task task1 = createTask("example task 1");
    createTask("example task 2", b -> b.addBlockingTask(task1));
    createTask("example task 3", b -> b.addBlockingTask(task1));
    createTask("example task 4", b -> b.addBlockingTask(task1));
    createTask("example task 5", b -> b.addBlockingTask(task1));
    createTask("example task 6", b -> b.addBlockingTask(task1));

    String output = underTest.handle(graphArgsAll()).blockingGet().renderWithoutCodes();

    assertThatOutputRepresentsCurrentTaskGraph(output);
  }

  @Test
  public void handle_whenReverseFork_printsGraph() {
    Task task1 = createTask("example task 1");
    createTask("example task 2", b -> b.addBlockedTask(task1));
    createTask("example task 3", b -> b.addBlockedTask(task1));
    createTask("example task 4", b -> b.addBlockedTask(task1));
    createTask("example task 5", b -> b.addBlockedTask(task1));
    createTask("example task 6", b -> b.addBlockedTask(task1));

    String output = underTest.handle(graphArgsAll()).blockingGet().renderWithoutCodes();

    assertThatOutputRepresentsCurrentTaskGraph(output);
  }

  @Test
  public void handle_whenChain_whenAll_withVariousStatus_printsGraph() {
    Task task1 = createTask("example task 1");
    Task task2 = createTask("example task 2", b -> b.addBlockingTask(task1));
    Task task3 =
        createTask(
            "example task 3", b -> b.addBlockingTask(task2).setStatus(Task.Status.COMPLETED));
    Task task4 =
        createTask("example task 4", b -> b.addBlockingTask(task3).setStatus(Task.Status.STARTED));
    createTask("example task 5", b -> b.addBlockingTask(task4));

    String output = underTest.handle(graphArgsAll()).blockingGet().renderWithoutCodes();

    assertThatOutputRepresentsCurrentTaskGraph(output);
  }

  @Test
  public void handle_whenChain_withSomeCompleted_printsGraph() {
    Task task1 = createTask("example task 1");
    Task task2 = createTask("example task 2", b -> b.addBlockingTask(task1));
    Task task3 = createTask(
        "example task 3", b -> b.addBlockingTask(task2).setStatus(Task.Status.COMPLETED));
    Task task4 = createTask("example task 4", b -> b.addBlockingTask(task3));
    createTask("example task 5", b -> b.addBlockingTask(task4));

    String output = underTest.handle(graphArgs()).blockingGet().renderWithoutCodes();

    assertThatOutputRepresentsGraph(output, stripCompletedTasksFromCurrentGraph());
  }

  @Test
  public void handle_whenWideDiamond_withSomeCompleted_printsGraph() {
    Task task1 = createTask("example task 1");
    Task task7 = createTask("example task 7");
    createTask("example task 2", b -> b.addBlockingTask(task1).addBlockedTask(task7));
    createTask(
        "example task 3",
        b -> b.addBlockingTask(task1).addBlockedTask(task7).setStatus(Task.Status.COMPLETED));
    createTask("example task 4", b -> b.addBlockingTask(task1).addBlockedTask(task7));
    createTask(
        "example task 5",
        b -> b.addBlockingTask(task1).addBlockedTask(task7).setStatus(Task.Status.COMPLETED));
    createTask("example task 6", b -> b.addBlockingTask(task1).addBlockedTask(task7));

    String output = underTest.handle(graphArgs()).blockingGet().renderWithoutCodes();

    assertThatOutputRepresentsGraph(output, stripCompletedTasksFromCurrentGraph());
  }

  @Test
  public void handle_whenD_printsGraph() {
    Task task1 = createTask("example task 1");
    Task task2 = createTask("example task 2", b -> b.addBlockingTask(task1));
    Task task3 = createTask("example task 3", b -> b.addBlockingTask(task2));
    Task task4 = createTask("example task 4", b -> b.addBlockingTask(task3));
    Task task5 = createTask("example task 5", b -> b.addBlockingTask(task4));
    Task task6 = createTask("example task 6", b -> b.addBlockingTask(task1).addBlockingTask(task5));

    String output = underTest.handle(graphArgsAll()).blockingGet().renderWithoutCodes();

    assertThatOutputRepresentsCurrentTaskGraph(output);
  }

  @Test
  public void handle_whenD_wheSomeCompleted_printsGraph() {
    Task task1 = createTask("example task 1");
    Task task2 = createTask("example task 2", b -> b.addBlockingTask(task1));
    Task task3 = createTask("example task 3", b -> b.addBlockingTask(task2));
    Task task4 = createTask(
        "example task 4", b -> b.addBlockingTask(task3).setStatus(Task.Status.COMPLETED));
    Task task5 = createTask("example task 5", b -> b.addBlockingTask(task4));
    Task task6 = createTask("example task 6", b -> b.addBlockingTask(task1).addBlockingTask(task5));

    String output = underTest.handle(graphArgs()).blockingGet().renderWithoutCodes();

    assertThatOutputRepresentsGraph(output, stripCompletedTasksFromCurrentGraph());
  }

  private static CommonArguments<GraphArguments> graphArgs() {
    return commonArgs(new GraphArguments(/* isAllSet= */ false));
  }

  private static CommonArguments<GraphArguments> graphArgsAll() {
    return commonArgs(new GraphArguments(/* isAllSet= */ true));
  }

  private Task createTask(String label) {
    return HandlerTestUtils.createTask(taskStore, label);
  }

  private Task createTask(String label, Function<TaskBuilder, TaskBuilder> builderFunction) {
    return HandlerTestUtils.createTask(taskStore, label, builderFunction);
  }

  private ImmutableDirectedGraph<? extends Task> stripCompletedTasksFromCurrentGraph() {
    ImmutableDirectedGraph<? extends Task> currentGraph =
        taskStore.observe().blockingFirst().taskGraph();

    ImmutableDirectedGraph.Builder<? extends Task> builder = currentGraph.toBuilder();

    currentGraph.contents()
        .stream()
        .filter(t -> t.status().isCompleted()).forEach(builder::removeUnknownTypedNode);

    return builder.build();
  }

  private Task getUpdatedVersionOf(Task task) {
    return HandlerTestUtils.getUpdatedVersionOf(taskStore, task);
  }

  private void assertThatOutputRepresentsCurrentTaskGraph(String output) {
    assertThatOutputRepresentsGraph(output, taskStore.observe().blockingFirst().taskGraph());
  }

  private static <T extends Task> void assertThatOutputRepresentsGraph(
      String output, DirectedGraph<T> graph) {
    Cursor cursor = new Cursor(output);

    Pattern nodePattern =
        Pattern.compile(
            Pattern.quote(Character.toString(GraphHandler.NODE_OPEN))
                + "|"
                + Pattern.quote(Character.toString(GraphHandler.NODE_COMPLETED)));

    for (Task expectedTask : graph.contents()) {
      assertThat(output).contains(expectedTask.render().renderWithoutCodes());
    }


    ImmutableMap<T, Integer> taskRows =
        Observable.fromIterable(graph.nodes())
            .map(Graph.Node::item)
            .to(
                toImmutableMap(
                    task -> task, task -> cursor.jumpTo(task.render().renderWithoutCodes()).row()))
            .blockingGet();
    ImmutableMap<Integer, T> tasksByRow =
        taskRows.entries().stream().collect(
            Collectors.toImmutableMap(Map.Entry::value, Map.Entry::key));

    cursor.row(0).col(0);

    ImmutableDirectedGraph.Builder<T> parsedGraphBuilder = ImmutableDirectedGraph.builder();
    MutableMap<Integer, MutableSet<T>> unfinishedEdges = HashMap.create();

    while (cursor.row() < cursor.lines().count()) {
      int row = cursor.row();
      String line = cursor.line();

      Matcher nodeMatcher = nodePattern.matcher(line);
      OptionalInt colOfNodeOptional =
          nodeMatcher.find() ? OptionalInt.of(nodeMatcher.start()) : OptionalInt.empty();

      colOfNodeOptional.ifPresentOrElse(
          colOfNode -> {
            // this line should contain a task AND continuations of any unfinished edges

            T taskAtCurrentRow = tasksByRow.valueOf(row).orElseThrow();

            parsedGraphBuilder.addNode(taskAtCurrentRow);

            for (T predecessor
                : unfinishedEdges.valueOf(colOfNode)
                    .map(set -> (Set<T>) set)
                    .orElse(Set.empty())) {
              parsedGraphBuilder.addEdge(predecessor, taskAtCurrentRow);
              unfinishedEdges.removeKey(colOfNode);
            }

            int columnTaskRenderingStarts =
                line.indexOf(taskAtCurrentRow.render().renderWithoutCodes());

            for (int c = 0; c < columnTaskRenderingStarts; c++) {
              int codePoint = cursor.col(c).codePoint();
              if (codePoint == GraphHandler.CONTINUATION_UP_DOWN
                  && (cursor.codePoint(-1, 0) == GraphHandler.NODE_OPEN
                      || cursor.codePoint(-1, 0) == GraphHandler.NODE_COMPLETED)) {
                unfinishedEdges.putMappingIfAbsent(c, HashSet::create)
                    .add(tasksByRow.valueOf(row - 1).orElseThrow());
              }

              assertWithMessage(
                  String.format(
                      "mismatch at row %d col %d%s%s",
                      cursor.row(),
                      cursor.col(),
                      lineSeparator(),
                      output))
                  .that(codePoint)
                  .isEqualTo(unfinishedEdges.keys().contains(c)
                      ? GraphHandler.CONTINUATION_UP_DOWN
                      : c == colOfNode
                          ? taskAtCurrentRow.status().isCompleted()
                              ? GraphHandler.NODE_COMPLETED
                              : GraphHandler.NODE_OPEN
                          : GraphHandler.GAP);
            }
          },
          () -> {
            // this is an edge-only line
            // see what columns the previous task maps too (necessary for this line to exist)

            OptionalInt incomingColumn = OptionalInt.empty();
            MutableList<Integer> outgoingColumns = ArrayList.create();

            for (int c = 0; c < line.length(); c++) {
              int codePoint = cursor.col(c).codePoint();
              if (codePoint == GraphHandler.EDGE_UP_DOWN_LEFT_RIGHT
                  || codePoint == GraphHandler.EDGE_UP_DOWN_LEFT
                  || codePoint == GraphHandler.EDGE_UP_DOWN_RIGHT
                  || codePoint == GraphHandler.EDGE_UP_DOWN
                  || codePoint == GraphHandler.EDGE_UP_LEFT
                  || codePoint == GraphHandler.EDGE_UP_RIGHT) {
                assertThat(incomingColumn).isEmpty();
                incomingColumn = OptionalInt.of(c);
              }

              if (codePoint == GraphHandler.EDGE_UP_DOWN_LEFT_RIGHT
                  || codePoint == GraphHandler.EDGE_UP_DOWN_LEFT
                  || codePoint == GraphHandler.EDGE_UP_DOWN_RIGHT
                  || codePoint == GraphHandler.EDGE_UP_DOWN
                  || codePoint == GraphHandler.EDGE_DOWN_LEFT_RIGHT
                  || codePoint == GraphHandler.EDGE_DOWN_LEFT
                  || codePoint == GraphHandler.EDGE_DOWN_RIGHT) {
                outgoingColumns.add(c);
              }

              if (codePoint == GraphHandler.CONTINUATION_UP_DOWN) {
                assertThat(unfinishedEdges.keys().contains(c)).isTrue();
              }

              if (codePoint == GraphHandler.GAP) {
                assertThat(unfinishedEdges.keys().contains(c)).isFalse();
              }
            }

            assertThat(incomingColumn).isPresent();
            assertThat(outgoingColumns.isPopulated()).isTrue();

            int codePointOfNode = cursor.col(incomingColumn.orElseThrow()).codePoint(-1, 0);
            assertThat(codePointOfNode)
                .isAnyOf(GraphHandler.NODE_OPEN, GraphHandler.NODE_COMPLETED);

            T taskAtPreviousLine = tasksByRow.valueOf(cursor.row() - 1).orElseThrow();
            for (int outgoingColumn : outgoingColumns) {
              unfinishedEdges.putMappingIfAbsent(outgoingColumn, HashSet::create)
                  .add(taskAtPreviousLine);
            }
          });

      cursor.down();
    }

    assertThat(unfinishedEdges.entries().isPopulated()).isFalse();

    ImmutableDirectedGraph<T> parsedGraph = parsedGraphBuilder.build();

    for (DirectedGraph.DirectedEdge<T> originalEdge : graph.edges()) {
      assertWithMessage(
              String.format(
                  "Rendered graph doesn't contain edge %s%s%s",
                  originalEdge,
                  lineSeparator(),
                  output))
          .that(
              parsedGraph.edgeOf(
                  originalEdge.start().item(), originalEdge.end().item()).isPresent())
          .isTrue();
    }
    for (DirectedGraph.DirectedNode<T> originalNode : graph.nodes()) {
      assertWithMessage(
              String.format(
                  "Rendered graph doesn't contain task %s%s%s",
                  originalNode,
                  lineSeparator(),
                  output))
          .that(parsedGraph.nodeOf(originalNode.item()).isPresent()).isTrue();
    }
    assertThat(parsedGraph.edges().count()).isEqualTo(graph.edges().count());
    assertThat(parsedGraph.nodes().count()).isEqualTo(graph.nodes().count());
  }

  private static final class Cursor {
    private final String source;
    private final ImmutableList<String> lines;
    private final ImmutableList<IntRange> cumulativeLineRanges;
    private int row = 0;
    private int col = 0;

    Cursor(String source) {
      this.source = source;
      this.lines = source.lines().collect(Collectors.toImmutableList());
      this.cumulativeLineRanges =
          Observable.fromIterable(lines)
              .scan(
                  IntRange.startingAt(0).withLength(0),
                  (previousRange, line) ->
                      IntRange.startingAt(previousRange.end())
                          .withLength(line.length() + lineSeparator().length()))
              .skip(1)
              .to(Observables.toImmutableList())
              .blockingGet();
    }

    int col() {
      return col;
    }

    Cursor col(int col) {
      this.col = col;
      return this;
    }

    int row() {
      return row;
    }

    Cursor row(int row) {
      this.row = row;
      return this;
    }

    Cursor up() {
      return row(this.row - 1);
    }

    Cursor down() {
      return row(this.row + 1);
    }

    Cursor left() {
      return col(this.col - 1);
    }

    Cursor right() {
      return col(this.col + 1);
    }

    String line() {
      return line(0);
    }

    String line(int offset) {
      int row = this.row + offset;
      try {
        checkIndex(row, lines.count());
      } catch (ArrayIndexOutOfBoundsException ex) {
        throw new RuntimeException("row out of bounds. row=" + row + "\n" + source, ex);
      }
      return lines.itemAt(row);
    }

    ImmutableList<String> lines() {
      return lines;
    }

    int codePoint() {
      return codePoint(0, 0);
    }

    int codePoint(int offsetRow, int offsetCol) {
      String line = line(offsetRow);
      int col = this.col + offsetCol;
      try {
        checkIndex(col, line.length());
      } catch (ArrayIndexOutOfBoundsException ex) {
        throw new RuntimeException(
            "cursor out of bounds. row=" +
                this.row +
                offsetRow +
                " col=" +
                col +
                lineSeparator() +
                source,
            ex);
      }
      return line.codePointAt(col);
    }

    Cursor jumpTo(String substring) {
      int index = source.indexOf(substring);
      if (index == -1) {
        throw new RuntimeException(
            "substring not found in source"
                + lineSeparator()
                + "substring="
                + substring
                + lineSeparator()
                + "source="
                + source);
      }
      this.row =
          ListAlgorithms.binarySearch(
                  cumulativeLineRanges,
                  IntRange.just(index),
                  (a, b) -> a.end() <= b.start() ? -1 : a.start() > b.start() ? 1 : 0)
              .orElseThrow();
      this.col = index - cumulativeLineRanges.itemAt(row).start();
      return this;
    }
  }
}