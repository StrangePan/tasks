package tasks.cli.feature.graph

import omnia.data.cache.Memoized
import omnia.data.structure.List
import org.apache.commons.cli.CommandLine
import tasks.cli.feature.blockers.BlockersCommand
import tasks.cli.parser.CommandParser
import tasks.cli.parser.ParseResult
import tasks.cli.parser.Parser
import tasks.cli.parser.ParserUtil
import tasks.model.Task

/** Parser for graph/xl command.  */
class GraphParser(
    private val taskParser: Memoized<out Parser<out List<out ParseResult<out Task>>>>)
    : CommandParser<GraphArguments> {
  override fun parse(commandLine: CommandLine): GraphArguments {
    /*
     * No non-flag parameters allowed
     * optional --all flag
     * optional --related tasks
     */
    ParserUtil.assertNoExtraArgs(commandLine)
    val isAllSet = ParserUtil.getFlagPresence(commandLine, GraphCommand.ALL_OPTION.value())
    val tasksToRelateTo = taskParser.value().parse(
        ParserUtil.getOptionValues(commandLine, GraphCommand.RELATED_TASKS_OPTION.value()))
    return GraphArguments(
      isAllSet, ParserUtil.extractSuccessfulResultsOrThrow(tasksToRelateTo))
  }
}