package tasks.model.impl;

import static java.util.Objects.requireNonNull;

import io.reactivex.Observable;
import omnia.data.structure.Set;
import omnia.data.structure.immutable.ImmutableSet;
import omnia.data.structure.mutable.HashSet;
import omnia.data.structure.mutable.MutableSet;
import tasks.model.Task;
import tasks.model.TaskBuilder;

final class TaskBuilderImpl implements TaskBuilder {

  private final TaskStoreImpl taskStore;
  private final String label;

  private boolean completed = false;
  private final MutableSet<TaskId> blockingTasksToAdd = HashSet.create();
  private final MutableSet<TaskId> blockedTasksToAdd = HashSet.create();

  TaskBuilderImpl(TaskStoreImpl taskStore, String label) {
    this.taskStore = requireNonNull(taskStore);
    this.label = requireNonNull(label);
  }

  @Override
  public TaskBuilderImpl setCompleted(boolean completed) {
    this.completed = completed;
    return this;
  }

  @Override
  public TaskBuilderImpl setBlockingTasks(Iterable<Task> tasks) {
    Iterable<TaskId> taskIds =
        Observable.fromIterable(tasks)
            .map(store()::validateTask)
            .map(TaskImpl::id)
            .blockingIterable();
    blockingTasksToAdd.clear();
    taskIds.forEach(blockingTasksToAdd::add);
    return this;
  }

  @Override
  public TaskBuilderImpl addBlockingTask(Task task) {
    TaskImpl taskImpl = store().validateTask(task);
    blockingTasksToAdd.add(taskImpl.id());
    return this;
  }

  @Override
  public TaskBuilderImpl setBlockedTasks(Iterable<Task> tasks) {
    Iterable<TaskId> taskIds =
        Observable.fromIterable(tasks)
            .map(store()::validateTask)
            .map(TaskImpl::id)
            .blockingIterable();
    blockedTasksToAdd.clear();
    taskIds.forEach(blockedTasksToAdd::add);
    return this;
  }

  @Override
  public TaskBuilderImpl addBlockedTask(Task task) {
    TaskImpl taskImpl = store().validateTask(task);
    blockedTasksToAdd.add(taskImpl.id());
    return this;
  }

  TaskStoreImpl store() {
    return taskStore;
  }

  boolean completed() {
    return completed;
  }

  String label() {
    return label;
  }

  Set<TaskId> blockingTasks() {
    return ImmutableSet.copyOf(blockingTasksToAdd);
  }

  Set<TaskId> blockedTasks() {
    return ImmutableSet.copyOf(blockedTasksToAdd);
  }
}
