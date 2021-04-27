package me.strangepan.tasks.cli.feature.graph

import omnia.data.cache.Memoized
import omnia.data.structure.List
import org.apache.commons.cli.CommandLine
import me.strangepan.tasks.cli.parser.CommandParser
import me.strangepan.tasks.cli.parser.ParseResult
import me.strangepan.tasks.cli.parser.Parser
import me.strangepan.tasks.cli.parser.ParserUtil
import me.strangepan.tasks.engine.model.Task

/** Parser for graph/xl command.  */
class GraphParser(
  private val taskParser: Memoized<out Parser<out List<out ParseResult<out Task>>>>
) : CommandParser<GraphArguments> {
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
    val tasksToGetBlockersFor = taskParser.value().parse(
      ParserUtil.getOptionValues(commandLine, GraphCommand.BEFORE_TASKS_OPTION.value()))
    return GraphArguments(
      isAllSet,
      ParserUtil.extractSuccessfulResultsOrThrow(tasksToRelateTo),
      ParserUtil.extractSuccessfulResultsOrThrow(tasksToGetBlockersFor))
  }
}