package tasks.cli.feature.reword

import omnia.data.cache.Memoized
import omnia.data.cache.Memoized.Companion.memoize
import omnia.data.structure.immutable.ImmutableList
import tasks.cli.command.Command
import tasks.cli.command.Parameter
import tasks.cli.command.StringParameter
import tasks.cli.command.TaskParameter

/** Canonical definition for the Reword command.  */
object RewordCommand {
  fun registration(): Command {
    return COMMAND.value()
  }

  val COMMAND_PARAMETERS = memoize {
    ImmutableList.of(
        TaskParameter(Parameter.Repeatable.NOT_REPEATABLE),
        StringParameter("description", Parameter.Repeatable.NOT_REPEATABLE))
  }
  private val COMMAND: Memoized<Command> = memoize {
    Command.builder()
        .canonicalName("reword")
        .aliases()
        .parameters(COMMAND_PARAMETERS.value())
        .options(ImmutableList.empty())
        .helpDocumentation("Changes the description of a task.")
  }
}