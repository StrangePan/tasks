package me.strangepan.tasks.cli.feature.list

import org.apache.commons.cli.CommandLine
import me.strangepan.tasks.cli.parser.CommandParser
import me.strangepan.tasks.cli.parser.ParserUtil

/** Command line argument parser for the List command.  */
class ListParser : CommandParser<ListArguments> {
  override fun parse(commandLine: CommandLine): ListArguments {
    /*
     * No non-flag parameters allowed
     * optional --blocked flag
     * optional --completed flag
     * optional --all flag
     */
    ParserUtil.assertNoExtraArgs(commandLine)
    val isAllSet = ParserUtil.getFlagPresence(commandLine, ListCommand.ALL_OPTION.value())
    val isBlockedSet = isAllSet || ParserUtil.getFlagPresence(commandLine, ListCommand.BLOCKED_OPTION.value())
    val isCompletedSet = isAllSet || ParserUtil.getFlagPresence(commandLine, ListCommand.COMPLETED_OPTION.value())
    val isStartedSet = ParserUtil.getFlagPresence(commandLine, ListCommand.STARTED_OPTION.value())
    val isUnblockedSet = (isAllSet
        || ParserUtil.getFlagPresence(commandLine, ListCommand.UNBLOCKED_OPTION.value())
        || !isBlockedSet && !isCompletedSet)
    return ListArguments(isUnblockedSet, isBlockedSet, isCompletedSet, isStartedSet)
  }
}