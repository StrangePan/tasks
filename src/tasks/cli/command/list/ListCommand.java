package tasks.cli.command.list;

import static omnia.data.cache.Memoized.memoize;
import static tasks.cli.arg.CliArguments.Parameter.Repeatable.NOT_REPEATABLE;

import omnia.data.cache.Memoized;
import omnia.data.structure.immutable.ImmutableList;
import tasks.cli.arg.CliArguments;
import tasks.cli.arg.CliMode;

/** Canonical definition for the List command. */
public final class ListCommand {
  private ListCommand() {}

  public static CliArguments.CommandRegistration registration() {
    return CliArguments.CommandRegistration.builder()
        .cliMode(CliMode.LIST)
        .canonicalName("list")
        .aliases("ls", "l")
        .parameters(ImmutableList.empty())
        .options(OPTIONS.value())
        .parser(ListParser::new)
        .helpDocumentation(
            "Prints a list of tasks. By default, only lists uncompleted tasks that are "
                + "unblocked. Can also list only blocked tasks, only completed tasks, any "
                + "combination of the three, or all tasks.");
  }

  static final Memoized<CliArguments.FlagOption> BLOCKED_OPTION =
      memoize(
          () -> new CliArguments.FlagOption(
              "blocked",
              "b",
              "List all tasks that are uncompleted, but blocked by other tasks. Can "
                  + "be combined with other flags.",
              NOT_REPEATABLE));

  static final Memoized<CliArguments.FlagOption> COMPLETED_OPTION =
      memoize(
          () -> new CliArguments.FlagOption(
              "completed",
              "c",
              "List all tasks already marked as completed. Can be combined with "
                  + "other flags.",
              NOT_REPEATABLE));

  static final Memoized<CliArguments.FlagOption> UNBLOCKED_OPTION =
      memoize(
          () -> new CliArguments.FlagOption(
              "unblocked",
              "u",
              "List all unblocked tasks. Can be combined with other flags.",
              NOT_REPEATABLE));

  static final Memoized<CliArguments.FlagOption> ALL_OPTION =
      memoize(
          () -> new CliArguments.FlagOption(
              "all",
              "a",
              "Lists all tasks. A shortcut for all other flags put together.",
              NOT_REPEATABLE));

  static final Memoized<ImmutableList<CliArguments.Option>> OPTIONS =
      memoize(
          () -> ImmutableList.of(
              BLOCKED_OPTION.value(),
              COMPLETED_OPTION.value(),
              UNBLOCKED_OPTION.value(),
              ALL_OPTION.value()));
}
