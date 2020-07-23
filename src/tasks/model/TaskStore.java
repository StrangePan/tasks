package tasks.model;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import java.util.function.Function;
import omnia.data.structure.Set;

/**
 * A queryable, observable collection of task objects. Contains the canonical data, or knows how to
 * fetch the canonical data of an individual Task.
 */
public interface TaskStore {

  Maybe<Task> lookUpById(long id);

  Flowable<Set<Task>> allTasks();

  Flowable<Set<Task>> allTasksBlocking(Task blockedTask);

  Flowable<Set<Task>> allTasksBlockedBy(Task blockingTask);

  Flowable<Set<Task>> allTasksWithoutOpenBlockers();

  Flowable<Set<Task>> allTasksWithOpenBlockers();

  Flowable<Set<Task>> completedTasks();

  Flowable<Set<Task>> allTasksMatchingCliPrefix(String prefix);

  Single<Task> createTask(
      String label, Function<? super TaskBuilder, ? extends TaskBuilder> builder);

  Completable mutateTask(Task task, Function<? super TaskMutator, ? extends TaskMutator> mutation);

  Completable deleteTask(Task task);

  Completable writeToDisk();
}
