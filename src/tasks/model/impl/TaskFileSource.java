package tasks.model.impl;

import io.reactivex.Completable;
import io.reactivex.Single;
import java.io.InputStream;
import java.io.OutputStream;
import omnia.data.structure.DirectedGraph;
import omnia.data.structure.Map;
import omnia.data.structure.Pair;
import omnia.data.structure.immutable.ImmutableDirectedGraph;
import omnia.data.structure.immutable.ImmutableMap;
import tasks.io.File;

final class TaskFileSource {

  private final File file;

  TaskFileSource(File file) {
    this.file = file;
  }

  private Single<Pair<DirectedGraph<TaskId>, Map<TaskId, TaskData>>> readFromFile() {
    return Single.using(file::openInputStream, Single::just, InputStream::close)
        .map(TaskFileSource::parseTaskData);
  }

  private Completable writeToFile(Pair<DirectedGraph<TaskId>, Map<TaskId, TaskData>> data) {
    return Single.using(file::openOutputStream, Single::just, OutputStream::close)
        .flatMapCompletable(stream -> Completable.fromAction(() -> serializeTaskData(data, stream)));
  }

  private static Pair<DirectedGraph<TaskId>, Map<TaskId, TaskData>> parseTaskData(InputStream stream) {
    // todo
    return Pair.of(ImmutableDirectedGraph.empty(), ImmutableMap.empty());
  }

  private void serializeTaskData(Pair<DirectedGraph<TaskId>, Map<TaskId, TaskData>> data, OutputStream stream) {
    // todo
  }
}
