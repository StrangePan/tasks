package tasks.model;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import omnia.data.structure.Set;

/**
 * A queryable, observable collection of task objects. Contains the canonical data, or knows how to
 * fetch the canonical data of an individual Task.
 */
public interface TaskStore {

  Flowable<Set<Task>> allTasks();

  Flowable<Set<Task>> tasksBlocking(Task blockedTask);

  Flowable<Set<Task>> tasksBlockedBy(Task blockingTask);

  Completable writeToDisk();
}
