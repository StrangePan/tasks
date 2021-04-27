package me.strangepan.tasks.engine.model

interface TaskMutator : TaskBuilder {
  fun setLabel(label: String): TaskMutator
  override fun setStatus(status: Task.Status): TaskMutator
  fun complete(): TaskMutator
  fun reopen(): TaskMutator
  fun start(): TaskMutator
  fun stop(): TaskMutator
  override fun setBlockingTasks(tasks: Iterable<Task>): TaskMutator
  override fun addBlockingTask(task: Task): TaskMutator
  fun removeBlockingTask(task: Task): TaskMutator
  override fun setBlockedTasks(tasks: Iterable<Task>): TaskMutator
  override fun addBlockedTask(task: Task): TaskMutator
  fun removeBlockedTask(task: Task): TaskMutator
}