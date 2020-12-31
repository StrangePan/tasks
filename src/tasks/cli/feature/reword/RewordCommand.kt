package tasks.cli.feature.reword;

import static omnia.data.cache.Memoized.memoize;
import static tasks.cli.command.Parameter.Repeatable.NOT_REPEATABLE;

import omnia.data.cache.Memoized;
import omnia.data.structure.immutable.ImmutableList;
import tasks.cli.command.Command;
import tasks.cli.command.Parameter;
import tasks.cli.command.StringParameter;
import tasks.cli.command.TaskParameter;

/** Canonical definition for the Reword command. */
public final class RewordCommand {
  private RewordCommand() {}

  public static Command registration() {
    return COMMAND.value();
  }

  static final Memoized<ImmutableList<Parameter>> COMMAND_PARAMETERS =
      memoize(
          () ->
              ImmutableList.of(
                  new TaskParameter(NOT_REPEATABLE),
                  new StringParameter("description", NOT_REPEATABLE)));

  private static final Memoized<Command> COMMAND =
      memoize(
          () -> Command.builder()
              .canonicalName("reword")
              .aliases()
              .parameters(COMMAND_PARAMETERS.value())
              .options(ImmutableList.empty())
              .helpDocumentation("Changes the description of a task."));
}
