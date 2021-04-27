package me.strangepan.tasks.cli.feature.stop

import omnia.data.cache.Memoized
import omnia.data.cache.Memoized.Companion.memoize
import omnia.data.structure.immutable.ImmutableList
import me.strangepan.tasks.cli.command.Command
import me.strangepan.tasks.cli.command.Parameter
import me.strangepan.tasks.cli.command.TaskParameter

/** Canonical definition for the Stop command.  */
object StopCommand {
  fun registration(): Command {
    return COMMAND.value()
  }

  private val COMMAND: Memoized<Command> = memoize {
    Command.builder()
        .canonicalName("stop")
        .aliases()
        .parameters(ImmutableList.of(TaskParameter(Parameter.Repeatable.REPEATABLE)))
        .options(ImmutableList.empty())
        .helpDocumentation(
            "Mark one or more tasks as open. This is the opposite of the start command. " +
                "Only tasks started with the start command can be stopped.")
  }
}