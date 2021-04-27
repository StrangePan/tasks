package me.strangepan.tasks.engine.model.impl

import io.reactivex.rxjava3.core.Observable
import java.util.Objects
import java.util.function.Consumer
import omnia.data.structure.Set
import omnia.data.structure.immutable.ImmutableSet
import omnia.data.structure.mutable.HashSet
import omnia.data.structure.mutable.MutableSet
import me.strangepan.tasks.engine.model.Task
import me.strangepan.tasks.engine.model.TaskBuilder

class TaskBuilderImpl(private val taskStore: ObservableTaskStoreImpl, private val label: String) : TaskBuilder {
  private var status = Task.Status.OPEN
  private val blockingTasksToAdd: MutableSet<TaskIdImpl> = HashSet.create()
  private val blockedTasksToAdd: MutableSet<TaskIdImpl> = HashSet.create()

  override fun setStatus(status: Task.Status): TaskBuilder {
    this.status = Objects.requireNonNull(status)
    return this
  }

  override fun setBlockingTasks(tasks: Iterable<Task>): TaskBuilderImpl {
    val taskIds = Observable.fromIterable(tasks)
        .map { task: Task -> store().validateTask(task) }
        .map { obj: TaskImpl -> obj.id() }
        .blockingIterable()
    blockingTasksToAdd.clear()
    taskIds.forEach(Consumer { item: TaskIdImpl -> blockingTasksToAdd.add(item) })
    return this
  }

  override fun addBlockingTask(task: Task): TaskBuilderImpl {
    val taskImpl = store().validateTask(task)
    blockingTasksToAdd.add(taskImpl.id())
    return this
  }

  override fun setBlockedTasks(tasks: Iterable<Task>): TaskBuilderImpl {
    val taskIds = Observable.fromIterable(tasks)
        .map { task: Task -> store().validateTask(task) }
        .map { obj: TaskImpl -> obj.id() }
        .blockingIterable()
    blockedTasksToAdd.clear()
    taskIds.forEach(Consumer { item: TaskIdImpl -> blockedTasksToAdd.add(item) })
    return this
  }

  override fun addBlockedTask(task: Task): TaskBuilderImpl {
    val taskImpl = store().validateTask(task)
    blockedTasksToAdd.add(taskImpl.id())
    return this
  }

  fun store(): ObservableTaskStoreImpl {
    return taskStore
  }

  fun completed(): Boolean {
    return status().isCompleted
  }

  fun status(): Task.Status {
    return status
  }

  fun label(): String {
    return label
  }

  fun blockingTasks(): Set<TaskIdImpl> {
    return ImmutableSet.copyOf(blockingTasksToAdd)
  }

  fun blockedTasks(): Set<TaskIdImpl> {
    return ImmutableSet.copyOf(blockedTasksToAdd)
  }
}