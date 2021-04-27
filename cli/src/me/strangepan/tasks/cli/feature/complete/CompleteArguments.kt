package me.strangepan.tasks.cli.feature.complete

import omnia.data.structure.List
import me.strangepan.tasks.cli.command.common.simple.SimpleArguments
import me.strangepan.tasks.engine.model.Task

/** Model for parsed Complete command arguments.  */
class CompleteArguments internal constructor(tasks: List<Task>) : SimpleArguments(tasks)