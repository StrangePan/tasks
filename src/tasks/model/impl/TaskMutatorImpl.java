package tasks.model.impl;

import static java.util.Objects.requireNonNull;

import io.reactivex.Observable;
import java.util.Optional;
import omnia.data.structure.Set;
import omnia.data.structure.immutable.ImmutableList;
import omnia.data.structure.immutable.ImmutableSet;
import omnia.data.structure.mutable.HashSet;
import omnia.data.structure.mutable.MutableSet;
import tasks.model.Task;
import tasks.model.TaskMutator;

public final class TaskMutatorImpl implements TaskMutator {

  private final ObservableTaskStoreImpl taskStore;
  private final TaskIdImpl taskId;

  private Optional<String> label = Optional.empty();
  private Optional<Task.Status> status = Optional.empty();
  private boolean overwriteBlockingTasks = false;
  private final MutableSet<TaskIdImpl> blockingTasksToAdd = HashSet.create();
  private final MutableSet<TaskIdImpl> blockingTasksToRemove = HashSet.create();
  private boolean overwriteBlockedTasks = false;
  private final MutableSet<TaskIdImpl> blockedTasksToAdd = HashSet.create();
  private final MutableSet<TaskIdImpl> blockedTasksToRemove = HashSet.create();

  TaskMutatorImpl(ObservableTaskStoreImpl taskStore, TaskIdImpl taskId) {
    this.taskStore = requireNonNull(taskStore);
    this.taskId = requireNonNull(taskId);
  }

  @Override
  public TaskMutatorImpl setLabel(String label) {
    this.label = Optional.of(label);
    return this;
  }

  @Override
  public TaskMutator setStatus(Task.Status status) {
    this.status = Optional.of(status);
    return this;
  }

  @Override
  public TaskMutatorImpl setBlockingTasks(Iterable<? extends Task> tasks) {
    Iterable<TaskIdImpl> taskIds =
        ImmutableList.copyOf(
            Observable.fromIterable(tasks)
                .map(store()::validateTask)
                .map(TaskImpl::id)
                .blockingIterable());
    overwriteBlockingTasks = true;
    blockingTasksToAdd.clear();
    blockingTasksToRemove.clear();
    taskIds.forEach(blockingTasksToAdd::add);
    return this;
  }

  @Override
  public TaskMutatorImpl addBlockingTask(Task task) {
    TaskImpl taskImpl = store().validateTask(task);
    blockingTasksToAdd.add(taskImpl.id());
    blockingTasksToRemove.remove(taskImpl.id());
    return this;
  }

  @Override
  public TaskMutatorImpl removeBlockingTask(Task task) {
    TaskImpl taskImpl = store().validateTask(task);
    if (!overwriteBlockingTasks) {
      blockingTasksToRemove.add(taskImpl.id());
    }
    blockingTasksToAdd.remove(taskImpl.id());
    return this;
  }

  @Override
  public TaskMutatorImpl setBlockedTasks(Iterable<? extends Task> tasks) {
    Iterable<TaskIdImpl> taskIds =
        ImmutableList.copyOf(
            Observable.fromIterable(tasks)
                .map(store()::validateTask)
                .map(TaskImpl::id)
                .blockingIterable());
    overwriteBlockedTasks = true;
    blockedTasksToAdd.clear();
    blockedTasksToRemove.clear();
    taskIds.forEach(blockedTasksToAdd::add);
    return this;
  }

  @Override
  public TaskMutatorImpl addBlockedTask(Task task) {
    TaskImpl taskImpl = store().validateTask(task);
    blockedTasksToAdd.add(taskImpl.id());
    blockedTasksToRemove.remove(taskImpl.id());
    return this;
  }

  @Override
  public TaskMutatorImpl removeBlockedTask(Task task) {
    TaskImpl taskImpl = store().validateTask(task);
    if (!overwriteBlockedTasks) {
      blockedTasksToRemove.add(taskImpl.id());
    }
    blockedTasksToAdd.remove(taskImpl.id());
    return this;
  }

  ObservableTaskStoreImpl store() {
    return taskStore;
  }

  TaskIdImpl id() {
    return taskId;
  }

  Optional<String> label() {
    return label;
  }

  Optional<Boolean> completed() {
    return status().map(Task.Status::isCompleted);
  }

  Optional<Task.Status> status() {
    return status;
  }

  boolean overwriteBlockingTasks() {
    return overwriteBlockingTasks;
  }

  Set<TaskIdImpl> blockingTasksToAdd() {
    return ImmutableSet.copyOf(blockingTasksToAdd);
  }

  Set<TaskIdImpl> blockingTasksToRemove() {
    return ImmutableSet.copyOf(blockingTasksToRemove);
  }

  boolean overwriteBlockedTasks() {
    return overwriteBlockedTasks;
  }

  Set<TaskIdImpl> blockedTasksToAdd() {
    return ImmutableSet.copyOf(blockedTasksToAdd);
  }

  Set<TaskIdImpl> blockedTasksToRemove() {
    return ImmutableSet.copyOf(blockedTasksToRemove);
  }
}
