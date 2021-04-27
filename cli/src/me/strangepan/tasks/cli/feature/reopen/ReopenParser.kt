package me.strangepan.tasks.cli.feature.reopen

import java.util.function.Function
import omnia.data.cache.Memoized
import omnia.data.structure.List
import me.strangepan.tasks.cli.command.common.simple.SimpleParser
import me.strangepan.tasks.cli.parser.ParseResult
import me.strangepan.tasks.cli.parser.Parser
import me.strangepan.tasks.engine.model.Task

/** Command line argument parser for the Reopen command.  */
class ReopenParser(
    taskParser: Memoized<out Parser<out List<out ParseResult<out Task>>>>) : SimpleParser<ReopenArguments>(Function { tasks: List<Task> -> ReopenArguments(tasks) }, taskParser)