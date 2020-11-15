package tasks.model.impl;

import static java.util.stream.Collectors.joining;
import static omnia.data.cache.Memoized.memoize;
import static omnia.data.stream.Collectors.toImmutableMap;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Writer;
import java.util.Comparator;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import omnia.data.cache.Memoized;
import omnia.data.structure.DirectedGraph;
import omnia.data.structure.Map;
import omnia.data.structure.immutable.ImmutableDirectedGraph;
import omnia.data.structure.immutable.ImmutableList;
import omnia.data.structure.immutable.ImmutableMap;
import omnia.data.structure.immutable.ImmutableSet;
import omnia.data.structure.mutable.HashMap;
import omnia.data.structure.mutable.HashSet;
import omnia.data.structure.mutable.MutableMap;
import omnia.data.structure.mutable.MutableSet;
import omnia.data.structure.tuple.Couple;
import omnia.data.structure.tuple.Tuple;
import tasks.io.File;
import tasks.model.Task;

final class TaskFileSource {

  private static final int VERSION = 2;
  private static final String END_OF_LINE = "\n";
  private static final String TASK_FIELD_DELIMITER = ";";
  private static final String TASK_ID_DELIMITER = ",";
  private static final Pattern VERSION_PATTERN = Pattern.compile("^# version (\\d+)$");

  private static final Memoized<ImmutableSet<Couple<Task.Status, ImmutableList<String>>>> STATUS_STRINGS =
      memoize(() ->
          ImmutableSet.of(
              // false = !isCompleted, legacy
              Tuple.of(Task.Status.OPEN, ImmutableList.of("open", "false")),
              // true = isCompleted, legacy
              Tuple.of(Task.Status.COMPLETED, ImmutableList.of("complete", "true")),
              Tuple.of(Task.Status.STARTED, ImmutableList.of("started"))));
  private static final Memoized<ImmutableMap<Task.Status, String>> STATUS_TO_STRING =
      memoize(() ->
          STATUS_STRINGS.value().stream()
              .map(couple -> couple.mapSecond(list -> list.itemAt(0)))
              .collect(toImmutableMap()));
  private static final Memoized<ImmutableMap<String, Task.Status>> STRING_TO_STATUS =
      memoize(() ->
          STATUS_STRINGS.value().stream()
              .flatMap(couple -> couple.second().stream().map(s -> Tuple.of(s, couple.first())))
              .collect(toImmutableMap()));

  private final File file;

  TaskFileSource(File file) {
    this.file = file;
  }

  Single<Couple<ImmutableDirectedGraph<TaskIdImpl>, ImmutableMap<TaskIdImpl, TaskData>>> readFromFile() {
    return Single.fromCallable(() -> {
      try (BufferedReader reader = new BufferedReader(file.openReader())) {
        return parseTaskData(reader);
      }
    });
  }

  Completable writeToFile(
      DirectedGraph<? extends TaskIdImpl> graph, Map<? extends TaskIdImpl, ? extends TaskData> data) {
    return Completable.fromAction(() -> {
      try (BufferedWriter writer = new BufferedWriter(file.openWriter())) {
        serializeTaskData(graph, data, writer);
      }
    });
  }

  private static Couple<ImmutableDirectedGraph<TaskIdImpl>, ImmutableMap<TaskIdImpl, TaskData>>
      parseTaskData(BufferedReader reader) {
    return Single.just(reader)
        .map(BufferedReader::lines)
        .map(Stream::iterator)
        .map(iterator -> (Iterable<String>) () -> iterator)
        .flatMapObservable(Observable::fromIterable)
        .filter(line -> !line.isBlank())
        .collect(LineCollector::new, LineCollector::collect)
        .map(LineCollector::build)
        .blockingGet();
  }

  private static final class LineCollector {
    private final ImmutableDirectedGraph.Builder<TaskIdImpl> graph = ImmutableDirectedGraph.builder();
    private final MutableSet<TaskIdImpl> tasksWithParsedEdges = HashSet.create();
    private final MutableMap<TaskIdImpl, TaskData> tasks = HashMap.create();
    private State state = State.PARSING_NOTHING;

