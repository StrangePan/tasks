package tasks.cli.command.list;

import static tasks.cli.arg.CliUtils.assertNoExtraArgs;
import static tasks.cli.arg.CliUtils.tryParse;

import omnia.data.structure.List;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import tasks.cli.arg.CliArguments;
import tasks.cli.arg.CliUtils;

/** Command line argument parser for the List command. */
public final class ListParser implements CliArguments.Parser<ListArguments> {

  @Override
  public ListArguments parse(List<? extends String> args) {
    /*
    First arg is assumed to be "ls" or an alias thereof
    No other unclassified arg allowed
    optional --blocked flag
    optional --completed flag
    */
    Options options = CliUtils.toOptions(ListCommand.OPTIONS.value());

    CommandLine commandLine = tryParse(args, options);
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
