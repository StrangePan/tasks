package tasks.cli.command.list;

import static tasks.cli.arg.CliArguments.Parameter.Repeatable.NOT_REPEATABLE;
import static tasks.cli.arg.CliUtils.assertNoExtraArgs;
import static tasks.cli.arg.CliUtils.tryParse;

import omnia.data.structure.immutable.ImmutableList;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import tasks.cli.arg.CliArguments;
import tasks.cli.arg.CliMode;

public final class ListArguments {
  private final boolean isUnblockedSet;
  private final boolean isBlockedSet;
  private final boolean isCompletedSet;

  private ListArguments(boolean isUnblockedSet, boolean isBlockedSet, boolean isCompletedSet) {
    this.isUnblockedSet = isUnblockedSet;
    this.isBlockedSet = isBlockedSet;
    this.isCompletedSet = isCompletedSet;
  }

  public static CliArguments.CommandRegistration registration() {
    return CliArguments.CommandRegistration.builder()
        .cliMode(CliMode.LIST)
        .canonicalName("list")
        .aliases("ls", "l")
        .parameters(ImmutableList.empty())
        .options(
            ImmutableList.of(
                new CliArguments.FlagOption(
                    "blocked",
                    "b",
                    "List all tasks that are uncompleted, but blocked by other tasks. Can "
                        + "be combined with other flags.",
                    NOT_REPEATABLE),
                new CliArguments.FlagOption(
                    "completed",
                    "c",
                    "List all tasks already marked as completed. Can be combined with "
                        + "other flags.",
                    NOT_REPEATABLE),
                new CliArguments.FlagOption(
                    "unblocked",
                    "u",
                    "List all unblocked tasks. Can be combined with other flags.",
                    NOT_REPEATABLE),
                new CliArguments.FlagOption(
                    "all",
                    "a",
                    "Lists all tasks. A shortcut for all other flags put together.",
                    NOT_REPEATABLE)))
        .parser(() -> ListArguments::parse)
        .helpDocumentation(
            "Prints a list of tasks. By default, only lists uncompleted tasks that are "
                + "unblocked. Can also list only blocked tasks, only completed tasks, any "
                + "combination of the three, or all tasks.");
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
