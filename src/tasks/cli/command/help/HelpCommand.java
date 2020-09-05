package tasks.cli.command.help;

import static tasks.cli.command.Parameter.Repeatable.NOT_REPEATABLE;

import omnia.data.structure.immutable.ImmutableList;
import tasks.cli.command.Command;
import tasks.cli.command.StringParameter;

/** Canonical definition for the Help command. */
public final class HelpCommand {
  private HelpCommand() {}

  public static Command registration() {
    return Command.builder()
        .canonicalName("help")
        .aliases()
        .parameters(ImmutableList.of(new StringParameter("command", NOT_REPEATABLE)))
        .options(ImmutableList.empty())
        .parser(HelpParser::new)
        .helpDocumentation("Retrieve the help documentation for a specific command.");
  }
}
