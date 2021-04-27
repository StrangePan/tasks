package me.strangepan.tasks.cli.feature.start

import java.util.function.Function
import omnia.data.cache.Memoized
import omnia.data.structure.List
import me.strangepan.tasks.cli.command.common.simple.SimpleParser
import me.strangepan.tasks.cli.parser.ParseResult
import me.strangepan.tasks.cli.parser.Parser
import me.strangepan.tasks.engine.model.Task

/** Command line argument parser for the Start command.  */
class StartParser(
    taskParser: Memoized<out Parser<out List<out ParseResult<out Task>>>>) : SimpleParser<StartArguments>(Function { tasks: List<Task> -> StartArguments(tasks) }, taskParser)