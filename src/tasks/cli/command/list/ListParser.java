package tasks.cli.command.list;

import static tasks.cli.parser.ParserUtil.assertNoExtraArgs;

import org.apache.commons.cli.CommandLine;
import tasks.cli.parser.ParserUtil;
import tasks.cli.parser.CommandParser;

/** Command line argument parser for the List command. */
public final class ListParser implements CommandParser<ListArguments> {

  @Override
  public ListArguments parse(CommandLine commandLine) {
    /*
     * No non-flag parameters allowed
     * optional --blocked flag
     * optional --completed flag
     * optional --all flag
     */
    assertNoExtraArgs(commandLine);

    boolean isAllSet = ParserUtil.getFlagPresence(commandLine, ListCommand.ALL_OPTION.value());
    boolean isBlockedSet =
        isAllSet || ParserUtil.getFlagPresence(commandLine, ListCommand.BLOCKED_OPTION.value());
    boolean isCompletedSet =
        isAllSet || ParserUtil.getFlagPresence(commandLine, ListCommand.COMPLETED_OPTION.value());
    boolean isUnblockedSet =
        isAllSet
            || ParserUtil.getFlagPresence(commandLine, ListCommand.UNBLOCKED_OPTION.value())
            || (!isBlockedSet && !isCompletedSet);

    return new ListArguments(isUnblockedSet, isBlockedSet, isCompletedSet);
  }
}
