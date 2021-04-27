package me.strangepan.tasks.cli.feature.remove

import java.util.function.Supplier
import omnia.data.cache.Memoized
import omnia.data.cache.Memoized.Companion.memoize
import omnia.data.structure.immutable.ImmutableList
import me.strangepan.tasks.cli.command.Command
import me.strangepan.tasks.cli.command.FlagOption
import me.strangepan.tasks.cli.command.Parameter
import me.strangepan.tasks.cli.command.TaskParameter

/** Canonical definition for the Remove command.  */
object RemoveCommand {
  fun registration(): Command {
    return COMMAND.value()
  }

  val FORCE_OPTION: Memoized<FlagOption> = memoize(
      Supplier {
        FlagOption(
            "force",
            "f",
            "Force. Automatically confirm all deletions, skipping confirmations.", Parameter.Repeatable.NOT_REPEATABLE)
      })
  private val COMMAND: Memoized<Command> = memoize {
    Command.builder()
        .canonicalName("remove")
        .aliases("rm")
        .parameters(ImmutableList.of(TaskParameter(Parameter.Repeatable.REPEATABLE)))
        .options(ImmutableList.of(FORCE_OPTION.value()))
        .helpDocumentation(
            "Completely deletes a task. THIS CANNOT BE UNDONE. It is recommended that "
                + "tasks be marked as completed rather than deleted, or amended if their "
                + "content needs to change.")
  }
}