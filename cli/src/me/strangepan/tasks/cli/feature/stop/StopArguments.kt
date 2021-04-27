package me.strangepan.tasks.cli.feature.stop

import omnia.data.structure.List
import me.strangepan.tasks.cli.command.common.simple.SimpleArguments
import me.strangepan.tasks.engine.model.Task

/** Model for parsed Stop command arguments.  */
class StopArguments internal constructor(tasks: List<Task>) : SimpleArguments(tasks)