    private enum State {
      PARSING_NOTHING,
      PARSING_GRAPH,
      PARSING_TASKS,
    }

    void collect(String line) {
      maybeParseStateChange(line).ifPresentOrElse(
          newState -> this.state = newState,
          () -> parseToCollection(line));
    }

    private Optional<State> maybeParseStateChange(String line) {
      Matcher versionMatcher = VERSION_PATTERN.matcher(line);
      if (versionMatcher.matches()) {
        assertSupportedVersion(versionMatcher.group(1));
        return Optional.of(State.PARSING_NOTHING);
      } else if (line.equals("# tasks")) {
        return Optional.of(State.PARSING_TASKS);
      } else if (line.equals("# dependencies")) {
        return Optional.of(State.PARSING_GRAPH);
      }
      return Optional.empty();
    }

    private static void assertSupportedVersion(String versionString) {
      try {
        int version = Integer.parseInt(versionString);
        if (version > VERSION) {
          throw new RuntimeException("unsupported file version: " + version + ". supported versions: " + VERSION);
        }
      } catch (NumberFormatException ex) {
        throw new RuntimeException("unable to parse file version code: " + versionString, ex);
      }
    }

    private void parseToCollection(String line) {
      switch (state) {
        case PARSING_GRAPH:
          parseToGraph(line);
          break;
        case PARSING_TASKS:
          parseToTasks(line);
          break;
        default:
          throw new IllegalStateException("unexpected line in file: " + line);
      }
    }

    private void parseToGraph(String line) {
      String[] fields = line.split(TASK_FIELD_DELIMITER);
      TaskIdImpl id = parseId(fields[0]);

      if (tasksWithParsedEdges.contains(id)) {
        // TODO(b4ahbuoudukg): Add custom parsing exceptions to FileTaskStore
        throw new RuntimeException("edges for task defined twice: " + id);
      }

      try {
        Stream.of(fields[1].split(TASK_ID_DELIMITER))
            .map(TaskFileSource::parseId)
            .forEach(dependency -> graph.addEdge(dependency, id));
      } catch (IllegalStateException e) {
        // TODO(b4ahbuoudukg): Add custom parsing exceptions to FileTaskStore
        // TODO(yisiy12nlclc): Add custom exceptions for graph builder illegal states
        throw new RuntimeException("missing task data for: " + id, e);
      }

      tasksWithParsedEdges.add(id);
    }

    private void parseToTasks(String line) {
      String[] fields = line.split(TASK_FIELD_DELIMITER);
      TaskIdImpl id = parseId(fields[0]);
      Task.Status status = parseStatus(fields[1]);
      String label = unescapeLabel(fields[2]);

      if (tasks.keys().contains(id)) {
        // TODO(b4ahbuoudukg): Add custom parsing exceptions to FileTaskStore
        throw new RuntimeException("task ID defined twice: " + id);
      }

      tasks.putMapping(id, new TaskData(label, status));
      graph.addNode(id);
    }

    private static Task.Status parseStatus(String field) {
      return STRING_TO_STATUS.value().valueOf(field.toLowerCase()).orElse(Task.Status.OPEN);
    }

    Couple<ImmutableDirectedGraph<TaskIdImpl>, ImmutableMap<TaskIdImpl, TaskData>> build() {
      return Tuple.of(graph.build(), ImmutableMap.copyOf(tasks));
    }
  }

  private static TaskIdImpl parseId(String string) {
    try {
      return TaskIdImpl.parse(string);
    } catch (NumberFormatException ex) {
      // TODO(b4ahbuoudukg): Add custom parsing exceptions to FileTaskStore
      throw new RuntimeException("invalid id: " + string, ex);
    }
  }

