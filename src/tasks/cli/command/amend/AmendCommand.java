package tasks.cli.command.amend;

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

public class AmendCommand {
  private AmendCommand() {}

  public static CliArguments.CommandRegistration registration(
      Memoized<CliArguments.Parser<? extends List<CliUtils.ParseResult<Task>>>> taskParser) {
    return CliArguments.CommandRegistration.builder()
        .cliMode(CliMode.AMEND)
        .canonicalName("amend")
        .aliases()
        .parameters(ImmutableList.of(new CliArguments.TaskParameter(NOT_REPEATABLE)))
        .options(OPTIONS.value())
        .parser(() -> new AmendParser(taskParser))
        .helpDocumentation(
            "Changes an existing task. Can be used to change the task description or to "
                + "add/remove blocking/blocked tasks.");
  }

  static final Memoized<CliArguments.StringOption> DESCRIPTION_OPTION =
      memoize(
          () -> new CliArguments.StringOption(
              "description",
              "m",
              "Set the task description.",
              NOT_REPEATABLE,
              "description"));

  static final Memoized<CliArguments.TaskOption> AFTER_OPTION =
      memoize(
          () -> new CliArguments.TaskOption(
              "after",
              "a",
              "Sets this task as coming after another task. Tasks listed here will "
                  + "be blocking this task. Removes all previous blocking tasks.",
              REPEATABLE));

  static final Memoized<CliArguments.TaskOption> ADD_AFTER_OPTION =
      memoize(
          () -> new CliArguments.TaskOption(
              "addafter",
              "aa",
              "Adds another task as blocking this one.",
              REPEATABLE));

  static final Memoized<CliArguments.TaskOption> REMOVE_AFTER_OPTION =
      memoize(
          () -> new CliArguments.TaskOption(
              "rmafter",
              "ra",
              "Removes another task as blocking this one.",
              REPEATABLE));

  static final Memoized<CliArguments.TaskOption> BEFORE_OPTION =
      memoize(
          () -> new CliArguments.TaskOption(
              "before",
              "b",
              "Sets this task as coming before another task. Tasks listed here will "
                  + "be blocked by this task. Removes all previous blocked tasks.",
              REPEATABLE));

  static final Memoized<CliArguments.TaskOption> ADD_BEFORE_OPTION =
      memoize(
          () -> new CliArguments.TaskOption(
              "addbefore",
              "ab",
              "Adds another task as being blocked by this one.",
              REPEATABLE));

  static final Memoized<CliArguments.TaskOption> REMOVE_BEFORE_OPTION =
      memoize(
          () -> new CliArguments.TaskOption(
              "rmbefore",
              "rb",
              "Removes another task as being blocked by this one.",
              REPEATABLE));

  static final Memoized<ImmutableList<CliArguments.Option>> OPTIONS =
      memoize(
          () -> ImmutableList.of(
              DESCRIPTION_OPTION.value(),
              AFTER_OPTION.value(),
              ADD_AFTER_OPTION.value(),
              REMOVE_AFTER_OPTION.value(),
              BEFORE_OPTION.value(),
              ADD_BEFORE_OPTION.value(),
              REMOVE_BEFORE_OPTION.value()));
}
