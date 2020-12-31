package tasks.model

interface TaskBuilder {
  fun setStatus(status: Task.Status): TaskBuilder
  fun setBlockingTasks(tasks: Iterable<Task>): TaskBuilder
  fun addBlockingTask(task: Task): TaskBuilder
  fun setBlockedTasks(tasks: Iterable<Task>): TaskBuilder
  fun addBlockedTask(task: Task): TaskBuilder
}