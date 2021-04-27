package me.strangepan.tasks.cli.feature.blockers

import omnia.data.cache.Memoized
import omnia.data.cache.Memoized.Companion.memoize
import omnia.data.structure.immutable.ImmutableList
import me.strangepan.tasks.cli.command.Command
import me.strangepan.tasks.cli.command.FlagOption
import me.strangepan.tasks.cli.command.Parameter
import me.strangepan.tasks.cli.command.TaskOption
import me.strangepan.tasks.cli.command.TaskParameter

/** Canonical definition for the Blockers command.  */
object BlockersCommand {
  fun registration(): Command {
    return COMMAND.value()
  }

  val ADD_OPTION: Memoized<TaskOption> = memoize {
    TaskOption(
      "add",
      "a",
      "Adds another task as a blocker.",
      Parameter.Repeatable.REPEATABLE
    )
  }
  val CLEAR_OPTION: Memoized<FlagOption> = memoize {
    FlagOption(
      "clear",
      "c",
      "Removes all blocking me.strangepan.tasks.engine.tasks. Can be used together with " +
          "--${ADD_OPTION.value().longName()} to replace existing blockers with new ones.",
      Parameter.Repeatable.NOT_REPEATABLE
    )
  }
  val REMOVE_OPTION: Memoized<TaskOption> = memoize {
    TaskOption(
      "remove",
      "d",
      "Removes another task from being a blocker. Ignored if " +
          "--${CLEAR_OPTION.value().longName()} is set.",
      Parameter.Repeatable.REPEATABLE
    )
  }
  private val OPTIONS = memoize {
    ImmutableList.of(
        ADD_OPTION.value(),
        CLEAR_OPTION.value(),
        REMOVE_OPTION.value())
  }
  private val COMMAND: Memoized<Command> = memoize {
    Command.builder()
      .canonicalName("blockers")
      .aliases("blocker", "bk")
      .parameters(ImmutableList.of(TaskParameter(Parameter.Repeatable.REPEATABLE)))
      .options(OPTIONS.value())
      .helpDocumentation(
        "Modifies or lists blockers of an existing task. Can be used to add or remove "
            + "blockers from a task. If no modifications are specified, simply lists the "
            + "existing blockers for a task."
      )
  }
}