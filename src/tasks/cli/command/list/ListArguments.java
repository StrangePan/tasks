package tasks.cli.command.list;

import static tasks.cli.arg.CliUtils.assertNoExtraArgs;
import static tasks.cli.arg.CliUtils.tryParse;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public final class ListArguments {
  private final boolean isUnblockedSet;
  private final boolean isBlockedSet;
  private final boolean isCompletedSet;

  private ListArguments(boolean isUnblockedSet, boolean isBlockedSet, boolean isCompletedSet) {
    this.isUnblockedSet = isUnblockedSet;
    this.isBlockedSet = isBlockedSet;
    this.isCompletedSet = isCompletedSet;
  }

  public boolean isUnblockedSet() {
    return isUnblockedSet;
  }

  public boolean isBlockedSet() {
    return isBlockedSet;
  }

  public boolean isCompletedSet() {
    return isCompletedSet;
  }

  public static ListArguments parse(String[] args) {
    /*
    First arg is assumed to be "ls" or an alias thereof
    No other unclassified arg allowed
    optional --blocked flag
    optional --completed flag
    */
    Options options = new Options();
    options.addOption(
        Option.builder("b")
            .longOpt("blocked")
            .desc("Setting this flag lists tasks that are uncompleted, but blocked by other tasks")
            .numberOfArgs(0)
            .build());
    options.addOption(
        Option.builder("c")
            .longOpt("completed")
            .desc("Setting this flag lists tasks that are already marked as completed.")
            .numberOfArgs(0)
            .build());
    options.addOption(
        Option.builder("u")
            .longOpt("unblocked")
            .desc("Setting this flag lists tasks that are unblocked. This is the default.")
            .numberOfArgs(0)
            .build());
    options.addOption(
        Option.builder("a")
            .longOpt("all")
            .desc("Setting this flag lists all tasks.")
            .numberOfArgs(0)
            .build());

    CommandLine commandLine = tryParse(args, options);
    assertNoExtraArgs(commandLine);

    boolean isAllSet = commandLine.hasOption("a");
    boolean isBlockedSet = isAllSet || commandLine.hasOption("b");
    boolean isCompletedSet = isAllSet || commandLine.hasOption("c");
    boolean isUnblockedSet =
        isAllSet || commandLine.hasOption("u") || (!isBlockedSet && !isCompletedSet);

    return new ListArguments(isUnblockedSet, isBlockedSet, isCompletedSet);
  }
}
