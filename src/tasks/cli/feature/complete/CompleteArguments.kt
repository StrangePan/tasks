package tasks.cli.feature.complete

import omnia.data.structure.List
import tasks.cli.command.common.simple.SimpleArguments
import tasks.model.Task

/** Model for parsed Complete command arguments.  */
class CompleteArguments internal constructor(tasks: List<Task>) : SimpleArguments(tasks)