package me.strangepan.tasks.cli.feature.blockers

import omnia.data.structure.List
import me.strangepan.tasks.engine.model.Task

/** Model for parsed Blockers command arguments.  */
class BlockersArguments internal constructor(
    private val targetTasks: List<Task>,
    private val blockingTasksToAdd: List<Task>,
    private val blockingTasksToRemove: List<Task>,
    private val clearAllBlockers: Boolean) {

  /** The tasks whose blockers to modify.  */
  fun targetTasks(): List<Task> {
    return targetTasks
  }

  /** The collection of tasks to add as blockers to [targetTasks].  */
  fun blockingTasksToAdd(): List<Task> {
    return blockingTasksToAdd
  }

  /**
   * The collection of tasks to remove as blockers from [targetTasks]. These tasks are in
   * the order defined in the command line.
   *
   *
   * This parameter is to be treated as mutually exclusive with the [clearAllBlockers]
   * flag. If [clearAllBlockers] is set, tasks listed here are redundant.
   */
  fun blockingTasksToRemove(): List<Task> {
    return blockingTasksToRemove
  }

  /**
   * A flag indicating that all current blockers of [targetTasks] should be removed. This
   * is to be applied before other [blockingTasksToAdd].
   */
  fun clearAllBlockers(): Boolean {
    return clearAllBlockers
  }
}