package me.strangepan.tasks.cli.feature.info

import omnia.data.structure.List
import me.strangepan.tasks.cli.command.common.simple.SimpleArguments
import me.strangepan.tasks.engine.model.Task

/** Model for parsed Info command arguments.  */
class InfoArguments internal constructor(tasks: List<Task>) : SimpleArguments(tasks)