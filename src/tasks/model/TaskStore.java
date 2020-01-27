package tasks.model;

import omnia.data.structure.observable.ObservableSet;

/**
 * A queryable, observable collection of task objects. Contains the canonical data, or knows how to
 * fetch the canonical data of an individual Task.
 */
public interface TaskStore {

  ObservableSet<Task> allTasks();

  ObservableSet<Task> tasksBlocking(Task blockedTask);

  ObservableSet<Task> tasksBlockedBy(Task blockingTask);
}
