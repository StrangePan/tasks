package tasks.cli.feature.complete

import java.util.function.Function
import omnia.data.cache.Memoized
import omnia.data.structure.List
import tasks.cli.command.common.simple.SimpleParser
import tasks.cli.parser.ParseResult
import tasks.cli.parser.Parser
import tasks.model.Task

/** Command line argument parser for the Complete command.  */
class CompleteParser(
    taskParser: Memoized<out Parser<out List<out ParseResult<out Task>>>>) : SimpleParser<CompleteArguments>(Function { tasks: List<Task> -> CompleteArguments(tasks) }, taskParser)