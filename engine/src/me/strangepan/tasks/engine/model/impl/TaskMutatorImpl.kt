package me.strangepan.tasks.engine.model.impl

import io.reactivex.rxjava3.core.Observable
import java.util.Optional
import java.util.function.Consumer
import java.util.function.Function
import omnia.data.structure.Set
import omnia.data.structure.immutable.ImmutableList
import omnia.data.structure.immutable.ImmutableSet
import omnia.data.structure.mutable.HashSet
import omnia.data.structure.mutable.MutableSet
import me.strangepan.tasks.engine.model.Task
import me.strangepan.tasks.engine.model.TaskMutator

class TaskMutatorImpl internal constructor(private val taskStore: ObservableTaskStoreImpl, private val taskId: TaskIdImpl) : TaskMutator {
  private var label: Optional<String> = Optional.empty()
  private var statusMutator: Optional<Function<TaskData, Task.Status>> = Optional.empty()
  private var overwriteBlockingTasks = false
  private val blockingTasksToAdd: MutableSet<TaskIdImpl> = HashSet.create()
  private val blockingTasksToRemove: MutableSet<TaskIdImpl> = HashSet.create()
  private var overwriteBlockedTasks = false
  private val blockedTasksToAdd: MutableSet<TaskIdImpl> = HashSet.create()
  private val blockedTasksToRemove: MutableSet<TaskIdImpl> = HashSet.create()

  override fun setLabel(label: String): TaskMutatorImpl {
    this.label = Optional.of(label)
    return this
  }

  override fun setStatus(status: Task.Status): TaskMutator {
    statusMutator = Optional.of(Function { status })
    return this
  }

  override fun complete(): TaskMutator {
    statusMutator = Optional.of(Function { Task.Status.COMPLETED })
    return this
  }

  override fun reopen(): TaskMutator {
    statusMutator = Optional.of(Function { if (it.status().isCompleted) Task.Status.OPEN else it.status() })
    return this
  }

  override fun start(): TaskMutator {
    statusMutator = Optional.of(Function { Task.Status.STARTED })
    return this
  }

  override fun stop(): TaskMutator {
    statusMutator = Optional.of(Function { if (it.status().isStarted) Task.Status.OPEN else it.status() })
    return this
  }

  override fun setBlockingTasks(tasks: Iterable<Task>): TaskMutatorImpl {
    val taskIds: Iterable<TaskIdImpl> = ImmutableList.copyOf(
        Observable.fromIterable(tasks)
            .map { store().validateTask(it) }
            .map { it.id() }
            .blockingIterable())
    overwriteBlockingTasks = true
    blockingTasksToAdd.clear()
    blockingTasksToRemove.clear()
    taskIds.forEach(Consumer { blockingTasksToAdd.add(it) })
    return this
  }

  override fun addBlockingTask(task: Task): TaskMutatorImpl {
    val taskImpl = store().validateTask(task)
    blockingTasksToAdd.add(taskImpl.id())
    blockingTasksToRemove.remove(taskImpl.id())
    return this
  }

  override fun removeBlockingTask(task: Task): TaskMutatorImpl {
    val taskImpl = store().validateTask(task)
    if (!overwriteBlockingTasks) {
      blockingTasksToRemove.add(taskImpl.id())
    }
    blockingTasksToAdd.remove(taskImpl.id())
    return this
  }

  override fun setBlockedTasks(tasks: Iterable<Task>): TaskMutatorImpl {
    val taskIds: Iterable<TaskIdImpl> = ImmutableList.copyOf(
        Observable.fromIterable(tasks)
            .map { store().validateTask(it) }
            .map { it.id() }
            .blockingIterable())
    overwriteBlockedTasks = true
    blockedTasksToAdd.clear()
    blockedTasksToRemove.clear()
    taskIds.forEach(Consumer { blockedTasksToAdd.add(it) })
    return this
  }

  override fun addBlockedTask(task: Task): TaskMutatorImpl {
    val taskImpl = store().validateTask(task)
    blockedTasksToAdd.add(taskImpl.id())
    blockedTasksToRemove.remove(taskImpl.id())
    return this
  }

  override fun removeBlockedTask(task: Task): TaskMutatorImpl {
    val taskImpl = store().validateTask(task)
    if (!overwriteBlockedTasks) {
      blockedTasksToRemove.add(taskImpl.id())
    }
    blockedTasksToAdd.remove(taskImpl.id())
    return this
  }

  fun store(): ObservableTaskStoreImpl {
    return taskStore
  }

  fun id(): TaskIdImpl {
    return taskId
  }

  fun label(): Optional<String> {
    return label
  }

  fun statusMutator(): Optional<Function<TaskData, Task.Status>> {
    return statusMutator
  }

  fun overwriteBlockingTasks(): Boolean {
    return overwriteBlockingTasks
  }

  fun blockingTasksToAdd(): Set<TaskIdImpl> {
    return ImmutableSet.copyOf(blockingTasksToAdd)
  }

  fun blockingTasksToRemove(): Set<TaskIdImpl> {
    return ImmutableSet.copyOf(blockingTasksToRemove)
  }

  fun overwriteBlockedTasks(): Boolean {
    return overwriteBlockedTasks
  }

  fun blockedTasksToAdd(): Set<TaskIdImpl> {
    return ImmutableSet.copyOf(blockedTasksToAdd)
  }

  fun blockedTasksToRemove(): Set<TaskIdImpl> {
    return ImmutableSet.copyOf(blockedTasksToRemove)
  }

}