package tasks.model;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import java.util.function.Function;
import omnia.data.structure.DirectedGraph;
import omnia.data.structure.Set;

/**
 * A queryable, observable collection of task objects. Contains the canonical data, or knows how to
 * fetch the canonical data of an individual ObservableTask.
 */
public interface ObservableTaskStore {

  Maybe<ObservableTask> lookUpById(long id);

  Flowable<Set<ObservableTask>> allTasks();

  Flowable<Set<ObservableTask>> allTasksBlocking(ObservableTask blockedTask);

  Flowable<Set<ObservableTask>> allTasksBlockedBy(ObservableTask blockingTask);

  Flowable<Set<ObservableTask>> allOpenTasksWithoutOpenBlockers();

  Flowable<Set<ObservableTask>> allOpenTasksWithOpenBlockers();

  Flowable<Set<ObservableTask>> allCompletedTasks();

  Flowable<Set<ObservableTask>> allOpenTasks();

  Flowable<Set<ObservableTask>> allTasksMatchingCliPrefix(String prefix);

  Flowable<DirectedGraph<ObservableTask>> taskGraph();

  /**
   * Attempts to create a new task and add it to the store.
   *
   * @param label the desired label for the new task
   * @param builder a function that configures and returns a {@link TaskBuilder} for the new task
   * @return A {@link Single} which either emits the completed task after it has been successfully
   *     added to the store, or an error if the task mutation would have put the store in an invalid
   *     state.
   */
  Single<ObservableTask> createTask(
      String label, Function<? super TaskBuilder, ? extends TaskBuilder> builder);

  /**
   * Attempts to mutate an existing task in the store.
   *
   * @param task the desired task to mutate
   * @param mutation a function that configures and returns a {@link TaskMutator}
   * @return A {@link Completable} that completes when the mutation has been successfully applied to
   *     the store, or emits an error if the mutation would have put the store in an invalid state.
   */
  Completable mutateTask(ObservableTask task, Function<? super TaskMutator, ? extends TaskMutator> mutation);

  /**
   * Attempts to delete a task from the store.
   *
   * @param task the desired task to delete
   * @return A {@link Completable} that completes when the deletion has been successfully completed,
   *     or emits an error if the mutation would have put the store in an invalid state.
   */
  Completable deleteTask(ObservableTask task);

  /**
   * Attempts to save the task store's contents to persistent storage.
   *
   * @return A {@link Completable} that completes when the contents of the store have been
   *     been successfully committed to persistent storage, or emits an error if the operation
   *     failed for any reason, including an {@link java.io.IOException}.
   */
  Completable writeToDisk();

  /**
   * Attempts to save the task store's contents to persistent storage and cease accepting mutations
   * or emitting results. Completes any outstanding streams, disposing their subscriptions.
   *
   * @return A {@link Completable} that completes when the store has completely shut down, including
   *     committing any contents to persistent storage, or emits an error if the operation
   *     failed for any reason, including an {@link java.io.IOException}.
   */
  Completable shutdown();
}
