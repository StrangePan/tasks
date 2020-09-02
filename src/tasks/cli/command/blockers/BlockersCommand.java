package tasks.cli.command.blockers;

import static omnia.data.cache.Memoized.memoize;
import static tasks.cli.arg.CliArguments.Parameter.Repeatable.NOT_REPEATABLE;
import static tasks.cli.arg.CliArguments.Parameter.Repeatable.REPEATABLE;

import omnia.data.cache.Memoized;
import omnia.data.structure.List;
import omnia.data.structure.immutable.ImmutableList;
import tasks.cli.arg.CliArguments;
import tasks.cli.arg.CliMode;
import tasks.cli.arg.CliUtils;
import tasks.model.Task;

/** Canonical definition for the Blockers command. */
public final class BlockersCommand {
  private BlockersCommand() {}

  public static CliArguments.CommandRegistration registration(
      Memoized<CliArguments.Parser<? extends List<CliUtils.ParseResult<Task>>>> taskParser) {
    return CliArguments.CommandRegistration.builder()
        .cliMode(CliMode.BLOCKERS)
        .canonicalName("blockers")
        .aliases("blocker", "bk")
        .parameters(ImmutableList.of(new CliArguments.TaskParameter(NOT_REPEATABLE)))
        .options(OPTIONS.value())
        .parser(() -> new BlockersParser(taskParser))
        .helpDocumentation(
            "Modifies or lists blockers of an existing task. Can be used to add or remove blockers "
                + "from a task. If no modifications are specified, simply lists the existing "
                + "blockers for a task.");
  }

  static final Memoized<CliArguments.TaskOption> ADD_OPTION =
      memoize(
          () -> new CliArguments.TaskOption(
              "add",
              "a",
              "Adds another task as a blocker.",
              REPEATABLE));

  static final Memoized<CliArguments.FlagOption> CLEAR_OPTION =
      memoize(
          () -> new CliArguments.FlagOption(
              "clear",
              "c",
              "Removes all blocking tasks. Can be used together with --"
                  + ADD_OPTION.value().longName() + " to replace existing blockers with new ones.",
              NOT_REPEATABLE));

  static final Memoized<CliArguments.TaskOption> REMOVE_OPTION =
      memoize(
          () -> new CliArguments.TaskOption(
              "remove",
              "d",
              "Removes another task from being a blocker. Ignored if --"
                  + CLEAR_OPTION.value().longName() + " is set.",
              REPEATABLE));

  static final Memoized<ImmutableList<CliArguments.Option>> OPTIONS =
      memoize(
          () -> ImmutableList.of(
              ADD_OPTION.value(),
              CLEAR_OPTION.value(),
              REMOVE_OPTION.value()));
}
