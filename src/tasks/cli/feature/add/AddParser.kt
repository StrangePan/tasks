package tasks.cli.feature.add

import java.util.Optional
import omnia.data.cache.Memoized
import omnia.data.structure.List
import omnia.data.structure.immutable.ImmutableList
import org.apache.commons.cli.CommandLine
import tasks.cli.parser.CommandParser
import tasks.cli.parser.ParseResult
import tasks.cli.parser.Parser
import tasks.cli.parser.ParserException
import tasks.cli.parser.ParserUtil
import tasks.model.Task

/** Command line argument parser for the Add command.  */
class AddParser(
    private val taskParser: Memoized<out Parser<out List<out ParseResult<out Task>>>>) : CommandParser<AddArguments> {
  override fun parse(commandLine: CommandLine): AddArguments {
    /*
     * 1st param must be description
     * 2+ params not supported
     * optional blocking tasks
     * optional blocked tasks
     */
    val argsList: List<String> = ImmutableList.copyOf(commandLine.argList)
    val taskDescription = extractTaskDescriptionFrom(argsList)
        .orElseThrow { ParserException("Task description not defined") }
    ParserUtil.assertNoExtraArgs(commandLine, AddCommand.COMMAND_PARAMETERS.value())
    val afterTasks = taskParser.value().parse(ParserUtil.getOptionValues(commandLine, AddCommand.AFTER_OPTION.value()))
    val beforeTasks = taskParser.value().parse(ParserUtil.getOptionValues(commandLine, AddCommand.BEFORE_OPTION.value()))

    // Initial validation combines before and after into a nice aggregate message
    ParserUtil.extractSuccessfulResultsOrThrow(
        ImmutableList.builder<ParseResult<*>>()
            .addAll(afterTasks)
            .addAll(beforeTasks)
            .build())
    return AddArguments(
        taskDescription,
        ParserUtil.extractSuccessfulResultsOrThrow(afterTasks),
        ParserUtil.extractSuccessfulResultsOrThrow(beforeTasks))
  }

  companion object {
    private fun extractTaskDescriptionFrom(args: List<String>): Optional<String> {
      return if (args.count() < 1) Optional.empty() else Optional.of(args.itemAt(0))
    }
  }
}