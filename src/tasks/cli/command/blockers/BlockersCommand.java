package tasks.cli.command.blockers;

import static omnia.data.cache.Memoized.memoize;
import static tasks.cli.command.Parameter.Repeatable.NOT_REPEATABLE;
import static tasks.cli.command.Parameter.Repeatable.REPEATABLE;

import omnia.data.cache.Memoized;
import omnia.data.structure.List;
import omnia.data.structure.immutable.ImmutableList;
import tasks.cli.parser.ParserUtil;
import tasks.cli.command.Command;
import tasks.cli.command.FlagOption;
import tasks.cli.command.Option;
import tasks.cli.command.TaskOption;
import tasks.cli.command.TaskParameter;
import tasks.cli.parser.Parser;
import tasks.model.Task;

/** Canonical definition for the Blockers command. */
public final class BlockersCommand {
  private BlockersCommand() {}

  public static Command registration(
      Memoized<Parser<? extends List<ParserUtil.ParseResult<Task>>>> taskParser) {
    return Command.builder()
        .canonicalName("blockers")
        .aliases("blocker", "bk")
        .parameters(ImmutableList.of(new TaskParameter(NOT_REPEATABLE)))
        .options(OPTIONS.value())
        .parser(() -> new BlockersParser(taskParser))
        .helpDocumentation(
            "Modifies or lists blockers of an existing task. Can be used to add or remove blockers "
                + "from a task. If no modifications are specified, simply lists the existing "
                + "blockers for a task.");
  }

  static final Memoized<TaskOption> ADD_OPTION =
      memoize(
          () -> new TaskOption(
              "add",
              "a",
              "Adds another task as a blocker.",
              REPEATABLE));

  static final Memoized<FlagOption> CLEAR_OPTION =
      memoize(
          () -> new FlagOption(
              "clear",
              "c",
              "Removes all blocking tasks. Can be used together with --"
                  + ADD_OPTION.value().longName() + " to replace existing blockers with new ones.",
              NOT_REPEATABLE));

  static final Memoized<TaskOption> REMOVE_OPTION =
      memoize(
          () -> new TaskOption(
              "remove",
              "d",
              "Removes another task from being a blocker. Ignored if --"
                  + CLEAR_OPTION.value().longName() + " is set.",
              REPEATABLE));

  static final Memoized<ImmutableList<Option>> OPTIONS =
      memoize(
          () -> ImmutableList.of(
              ADD_OPTION.value(),
              CLEAR_OPTION.value(),
              REMOVE_OPTION.value()));
}
