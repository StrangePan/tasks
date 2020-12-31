package tasks.cli.feature.reword

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

/** Command line argument parser for the Reword command.  */
class RewordParser(
    private val taskParser: Memoized<out Parser<out List<out ParseResult<out Task>>>>) : CommandParser<RewordArguments> {
  override fun parse(commandLine: CommandLine): RewordArguments {
    /*
     * 1st param must be task id
     * 2nd param must be description
     * 3+ params not supported
     */
    val argsList: List<String> = ImmutableList.copyOf(commandLine.argList)
    if (argsList.count() < 1) {
      throw ParserException("No task ID or description specified")
    }
    if (argsList.count() < 2) {
      throw ParserException("No description specified")
    }
    ParserUtil.assertNoExtraArgs(commandLine, RewordCommand.COMMAND_PARAMETERS.value())
    val targetTask = taskParser.value().parse(ImmutableList.of(argsList.itemAt(0))).itemAt(0)
    val description = argsList.itemAt(1)
    return RewordArguments(ParserUtil.extractSuccessfulResultOrThrow(targetTask), description)
  }
}