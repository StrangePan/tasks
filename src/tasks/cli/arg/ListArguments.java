package tasks.cli.arg;

import static tasks.cli.arg.CliUtils.assertNoExtraArgs;
import static tasks.cli.arg.CliUtils.tryParse;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public final class ListArguments {
  private final boolean isBlockedSet;
  private final boolean isCompletedSet;

  private ListArguments(boolean isBlockedSet, boolean isCompletedSet) {
    this.isBlockedSet = isBlockedSet;
    this.isCompletedSet = isCompletedSet;
  }

  public boolean isBlockedSet() {
    return isBlockedSet;
  }

  public boolean isCompletedSet() {
    return isCompletedSet;
  }

  static ListArguments parse(String[] args) {
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

    CommandLine commandLine = tryParse(args, options);
    assertNoExtraArgs(commandLine);

    boolean isBlockedSet = commandLine.hasOption("b");
    boolean isCompletedSet = commandLine.hasOption("c");

    return new ListArguments(isBlockedSet, isCompletedSet);
  }
}
