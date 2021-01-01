package tasks.cli.command.common.simple

import omnia.data.structure.List
import tasks.model.Task

abstract class SimpleArguments protected constructor(private val tasks: List<Task>) {

  /** The list of tasks parsed from the command line.  */
  open fun tasks(): List<Task> {
    return tasks
  }
}