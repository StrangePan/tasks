package me.strangepan.tasks.cli.feature.reopen

import omnia.data.structure.List
import me.strangepan.tasks.cli.command.common.simple.SimpleArguments
import me.strangepan.tasks.engine.model.Task

/** Model for parsed Reopen command arguments.  */
class ReopenArguments internal constructor(tasks: List<Task>) : SimpleArguments(tasks)