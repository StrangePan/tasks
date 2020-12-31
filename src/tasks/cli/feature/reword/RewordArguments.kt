package tasks.cli.feature.reword

import tasks.model.Task

class RewordArguments internal constructor(private val targetTask: Task, private val description: String) {
  /** The target to reword.  */
  fun targetTask(): Task {
    return targetTask
  }

  /** The new description for the task.  */
  fun description(): String {
    return description
  }
}