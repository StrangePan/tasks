package me.strangepan.tasks.cli.feature.info

import omnia.data.cache.Memoized
import omnia.data.cache.Memoized.Companion.memoize
import omnia.data.structure.immutable.ImmutableList
import me.strangepan.tasks.cli.command.Command
import me.strangepan.tasks.cli.command.Parameter
import me.strangepan.tasks.cli.command.TaskParameter

/** Canonical definition for the Info command.  */
object InfoCommand {
  fun registration(): Command {
    return COMMAND.value()
  }

  private val COMMAND: Memoized<Command> = memoize {
    Command.builder()
        .canonicalName("info")
        .aliases("i")
        .parameters(ImmutableList.of(TaskParameter(Parameter.Repeatable.REPEATABLE)))
        .options(ImmutableList.empty())
        .helpDocumentation("Prints all known information about a particular task, including its "
            + "description, all tasks blocking it, and all tasks it is blocking.")
  }
}