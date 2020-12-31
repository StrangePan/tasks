package tasks.cli.feature.info

import omnia.data.structure.List
import tasks.cli.command.common.simple.SimpleArguments
import tasks.model.Task

/** Model for parsed Info command arguments.  */
class InfoArguments internal constructor(tasks: List<Task>) : SimpleArguments(tasks) {
  /** The tasks for which to display information, in the order specified in the command line.  */
  public override fun tasks(): List<Task> {
    return super.tasks()
  }
}