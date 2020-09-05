package tasks.cli.command.reword;

import static omnia.data.cache.Memoized.memoize;
import static tasks.cli.arg.registration.Parameter.Repeatable.NOT_REPEATABLE;

import omnia.data.cache.Memoized;
import omnia.data.structure.List;
import omnia.data.structure.immutable.ImmutableList;
import tasks.cli.arg.CliArguments;
import tasks.cli.arg.CliMode;
import tasks.cli.arg.CliUtils;
import tasks.cli.arg.registration.CommandRegistration;
import tasks.cli.arg.registration.Parameter;
import tasks.cli.arg.registration.StringParameter;
import tasks.cli.arg.registration.TaskParameter;
import tasks.model.Task;

/** Canonical definition for the Reword command. */
public final class RewordCommand {
  private RewordCommand() {}

  public static CommandRegistration registration(
      Memoized<CliArguments.Parser<? extends List<CliUtils.ParseResult<Task>>>> taskParser) {
    return CommandRegistration.builder()
        .cliMode(CliMode.REWORD)
        .canonicalName("reword")
        .aliases()
        .parameters(COMMAND_PARAMETERS.value())
        .options(ImmutableList.empty())
        .parser(() -> new RewordParser(taskParser))
        .helpDocumentation("Changes the description of a task.");
  }

  static final Memoized<ImmutableList<Parameter>> COMMAND_PARAMETERS =
      memoize(
          () ->
              ImmutableList.of(
                  new TaskParameter(NOT_REPEATABLE),
                  new StringParameter("description", NOT_REPEATABLE)));
}
