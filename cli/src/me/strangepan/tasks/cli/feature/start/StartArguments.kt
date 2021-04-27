package me.strangepan.tasks.cli.feature.start

import omnia.data.structure.List
import me.strangepan.tasks.cli.command.common.simple.SimpleArguments
import me.strangepan.tasks.engine.model.Task

/** Model for parsed Start command arguments.  */
class StartArguments internal constructor(tasks: List<Task>) : SimpleArguments(tasks)