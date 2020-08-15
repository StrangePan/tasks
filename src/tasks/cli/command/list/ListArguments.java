package tasks.cli.command.list;

import static omnia.data.cache.Memoized.memoize;
import static tasks.cli.arg.CliArguments.Parameter.Repeatable.NOT_REPEATABLE;
import static tasks.cli.arg.CliUtils.assertNoExtraArgs;
import static tasks.cli.arg.CliUtils.tryParse;

import omnia.data.cache.Memoized;
import omnia.data.structure.immutable.ImmutableList;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import tasks.cli.arg.CliArguments;
import tasks.cli.arg.CliMode;
import tasks.cli.arg.CliUtils;

public final class ListArguments {
  private final boolean isUnblockedSet;
  private final boolean isBlockedSet;
  private final boolean isCompletedSet;

  private static final Memoized<CliArguments.FlagOption> BLOCKED_OPTION =
      memoize(
          () -> new CliArguments.FlagOption(
              "blocked",
              "b",
              "List all tasks that are uncompleted, but blocked by other tasks. Can "
                  + "be combined with other flags.",
              NOT_REPEATABLE));

  private static final Memoized<CliArguments.FlagOption> COMPLETED_OPTION =
      memoize(
          () -> new CliArguments.FlagOption(
              "completed",
              "c",
              "List all tasks already marked as completed. Can be combined with "
                  + "other flags.",
              NOT_REPEATABLE));

  private static final Memoized<CliArguments.FlagOption> UNBLOCKED_OPTION =
      memoize(
          () -> new CliArguments.FlagOption(
              "unblocked",
              "u",
              "List all unblocked tasks. Can be combined with other flags.",
              NOT_REPEATABLE));

  private static final Memoized<CliArguments.FlagOption> ALL_OPTION =
      memoize(
          () -> new CliArguments.FlagOption(
              "all",
              "a",
              "Lists all tasks. A shortcut for all other flags put together.",
              NOT_REPEATABLE));

  private static final Memoized<ImmutableList<CliArguments.Option>> OPTIONS =
      memoize(
          () -> ImmutableList.of(
              BLOCKED_OPTION.value(),
              COMPLETED_OPTION.value(),
              UNBLOCKED_OPTION.value(),
              ALL_OPTION.value()));

  public static CliArguments.CommandRegistration registration() {
    return CliArguments.CommandRegistration.builder()
        .cliMode(CliMode.LIST)
        .canonicalName("list")
        .aliases("ls", "l")
        .parameters(ImmutableList.empty())
        .options(OPTIONS.value())
        .parser(Parser::new)
        .helpDocumentation(
            "Prints a list of tasks. By default, only lists uncompleted tasks that are "
                + "unblocked. Can also list only blocked tasks, only completed tasks, any "
                + "combination of the three, or all tasks.");
  }

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

  public static final class Parser implements CliArguments.Parser<ListArguments> {

    @Override
    public ListArguments parse(String[] args) {
      /*
      First arg is assumed to be "ls" or an alias thereof
      No other unclassified arg allowed
      optional --blocked flag
      optional --completed flag
      */
      Options options = CliUtils.toOptions(OPTIONS.value());

      CommandLine commandLine = tryParse(args, options);
      assertNoExtraArgs(commandLine);

      boolean isAllSet = commandLine.hasOption(ALL_OPTION.value().shortName());
      boolean isBlockedSet = isAllSet || commandLine.hasOption(BLOCKED_OPTION.value().shortName());
      boolean isCompletedSet =
          isAllSet || commandLine.hasOption(COMPLETED_OPTION.value().shortName());
      boolean isUnblockedSet =
          isAllSet
              || commandLine.hasOption(UNBLOCKED_OPTION.value().shortName())
              || (!isBlockedSet && !isCompletedSet);

      return new ListArguments(isUnblockedSet, isBlockedSet, isCompletedSet);
    }
  }
}
