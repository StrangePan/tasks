package tasks.model.impl;

import static java.util.stream.Collectors.joining;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;
import omnia.data.structure.DirectedGraph;
import omnia.data.structure.Map;
import omnia.data.structure.Pair;
import omnia.data.structure.immutable.ImmutableDirectedGraph;
import omnia.data.structure.immutable.ImmutableMap;
import tasks.io.File;

final class TaskFileSource {

  private static final int VERSION = 1;
  private static final String END_OF_LINE = "\n";
  private static final String TASK_FIELD_DELIMITER = ";";
  private static final String TASK_ID_DELIMITER = ",";
  private static final int ID_RADIX = Character.MAX_RADIX;

  private final File file;

  TaskFileSource(File file) {
    this.file = file;
  }

  Single<Pair<DirectedGraph<TaskId>, Map<TaskId, TaskData>>> readFromFile() {
    return Single.using(file::openInputStream, Single::just, InputStream::close)
        .map(TaskFileSource::parseTaskData);
  }

  Completable writeToFile(Pair<DirectedGraph<TaskId>, Map<TaskId, TaskData>> data) {
    return Single.using(file::openOutputStream, Single::just, OutputStream::close)
        .flatMapCompletable(stream -> Completable.fromAction(() -> serializeTaskData(data, stream)));
  }

  private static Pair<DirectedGraph<TaskId>, Map<TaskId, TaskData>> parseTaskData(InputStream stream) {
    return Single.just(stream)
        .map(InputStreamReader::new)
        .map(BufferedReader::new)
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
    private final ImmutableDirectedGraph.Builder<TaskId> graph = ImmutableDirectedGraph.builder();
    private final ImmutableMap.Builder<TaskId, TaskData> tasks = ImmutableMap.builder();
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
      if (line.matches("^# version \\d+$")) {
        return Optional.of(State.PARSING_NOTHING);
      } else if (line.equals("# tasks")) {
        return Optional.of(State.PARSING_TASKS);
      } else if (line.equals("# dependencies")) {
        return Optional.of(State.PARSING_GRAPH);
      }
      return Optional.empty();
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
      TaskId id = parseId(fields[0]);
      Stream.of(fields[1].split(TASK_ID_DELIMITER))
          .map(TaskFileSource::parseId)
          .forEach(dependency -> graph.addEdge(dependency, id));
      // TODO: ensure unique ids
      // TODO: ensure we have data for all ids
    }

    private void parseToTasks(String line) {
      String[] fields = line.split(TASK_FIELD_DELIMITER);
      TaskId id = parseId(fields[0]);
      boolean completed = Boolean.parseBoolean(fields[1]);
      String label = unescapeLabel(fields[2]);
      tasks.put(id, new TaskData(completed, label));
      // TODO: ensure unique ids
    }

    Pair<DirectedGraph<TaskId>, Map<TaskId, TaskData>> build() {
      return Pair.of(graph.build(), tasks.build());
    }
  }

  private static TaskId parseId(String string) {
    try {
      return new TaskId(Long.parseLong(string, ID_RADIX));
    } catch (NumberFormatException ex) {
      // TODO make a parsing exception
      throw new RuntimeException("invalid id: " + string, ex);
    }
  }

  private void serializeTaskData(Pair<DirectedGraph<TaskId>, Map<TaskId, TaskData>> data, OutputStream stream) {
    Observable.just(
            Observable.just("# version " + VERSION),
            Observable.just("# tasks"),
            serialize(data.second()),
            Observable.just("# dependencies"),
            serialize(data.first()))
        .concatMap(o -> o)
        .concatMap(line -> Observable.just(line, END_OF_LINE))
        .map(String::getBytes)
        .blockingForEach(stream::write);
  }

  private static Observable<String> serialize(Map<TaskId, TaskData> tasks) {
    return Single.just(tasks)
        .map(Map::entries)
        .flatMapObservable(Observable::fromIterable)
        .sorted(Comparator.comparing(entry -> entry.key().asLong()))
        .map(entry -> Pair.of(entry.key(), entry.value()))
        .map(TaskFileSource::serialize);
  }

  private static String serialize(Pair<TaskId, TaskData> task) {
    return new StringBuilder()
        .append(serialize(task.first()))
        .append(TASK_FIELD_DELIMITER)
        .append(task.second().isCompleted())
        .append(TASK_FIELD_DELIMITER)
        .append(escapeLabel(task.second().label()))
        .append(TASK_FIELD_DELIMITER)
        .toString();
  }

  private static String serialize(TaskId id) {
    return Long.toString(id.asLong(), ID_RADIX);
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

  private static Observable<String> serialize(DirectedGraph<TaskId> graph) {
    return Single.just(graph)
        .map(DirectedGraph::nodes)
        .flatMapObservable(Observable::fromIterable)
        .filter(node -> node.successors().isPopulated())
        .sorted(Comparator.comparing(node -> node.item().asLong()))
        .map(TaskFileSource::serialize);
  }

  private static String serialize(DirectedGraph.DirectedNode<TaskId> node) {
    return new StringBuilder()
        .append(serialize(node.item()))
        .append(TASK_FIELD_DELIMITER)
        .append(
            node.successors().stream()
                .map(DirectedGraph.DirectedNode::item)
                .sorted(Comparator.comparing(TaskId::asLong))
                .map(TaskFileSource::serialize)
                .collect(joining(TASK_ID_DELIMITER)))
        .append(TASK_FIELD_DELIMITER)
        .toString();
  }
}
