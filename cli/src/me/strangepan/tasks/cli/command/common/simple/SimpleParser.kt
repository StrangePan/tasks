package me.strangepan.tasks.cli.command.common.simple

import java.util.function.Function
import omnia.data.cache.Memoized
import omnia.data.structure.List
import omnia.data.structure.immutable.ImmutableList
import org.apache.commons.cli.CommandLine
import me.strangepan.tasks.cli.parser.CommandParser
import me.strangepan.tasks.cli.parser.ParseResult
import me.strangepan.tasks.cli.parser.Parser
import me.strangepan.tasks.cli.parser.ParserException
import me.strangepan.tasks.cli.parser.ParserUtil
import me.strangepan.tasks.engine.model.Task

abstract class SimpleParser<T : SimpleArguments> protected constructor(
    private val constructor: Function<List<Task>, T>,
    private val taskParser: Memoized<out Parser<out List<out ParseResult<out Task>>>>) : CommandParser<T> {

  override fun parse(commandLine: CommandLine): T {
    /*
     * All params must be task IDs
     */
    val argsList: List<String> = ImmutableList.copyOf(commandLine.argList)
    if (argsList.count() < 1) {
      throw ParserException("No task IDs specified")
    }
    return constructor.apply(
        ParserUtil.extractSuccessfulResultsOrThrow(taskParser.value().parse(argsList)))
  }

}