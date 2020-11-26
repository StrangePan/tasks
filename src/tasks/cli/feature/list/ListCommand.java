package tasks.cli.feature.list;

import static omnia.data.cache.Memoized.memoize;
import static tasks.cli.command.Parameter.Repeatable.NOT_REPEATABLE;

import omnia.data.cache.Memoized;
import omnia.data.structure.immutable.ImmutableList;
import tasks.cli.command.Command;
import tasks.cli.command.FlagOption;
import tasks.cli.command.Option;

/** Canonical definition for the List command. */
public final class ListCommand {
  private ListCommand() {}

  public static Command registration() {
    return COMMAND.value();
  }

  static final Memoized<FlagOption> BLOCKED_OPTION =
      memoize(
          () -> new FlagOption(
              "blocked",
              "b",
              "List all tasks that are uncompleted, but blocked by other tasks. Can "
                  + "be combined with other flags.",
              NOT_REPEATABLE));

  public static final Memoized<FlagOption> COMPLETED_OPTION =
      memoize(
          () -> new FlagOption(
              "completed",
              "c",
              "List all tasks already marked as completed. Can be combined with "
                  + "other flags.",
              NOT_REPEATABLE));

  static final Memoized<FlagOption> UNBLOCKED_OPTION =
      memoize(
          () -> new FlagOption(
              "unblocked",
              "u",
              "List all unblocked tasks. Can be combined with other flags.",
              NOT_REPEATABLE));

  static final Memoized<FlagOption> STARTED_OPTION =
      memoize(
          () -> new FlagOption(
              "started",
              "s",
              "List all started tasks. Can be combined with other flags.",
              NOT_REPEATABLE));

  public static final Memoized<FlagOption> ALL_OPTION =
      memoize(
          () -> new FlagOption(
              "all",
              "a",
              "Lists all tasks. A shortcut for all other flags put together.",
              NOT_REPEATABLE));

  static final Memoized<ImmutableList<Option>> OPTIONS =
      memoize(
          () -> ImmutableList.of(
              BLOCKED_OPTION.value(),
              COMPLETED_OPTION.value(),
              UNBLOCKED_OPTION.value(),
              STARTED_OPTION.value(),
              ALL_OPTION.value()));

  private static final Memoized<Command> COMMAND =
      memoize(
          () -> Command.builder()
              .canonicalName("list")
              .aliases("ls", "l")
              .parameters(ImmutableList.empty())
              .options(OPTIONS.value())
              .helpDocumentation(
                  "Prints a list of tasks. By default, only lists uncompleted tasks that are "
                      + "unblocked. Can also list only blocked tasks, only completed tasks, any "
                      + "combination of the three, or all tasks."));
}
