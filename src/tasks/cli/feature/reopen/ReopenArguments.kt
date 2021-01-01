package tasks.cli.feature.reopen

import omnia.data.structure.List
import tasks.cli.command.common.simple.SimpleArguments
import tasks.model.Task

/** Model for parsed Reopen command arguments.  */
class ReopenArguments internal constructor(tasks: List<Task>) : SimpleArguments(tasks)