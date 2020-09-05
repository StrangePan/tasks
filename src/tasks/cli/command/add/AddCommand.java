package tasks.cli.command.add;

import static omnia.data.cache.Memoized.memoize;
import static tasks.cli.arg.registration.Parameter.Repeatable.NOT_REPEATABLE;
import static tasks.cli.arg.registration.Parameter.Repeatable.REPEATABLE;

import omnia.data.cache.Memoized;
import omnia.data.structure.List;
import omnia.data.structure.immutable.ImmutableList;
import tasks.cli.arg.CliArguments;
import tasks.cli.arg.CliMode;
import tasks.cli.arg.CliUtils;
import tasks.cli.arg.registration.CommandRegistration;
import tasks.cli.arg.registration.Option;
import tasks.cli.arg.registration.Parameter;
import tasks.cli.arg.registration.StringParameter;
import tasks.cli.arg.registration.TaskOption;
import tasks.model.Task;

/** Canonical definition for the Add command. */
public final class AddCommand {
  private AddCommand() {}

  public static CommandRegistration registration(
      Memoized<CliArguments.Parser<? extends List<CliUtils.ParseResult<Task>>>> taskParser) {
    return CommandRegistration.builder()
        .cliMode(CliMode.ADD)
        .canonicalName("add")
        .aliases()
        .parameters(COMMAND_PARAMETERS.value())
        .options(OPTIONS.value())
        .parser(() -> new AddParser(taskParser))
        .helpDocumentation("Creates a new task.");
  }

  static final Memoized<ImmutableList<Parameter>> COMMAND_PARAMETERS =
      memoize(
          () -> ImmutableList.of(new StringParameter("description", NOT_REPEATABLE)));

  static final Memoized<TaskOption> AFTER_OPTION =
      memoize(
          () -> new TaskOption(
              "after",
              "a",
              "The tasks this one comes after. Tasks listed here will be blocking this task.",
              REPEATABLE));

  static final Memoized<TaskOption> BEFORE_OPTION =
      memoize(
          () -> new TaskOption(
              "before",
              "b",
              "The tasks this one comes before. Tasks listed here will be blocked by this task.",
              REPEATABLE));

  static final Memoized<ImmutableList<Option>> OPTIONS =
      memoize(() -> ImmutableList.of(AFTER_OPTION.value(), BEFORE_OPTION.value()));
}
