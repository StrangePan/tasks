package tasks.model.impl;

import io.reactivex.Completable;
import io.reactivex.Single;
import omnia.data.structure.DirectedGraph;
import omnia.data.structure.Map;
import omnia.data.structure.immutable.ImmutableDirectedGraph;
import omnia.data.structure.immutable.ImmutableMap;
import omnia.data.structure.tuple.Couple;

public interface TaskStorage {

  /**
   * Reads task data from storage. Returns a Single that will emit the retrieved data when the
   * transfer and parsing completes.
   */
  Single<Couple<ImmutableDirectedGraph<TaskIdImpl>, ImmutableMap<TaskIdImpl, TaskData>>>
      readFromStorage();

  /**
   * Writes the provided task data to storage. Returns a Completable that completes when the
   * serialization and transfer completes.
   */
  Completable writeToStorage(
      DirectedGraph<? extends TaskIdImpl> graph,
      Map<? extends TaskIdImpl, ? extends TaskData> data);
}
