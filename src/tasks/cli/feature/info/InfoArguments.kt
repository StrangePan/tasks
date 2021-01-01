package tasks.cli.feature.info

import omnia.data.structure.List
import tasks.cli.command.common.simple.SimpleArguments
import tasks.model.Task

/** Model for parsed Info command arguments.  */
class InfoArguments internal constructor(tasks: List<Task>) : SimpleArguments(tasks)