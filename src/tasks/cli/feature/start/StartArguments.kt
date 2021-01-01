package tasks.cli.feature.start

import omnia.data.structure.List
import tasks.cli.command.common.simple.SimpleArguments
import tasks.model.Task

/** Model for parsed Start command arguments.  */
class StartArguments internal constructor(tasks: List<Task>) : SimpleArguments(tasks)