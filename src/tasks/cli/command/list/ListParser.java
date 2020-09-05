package tasks.cli.command.list;

import static tasks.cli.arg.CliUtils.assertNoExtraArgs;

import org.apache.commons.cli.CommandLine;
import tasks.cli.arg.CliArguments;
import tasks.cli.arg.CliUtils;

/** Command line argument parser for the List command. */
public final class ListParser implements CliArguments.CommandParser<ListArguments> {

  @Override
  public ListArguments parse(CommandLine commandLine) {
    /*
     * No non-flag parameters allowed
     * optional --blocked flag
     * optional --completed flag
     */
    assertNoExtraArgs(commandLine);

    boolean isAllSet = CliUtils.getFlagPresence(commandLine, ListCommand.ALL_OPTION.value());
    boolean isBlockedSet =
        isAllSet || CliUtils.getFlagPresence(commandLine, ListCommand.BLOCKED_OPTION.value());
    boolean isCompletedSet =
        isAllSet || CliUtils.getFlagPresence(commandLine, ListCommand.COMPLETED_OPTION.value());
    boolean isUnblockedSet =
        isAllSet
            || CliUtils.getFlagPresence(commandLine, ListCommand.UNBLOCKED_OPTION.value())
            || (!isBlockedSet && !isCompletedSet);

    return new ListArguments(isUnblockedSet, isBlockedSet, isCompletedSet);
  }
}
