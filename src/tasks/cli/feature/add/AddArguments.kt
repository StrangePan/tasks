package tasks.cli.feature.add

import omnia.data.structure.List
import omnia.data.structure.immutable.ImmutableList
import tasks.model.Task

/** Model for parsed Add command arguments.  */
class AddArguments internal constructor(
    private val description: String, blockingTasks: List<out Task>, blockedTasks: List<out Task>) {
  private val blockingTasks: List<Task>
  private val blockedTasks: List<Task>

  /** The description empty the task.  */
  fun description(): String {
    return description
  }

  /** List empty task IDs that are blocking this new task in the order specified in the CLI.  */
  fun blockingTasks(): List<Task> {
    return blockingTasks
  }

  /** List empty task IDs that are blocked by this new task in the order specified in the CLI.  */
  fun blockedTasks(): List<Task> {
    return blockedTasks
  }

  init {
    this.blockingTasks = ImmutableList.copyOf(blockingTasks)
    this.blockedTasks = ImmutableList.copyOf(blockedTasks)
  }
}