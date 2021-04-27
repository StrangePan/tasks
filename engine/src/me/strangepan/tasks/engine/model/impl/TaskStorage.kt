package me.strangepan.tasks.engine.model.impl

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import omnia.data.structure.DirectedGraph
import omnia.data.structure.Map
import omnia.data.structure.immutable.ImmutableDirectedGraph
import omnia.data.structure.immutable.ImmutableMap
import omnia.data.structure.tuple.Couple

interface TaskStorage {
  /**
   * Reads task data from storage. Returns a Single that will emit the retrieved data when the
   * transfer and parsing completes.
   */
  fun readFromStorage(): Single<Couple<ImmutableDirectedGraph<TaskIdImpl>, ImmutableMap<TaskIdImpl, TaskData>>>

  /**
   * Writes the provided task data to storage. Returns a Completable that completes when the
   * serialization and transfer completes.
   */
  fun writeToStorage(
      graph: DirectedGraph<out TaskIdImpl>,
      data: Map<out TaskIdImpl, out TaskData>): Completable
}