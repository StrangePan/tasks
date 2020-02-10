package tasks.model.impl;

import static java.util.stream.Collectors.joining;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Comparator;
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
    // TODO
    return Pair.of(ImmutableDirectedGraph.empty(), ImmutableMap.empty());
  }

  private void serializeTaskData(Pair<DirectedGraph<TaskId>, Map<TaskId, TaskData>> data, OutputStream stream) {
    Observable.just(
            Observable.just("# version ", Integer.toString(VERSION), END_OF_LINE),
            Observable.just("# tasks", END_OF_LINE),
            serialize(data.second()),
            Observable.just("# dependencies", END_OF_LINE),
            serialize(data.first()))
        .concatMap(o -> o)
        .map(String::getBytes)
        .blockingForEach(stream::write);
  }

  private static Observable<String> serialize(Map<TaskId, TaskData> tasks) {
    return Single.just(tasks)
        .map(Map::entries)
        .flatMapObservable(Observable::fromIterable)
        .sorted(Comparator.comparing(entry -> entry.key().asLong()))
        .map(entry -> Pair.of(entry.key(), entry.value()))
        .map(TaskFileSource::serialize)
        .concatMap(line -> Observable.just(line, END_OF_LINE));
  }

  private static String serialize(Pair<TaskId, TaskData> task) {
    return new StringBuilder()
        .append(serialize(task.first()))
        .append(TASK_FIELD_DELIMITER)
        .append(task.second().isCompleted())
        .append(TASK_FIELD_DELIMITER)
        .append(escapeField(task.second().label()))
        .toString();
  }

  private static String serialize(TaskId id) {
    return Long.toString(id.asLong(), ID_RADIX);
  }

  private static String escapeField(String field) {
    StringBuilder builder = new StringBuilder();
    int i = 0;
    for (int j = 0; j <= field.length(); j++) {
      boolean isEscapableCharacter = j < field.length() && isEscapableCharacter(field.charAt(j));
      if (j == field.length() || isEscapableCharacter) {
        builder.append(field, i, j);
      }
      if (j < field.length() && isEscapableCharacter) {
        builder.append("\\").append(escapeCharacter(field.charAt(j)));
      }
    }
    return builder.toString();
  }

  private static boolean isEscapableCharacter(char c) {
    switch (c) {
      case '\\':
      case '\n':
      case ';':
        return true;
    }
    return false;
  }

  private static char escapeCharacter(char c) {
    switch (c) {
      case '\\':
        return '\\';
      case '\n':
        return 'n';
      case ';':
        return ':';
    }
    return c;
  }

  private static Observable<String> serialize(DirectedGraph<TaskId> graph) {
    return Single.just(graph)
        .map(DirectedGraph::nodes)
        .flatMapObservable(Observable::fromIterable)
        .filter(node -> node.successors().isPopulated())
        .sorted(Comparator.comparing(node -> node.item().asLong()))
        .map(TaskFileSource::serialize)
        .concatMap(line -> Observable.just(line, END_OF_LINE));
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
                .collect(joining()))
        .toString();
  }
}
