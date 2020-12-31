package tasks.cli.feature.start

import omnia.data.structure.List
import tasks.cli.command.common.simple.SimpleArguments
import tasks.model.Task

/** Model for parsed Start command arguments.  */
class StartArguments internal constructor(tasks: List<Task>) : SimpleArguments(tasks) {
  /** The tasks to mark as started, in the order specified in the command line.  */
  public override fun tasks(): List<Task> {
    return super.tasks()
  }
}