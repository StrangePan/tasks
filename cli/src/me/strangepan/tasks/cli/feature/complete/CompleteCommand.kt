package me.strangepan.tasks.cli.feature.complete

import omnia.data.cache.Memoized
import omnia.data.cache.Memoized.Companion.memoize
import omnia.data.structure.immutable.ImmutableList
import me.strangepan.tasks.cli.command.Command
import me.strangepan.tasks.cli.command.Parameter
import me.strangepan.tasks.cli.command.TaskParameter

/** Canonical definition for the Complete command.  */
object CompleteCommand {
  fun registration(): Command {
    return COMMAND.value()
  }

  private val COMMAND: Memoized<Command> = memoize {
    Command.builder()
        .canonicalName("complete")
        .aliases("finish")
        .parameters(ImmutableList.of(TaskParameter(Parameter.Repeatable.REPEATABLE)))
        .options(ImmutableList.empty())
        .helpDocumentation(
            "Mark one or more tasks as complete. This can be undone with the reopen "
                + "command. When a task is completed, other tasks it was blocking may "
                + "become unblocked.")
  }
}