package tasks.cli.feature.help

import omnia.data.cache.Memoized
import omnia.data.structure.immutable.ImmutableList
import tasks.cli.command.Command
import tasks.cli.command.Parameter
import tasks.cli.command.StringParameter

/** Canonical definition for the Help command.  */
object HelpCommand {
  fun registration(): Command {
    return COMMAND.value()
  }

  private val COMMAND: Memoized<Command> = Memoized.memoize {
    Command.builder()
        .canonicalName("help")
        .aliases()
        .parameters(ImmutableList.of(StringParameter("command", Parameter.Repeatable.NOT_REPEATABLE)))
        .options(ImmutableList.empty())
        .helpDocumentation("Retrieve the help documentation for a specific command.")
  }
}