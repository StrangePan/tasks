package me.strangepan.tasks.cli.feature.start

import omnia.data.cache.Memoized
import omnia.data.cache.Memoized.Companion.memoize
import omnia.data.structure.immutable.ImmutableList
import me.strangepan.tasks.cli.command.Command
import me.strangepan.tasks.cli.command.Parameter
import me.strangepan.tasks.cli.command.TaskParameter

/** Canonical definition for the Start command.  */
object StartCommand {
  fun registration(): Command {
    return COMMAND.value()
  }

  private val COMMAND: Memoized<Command> = memoize {
    Command.builder()
        .canonicalName("start")
        .aliases()
        .parameters(ImmutableList.of(TaskParameter(Parameter.Repeatable.REPEATABLE)))
        .options(ImmutableList.empty())
        .helpDocumentation(
            "Mark one or more me.strangepan.tasks.engine.tasks as started. This can be undone with the stop "
                + "command. Started me.strangepan.tasks.engine.tasks will be highlighted and will appear at the top of "
                + "the list command. Starting a completed task will reopen the task, "
                + "which may cause other me.strangepan.tasks.engine.tasks to become blocked.")
  }
}