package tasks.model;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import java.util.function.Function;
import omnia.data.structure.Set;

/**
 * A queryable, observable collection of task objects. Contains the canonical data, or knows how to
 * fetch the canonical data of an individual Task.
 */
public interface TaskStore {

  Flowable<Set<Task>> allTasks();

  Flowable<Set<Task>> tasksBlocking(Task blockedTask);

  Flowable<Set<Task>> tasksBlockedBy(Task blockingTask);

  Completable createTask(String label, Function<? super TaskBuilder, ? extends TaskBuilder> builder);

  Completable mutateTask(Task task, Function<? super TaskMutator, ? extends TaskMutator> mutation);

  Completable writeToDisk();
}
