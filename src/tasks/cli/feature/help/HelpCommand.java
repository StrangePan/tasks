package tasks.cli.feature.help;

import static omnia.data.cache.Memoized.memoize;
import static tasks.cli.command.Parameter.Repeatable.NOT_REPEATABLE;

import omnia.data.cache.Memoized;
import omnia.data.structure.immutable.ImmutableList;
import tasks.cli.command.Command;
import tasks.cli.command.StringParameter;

/** Canonical definition for the Help command. */
public final class HelpCommand {
  private HelpCommand() {}

  public static Command registration() {
    return COMMAND.value();
  }

  private static final Memoized<Command> COMMAND =
      memoize(
          () -> Command.builder()
              .canonicalName("help")
              .aliases()
              .parameters(ImmutableList.of(new StringParameter("command", NOT_REPEATABLE)))
              .options(ImmutableList.empty())
              .helpDocumentation("Retrieve the help documentation for a specific command."));
}