  private void serializeTaskData(
      DirectedGraph<? extends TaskIdImpl> graph,
      Map<? extends TaskIdImpl, ? extends TaskData> data,
      Writer writer) {
    Observable.just(
            Observable.just("# version " + VERSION),
            Observable.just("# tasks"),
            serialize(data),
            Observable.just("# dependencies"),
            serialize(graph))
        .concatMap(o -> o)
        .concatMap(line -> Observable.just(line, END_OF_LINE))
        .blockingForEach(writer::write);
  }

  private static Observable<String> serialize(Map<? extends TaskIdImpl, ? extends TaskData> tasks) {
    return Single.just(tasks)
        .map(Map::entries)
        .flatMapObservable(Observable::fromIterable)
        .sorted(Comparator.comparing(entry -> entry.key().asLong()))
        .map(entry -> Tuple.of(entry.key(), entry.value()))
        .map(TaskFileSource::serialize);
  }

  private static String serialize(Couple<? extends TaskIdImpl, ? extends TaskData> task) {
    return new StringBuilder()
        .append(serialize(task.first()))
        .append(TASK_FIELD_DELIMITER)
        .append(STATUS_TO_STRING.value().valueOf(task.second().status()).orElseThrow())
        .append(TASK_FIELD_DELIMITER)
        .append(escapeLabel(task.second().label()))
        .append(TASK_FIELD_DELIMITER)
        .toString();
  }

  private static String serialize(TaskIdImpl id) {
    return id.toString();
  }

  private static String escapeLabel(String label) {
    StringBuilder escapedLabel = new StringBuilder();
    int i = 0;
    for (int j = 0; j <= label.length(); j++) {
      boolean isEscapableCharacter = j < label.length() && isEscapableCharacter(label.charAt(j));
      if (j == label.length() || isEscapableCharacter) {
        escapedLabel.append(label, i, j);
        i = j + 1;
      }
      if (j < label.length() && isEscapableCharacter) {
        escapedLabel.append("\\").append(escapeCharacter(label.charAt(j)));
      }
    }
    return escapedLabel.toString();
  }

  private static boolean isEscapableCharacter(char c) {
    switch (c) {
      case '\\':
      case '\n':
      case ';':
        return true;
      default:
        return false;
    }
  }

  private static char escapeCharacter(char c) {
    switch (c) {
      case '\\':
        return '\\';
      case '\n':
        return 'n';
      case ';':
        return ':';
      default:
        return c;
    }
  }

  private static String unescapeLabel(String escapedLabel) {
    StringBuilder label = new StringBuilder();
    int i = 0;
    for (int j = 0; j <= escapedLabel.length(); j++) {
      boolean isEscapeCharacter = j < escapedLabel.length() && escapedLabel.charAt(j) == '\\';
      if (j == escapedLabel.length() || isEscapeCharacter) {
        label.append(escapedLabel, i, j);
        i = j + 2;
      }
      if (j < escapedLabel.length() && isEscapeCharacter) {
        j++;
        label.append(unescapeCharacter(escapedLabel.charAt(j)));
      }
    }
    return label.toString();
  }

  private static char unescapeCharacter(char c) {
    switch (c) {
      case '\\':
        return '\\';
      case 'n':
        return '\n';
      case ':':
        return ';';
      default:
        return c;
    }
  }

  private static Observable<String> serialize(DirectedGraph<? extends TaskIdImpl> graph) {
    return Single.just(graph)
        .map(DirectedGraph::nodes)
        .flatMapObservable(Observable::fromIterable)
        .filter(node -> node.predecessors().isPopulated())
        .sorted(Comparator.comparing(node -> node.item().asLong()))
        .map(TaskFileSource::serialize);
  }

  private static String serialize(DirectedGraph.DirectedNode<? extends TaskIdImpl> node) {
    return new StringBuilder()
        .append(serialize(node.item()))
        .append(TASK_FIELD_DELIMITER)
        .append(
            node.predecessors().stream()
                .map(DirectedGraph.DirectedNode::item)
                .sorted(Comparator.comparing(TaskIdImpl::asLong))
                .map(TaskFileSource::serialize)
                .collect(joining(TASK_ID_DELIMITER)))
        .append(TASK_FIELD_DELIMITER)
        .toString();
  }
}
