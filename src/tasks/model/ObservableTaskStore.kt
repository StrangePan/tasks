package tasks.model

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.util.function.Function
import omnia.data.structure.tuple.Triple

/**
 * A queryable, observable collection of task objects. Contains the canonical data, or knows how to
 * fetch the canonical data of an individual ObservableTask.
 */
interface ObservableTaskStore {
  fun observe(): Observable<out TaskStore>

  /**
   * Attempts to create a new task and add it to the store.
   *
   * @param label the desired label for the new task
   * @param builder a function that configures and returns a [TaskBuilder] for the new task
   * @return A [Single] that emits the mutated task, as well as the before and after states
   * of the store when the new task has been successfully applied to the store, or emits an
   * error if the mutation would have put the store in an invalid state.
   */
  fun createTask(
      label: String, builder: Function<in TaskBuilder, out TaskBuilder>): Single<out Triple<out TaskStore, out TaskStore, out Task>>

  /**
   * Attempts to mutate an existing task in the store.
   *
   * @param task the desired task to mutate
   * @param mutation a function that configures and returns a [TaskMutator]
   * @return A [Single] that emits the mutated task, as well as the before and after states
   * of the store when the mutation has been successfully applied to the store, or emits an
   * error if the mutation would have put the store in an invalid state.
   */
  fun mutateTask(
      task: Task, mutation: Function<in TaskMutator, out TaskMutator>): Single<out Triple<out TaskStore, out TaskStore, out Task>>

  /**
   * Attempts to delete a task from the store.
   *
   * @param task the desired task to delete
   * @return A [Completable] that completes when the deletion has been successfully completed,
   * or emits an error if the mutation would have put the store in an invalid state.
   */
  fun deleteTask(task: Task): Maybe<out Task>

  /**
   * Attempts to save the task store's contents to persistent storage.
   *
   * @return A [Completable] that completes when the contents of the store have been
   * been successfully committed to persistent storage, or emits an error if the operation
   * failed for any reason, including an [java.io.IOException].
   */
  fun writeToDisk(): Completable

  /**
   * Attempts to save the task store's contents to persistent storage and cease accepting mutations
   * or emitting results. Completes any outstanding streams, disposing their subscriptions.
   *
   * @return A [Completable] that completes when the store has completely shut down, including
   * committing any contents to persistent storage, or emits an error if the operation
   * failed for any reason, including an [java.io.IOException].
   */
  fun shutdown(): Completable
}