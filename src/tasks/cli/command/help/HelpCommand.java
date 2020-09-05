package tasks.cli.command.help;

import static tasks.cli.arg.registration.Parameter.Repeatable.NOT_REPEATABLE;

import omnia.data.cache.Memoized;
import omnia.data.structure.Set;
import omnia.data.structure.immutable.ImmutableList;
import tasks.cli.arg.CliMode;
import tasks.cli.arg.registration.CommandRegistration;
import tasks.cli.arg.registration.StringParameter;

/** Canonical definition for the Help command. */
public final class HelpCommand {
  private HelpCommand() {}

  public static CommandRegistration registration(Memoized<Set<String>> validModes) {
    return CommandRegistration.builder()
        .cliMode(CliMode.HELP)
        .canonicalName("help")
        .aliases()
        .parameters(ImmutableList.of(new StringParameter("command", NOT_REPEATABLE)))
        .options(ImmutableList.empty())
        .parser(() -> new HelpParser(validModes))
        .helpDocumentation("Retrieve the help documentation for a specific command.");
  }
}
