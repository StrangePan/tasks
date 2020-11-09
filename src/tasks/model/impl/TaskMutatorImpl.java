package tasks.model.impl;

import static java.util.Objects.requireNonNull;

import io.reactivex.Observable;
import java.util.Optional;
import omnia.data.structure.Set;
import omnia.data.structure.immutable.ImmutableList;
import omnia.data.structure.immutable.ImmutableSet;
import omnia.data.structure.mutable.HashSet;
import omnia.data.structure.mutable.MutableSet;
import tasks.model.ObservableTask;
import tasks.model.TaskMutator;

public final class TaskMutatorImpl implements TaskMutator {

  private final ObservableTaskStoreImpl taskStore;
  private final TaskId taskId;

  private Optional<String> label = Optional.empty();
  private Optional<Boolean> completed = Optional.empty();
  private boolean overwriteBlockingTasks = false;
  private final MutableSet<TaskId> blockingTasksToAdd = HashSet.create();
  private final MutableSet<TaskId> blockingTasksToRemove = HashSet.create();
  private boolean overwriteBlockedTasks = false;
  private final MutableSet<TaskId> blockedTasksToAdd = HashSet.create();
  private final MutableSet<TaskId> blockedTasksToRemove = HashSet.create();

  TaskMutatorImpl(ObservableTaskStoreImpl taskStore, TaskId taskId) {
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
  public TaskMutatorImpl setBlockingTasks(Iterable<ObservableTask> tasks) {
    Iterable<TaskId> taskIds =
        ImmutableList.copyOf(
            Observable.fromIterable(tasks)
                .map(store()::validateTask)
                .map(ObservableTaskImpl::id)
                .blockingIterable());
    overwriteBlockingTasks = true;
    blockingTasksToAdd.clear();
    blockingTasksToRemove.clear();
    taskIds.forEach(blockingTasksToAdd::add);
    return this;
  }

  @Override
  public TaskMutatorImpl addBlockingTask(ObservableTask task) {
    ObservableTaskImpl taskImpl = store().validateTask(task);
    blockingTasksToAdd.add(taskImpl.id());
    blockingTasksToRemove.remove(taskImpl.id());
    return this;
  }

  @Override
  public TaskMutatorImpl removeBlockingTask(ObservableTask task) {
    ObservableTaskImpl taskImpl = store().validateTask(task);
    if (!overwriteBlockingTasks) {
      blockingTasksToRemove.add(taskImpl.id());
    }
    blockingTasksToAdd.remove(taskImpl.id());
    return this;
  }

  @Override
  public TaskMutatorImpl setBlockedTasks(Iterable<ObservableTask> tasks) {
    Iterable<TaskId> taskIds =
        ImmutableList.copyOf(
            Observable.fromIterable(tasks)
                .map(store()::validateTask)
                .map(ObservableTaskImpl::id)
                .blockingIterable());
    overwriteBlockedTasks = true;
    blockedTasksToAdd.clear();
    blockedTasksToRemove.clear();
    taskIds.forEach(blockedTasksToAdd::add);
    return this;
  }

  @Override
  public TaskMutatorImpl addBlockedTask(ObservableTask task) {
    ObservableTaskImpl taskImpl = store().validateTask(task);
    blockedTasksToAdd.add(taskImpl.id());
    blockedTasksToRemove.remove(taskImpl.id());
    return this;
  }

  @Override
  public TaskMutatorImpl removeBlockedTask(ObservableTask task) {
    ObservableTaskImpl taskImpl = store().validateTask(task);
    if (!overwriteBlockedTasks) {
      blockedTasksToRemove.add(taskImpl.id());
    }
    blockedTasksToAdd.remove(taskImpl.id());
    return this;
  }

  ObservableTaskStoreImpl store() {
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

  boolean overwriteBlockingTasks() {
    return overwriteBlockingTasks;
  }

  Set<TaskId> blockingTasksToAdd() {
    return ImmutableSet.copyOf(blockingTasksToAdd);
  }

  Set<TaskId> blockingTasksToRemove() {
    return ImmutableSet.copyOf(blockingTasksToRemove);
  }

  boolean overwriteBlockedTasks() {
    return overwriteBlockedTasks;
  }

  Set<TaskId> blockedTasksToAdd() {
    return ImmutableSet.copyOf(blockedTasksToAdd);
  }

  Set<TaskId> blockedTasksToRemove() {
    return ImmutableSet.copyOf(blockedTasksToRemove);
  }
}
