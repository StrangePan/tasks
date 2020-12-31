package tasks.cli.feature.reopen

import omnia.data.cache.Memoized
import omnia.data.structure.immutable.ImmutableList
import tasks.cli.command.Command
import tasks.cli.command.Parameter
import tasks.cli.command.TaskParameter

/** Canonical definition for the Reopen command.  */
object ReopenCommand {
  fun registration(): Command {
    return COMMAND.value()
  }

  private val COMMAND: Memoized<Command> = Memoized.memoize {
    Command.builder()
        .canonicalName("reopen")
        .aliases()
        .parameters(ImmutableList.of(TaskParameter(Parameter.Repeatable.REPEATABLE)))
        .options(ImmutableList.empty())
        .helpDocumentation("Reopens one or more completed tasks. This can be undone with the complete "
            + "command.")
  }
}