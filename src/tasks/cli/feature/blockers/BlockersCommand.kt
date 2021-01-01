package tasks.cli.feature.blockers

import java.util.function.Supplier
import omnia.data.cache.Memoized
import omnia.data.cache.Memoized.Companion.memoize
import omnia.data.structure.immutable.ImmutableList
import tasks.cli.command.Command
import tasks.cli.command.FlagOption
import tasks.cli.command.Parameter
import tasks.cli.command.TaskOption
import tasks.cli.command.TaskParameter

/** Canonical definition for the Blockers command.  */
object BlockersCommand {
  fun registration(): Command {
    return COMMAND.value()
  }

  val ADD_OPTION: Memoized<TaskOption> = memoize(
      Supplier {
        TaskOption(
            "add",
            "a",
            "Adds another task as a blocker.",
            Parameter.Repeatable.REPEATABLE)
      })
  val CLEAR_OPTION: Memoized<FlagOption> = memoize(
      Supplier {
        FlagOption(
            "clear",
            "c",
            "Removes all blocking tasks. Can be used together with " +
                "--${ADD_OPTION.value().longName()} to replace existing blockers with new ones.",
            Parameter.Repeatable.NOT_REPEATABLE)
      })
  val REMOVE_OPTION: Memoized<TaskOption> = memoize(
      Supplier {
        TaskOption(
            "remove",
            "d",
            "Removes another task from being a blocker. Ignored if " +
                "--${CLEAR_OPTION.value().longName()} is set.",
            Parameter.Repeatable.REPEATABLE)
      })
  private val OPTIONS = memoize {
    ImmutableList.of(
        ADD_OPTION.value(),
        CLEAR_OPTION.value(),
        REMOVE_OPTION.value())
  }
  private val COMMAND: Memoized<Command> = memoize(
      Supplier {
        Command.builder()
            .canonicalName("blockers")
            .aliases("blocker", "bk")
            .parameters(ImmutableList.of(TaskParameter(Parameter.Repeatable.REPEATABLE)))
            .options(OPTIONS.value())
            .helpDocumentation(
                "Modifies or lists blockers of an existing task. Can be used to add or remove "
                    + "blockers from a task. If no modifications are specified, simply lists the "
                    + "existing blockers for a task.")
      })
}