package tasks.cli.feature.graph

import org.apache.commons.cli.CommandLine
import tasks.cli.parser.CommandParser
import tasks.cli.parser.ParserUtil

/** Parser for graph/xl command.  */
class GraphParser : CommandParser<GraphArguments> {
  override fun parse(commandLine: CommandLine): GraphArguments {
    /*
     * No non-flag parameters allowed
     * optional --completed flag
     * optional --all flag
     */
    ParserUtil.assertNoExtraArgs(commandLine)
    val isAllSet = ParserUtil.getFlagPresence(commandLine, GraphCommand.ALL_OPTION.value())
    return GraphArguments(isAllSet)
  }
}