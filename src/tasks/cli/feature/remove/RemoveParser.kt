package tasks.cli.feature.remove

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

/** Command line argument parser for the Remove command.  */
class RemoveParser(
    private val taskParser: Memoized<out Parser<out List<out ParseResult<out Task>>>>) : CommandParser<RemoveArguments> {

  override fun parse(commandLine: CommandLine): RemoveArguments {
    /*
     * All params must be task IDs.
     * optional --force flag
     */
    val argsList: List<String> = ImmutableList.copyOf(commandLine.argList)
    if (argsList.count() < 1) {
      throw ParserException("No task IDs specified")
    }
    return RemoveArguments(
        ParserUtil.extractSuccessfulResultsOrThrow(taskParser.value().parse(argsList)),
        ParserUtil.getFlagPresence(commandLine, RemoveCommand.FORCE_OPTION.value()))
  }

}