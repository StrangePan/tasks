package tasks.model.impl;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import omnia.data.structure.Set;
import omnia.data.structure.immutable.ImmutableSet;
import omnia.data.structure.mutable.HashSet;
import omnia.data.structure.mutable.MutableSet;
import tasks.model.Task;
import tasks.model.TaskMutator;

public final class TaskMutatorImpl implements TaskMutator {

  private final TaskStoreImpl taskStore;
  private final TaskId taskId;

  private Optional<String> label = Optional.empty();
  private Optional<Boolean> completed = Optional.empty();
  private final MutableSet<TaskId> blockingTasksToAdd = HashSet.create();
  private final MutableSet<TaskId> blockingTasksToRemove = HashSet.create();
  private final MutableSet<TaskId> blockedTasksToAdd = HashSet.create();
  private final MutableSet<TaskId> blockedTasksToRemove = HashSet.create();

  TaskMutatorImpl(TaskStoreImpl taskStore, TaskId taskId) {
    this.taskStore = requireNonNull(taskStore);
    this.taskId = requireNonNull(taskId);
  }

  @Override
  public TaskMutatorImpl setCompleted(boolean completed) {
    this.completed = Optional.of(completed);
    return this;
  }

  @Override
  public TaskMutatorImpl setLabel(String label) {
    this.label = Optional.of(label);
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
    blockingTasksToRemove.add(taskImpl.id());
    blockingTasksToAdd.remove(taskImpl.id());
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
    blockedTasksToRemove.add(taskImpl.id());
    blockedTasksToAdd.remove(taskImpl.id());
    return this;
  }

  TaskStoreImpl store() {
    return taskStore;
  }

  TaskId id() {
    return taskId;
  }

  Optional<String> label() {
    return label;
  }

  Optional<Boolean> completed() {
    return completed;
  }

  Set<TaskId> blockingTasksToAdd() {
    return ImmutableSet.copyOf(blockingTasksToAdd);
  }

  Set<TaskId> blockingTasksToRemove() {
    return ImmutableSet.copyOf(blockingTasksToRemove);
  }

  Set<TaskId> blockedTasksToAdd() {
    return ImmutableSet.copyOf(blockedTasksToAdd);
  }

  Set<TaskId> blockedTasksToRemove() {
    return ImmutableSet.copyOf(blockedTasksToRemove);
  }
}
