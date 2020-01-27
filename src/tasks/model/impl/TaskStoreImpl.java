package tasks.model.impl;

import static java.util.Objects.requireNonNull;

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import omnia.data.structure.observable.ObservableSet;
import omnia.data.structure.observable.writable.WritableObservableDirectedGraph;
import tasks.model.Task;
import tasks.model.TaskStore;

final class TaskStoreImpl implements TaskStore {

  WritableObservableDirectedGraph<TaskId> taskGraph = WritableObservableDirectedGraph.create();

  Maybe<Flowable<TaskData>> lookUp(TaskId id) {
    return Maybe.empty();
    // TODO
  }

  @Override
  public ObservableSet<Task> allTasks() {
    return taskGraph.contents().transform().<Task>map(this::toTaskImpl).toSet();
  }

  @Override
  public ObservableSet<Task> tasksBlocking(Task blockedTask) {
    return toObservableTaskSet(tasksBlocking(validateTask(blockedTask).id()));
  }

  ObservableSet<TaskImpl> tasksBlocking(TaskId taskId) {
    // TODO
    throw new UnsupportedOperationException();
  }

  @Override
  public ObservableSet<Task> tasksBlockedBy(Task blockingTask) {
    return toObservableTaskSet(tasksBlockedBy(validateTask(blockingTask).id()));
  }

  ObservableSet<TaskImpl> tasksBlockedBy(TaskId taskId) {
    // TODO
    throw new UnsupportedOperationException();
  }

  private TaskImpl validateTask(Task task) {
    requireNonNull(task);
    if (!(task instanceof TaskImpl)) {
      throw new IllegalArgumentException(
          "Unrecognized task type. Expected "
              + TaskImpl.class
              + ", received "
              + task.getClass()
              + ": "
              + task);
    }
    TaskImpl taskImpl = (TaskImpl) task;
    if (taskImpl.store() != this) {
      throw new IllegalArgumentException(
          "Task associated with another store. Expected <"
              + this
              + ">, received <"
              + taskImpl.store()
              + ">: "
              + taskImpl);
    }
    return taskImpl;
  }

  private TaskImpl toTaskImpl(TaskId id) {
    return new TaskImpl(this, id);
  }

  private static ObservableSet<Task> toObservableTaskSet(ObservableSet<TaskImpl> taskImplSet) {
    return taskImplSet.transform().map(taskImpl -> (Task) taskImpl).toSet();
  }
}
