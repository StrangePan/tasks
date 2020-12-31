package tasks.cli.feature.stop

import omnia.data.structure.List
import tasks.cli.command.common.simple.SimpleArguments
import tasks.model.Task

/** Model for parsed Stop command arguments.  */
class StopArguments internal constructor(tasks: List<Task>) : SimpleArguments(tasks) {
  /** The tasks to mark ask open, in the order specified in the command line.  */
  public override fun tasks(): List<Task> {
    return super.tasks()
  }
}