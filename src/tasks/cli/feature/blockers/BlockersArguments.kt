package tasks.cli.feature.blockers

import omnia.data.structure.List
import tasks.model.Task

/** Model for parsed Blockers command arguments.  */
class BlockersArguments internal constructor(
    private val targetTask: Task,
    private val blockingTasksToAdd: List<Task>,
    private val blockingTasksToRemove: List<Task>,
    private val clearAllBlockers: Boolean) {
  /** The task whose blockers to modify.  */
  fun targetTask(): Task {
    return targetTask
  }

  /** The collection of tasks to add as blockers to [targetTask].  */
  fun blockingTasksToAdd(): List<Task> {
    return blockingTasksToAdd
  }

  /**
   * The collection of tasks to remove as blockers from [targetTask]. These tasks are in
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
   * A flag indicating that all current blockers of [targetTask] should be removed. This
   * is to be applied before other [blockingTasksToAdd].
   */
  fun clearAllBlockers(): Boolean {
    return clearAllBlockers
  }
}