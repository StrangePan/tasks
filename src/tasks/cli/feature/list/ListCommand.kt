package tasks.cli.feature.list

import java.util.function.Supplier
import omnia.data.cache.Memoized
import omnia.data.cache.Memoized.Companion.memoize
import omnia.data.structure.immutable.ImmutableList
import tasks.cli.command.Command
import tasks.cli.command.FlagOption
import tasks.cli.command.Option
import tasks.cli.command.Parameter

/** Canonical definition for the List command.  */
object ListCommand {
  fun registration(): Command {
    return COMMAND.value()
  }

  val BLOCKED_OPTION: Memoized<FlagOption> = memoize(
      Supplier {
        FlagOption(
            "blocked",
            "b", "List all tasks that are uncompleted, but blocked by other tasks. Can "
            + "be combined with other flags.", Parameter.Repeatable.NOT_REPEATABLE)
      })
  val COMPLETED_OPTION: Memoized<FlagOption> = memoize(
      Supplier {
        FlagOption(
            "completed",
            "c", "List all tasks already marked as completed. Can be combined with "
            + "other flags.", Parameter.Repeatable.NOT_REPEATABLE)
      })
  val UNBLOCKED_OPTION: Memoized<FlagOption> = memoize(
      Supplier {
        FlagOption(
            "unblocked",
            "u",
            "List all unblocked tasks. Can be combined with other flags.", Parameter.Repeatable.NOT_REPEATABLE)
      })
  val STARTED_OPTION: Memoized<FlagOption> = memoize(
      Supplier {
        FlagOption(
            "started",
            "s",
            "List all started tasks. Can be combined with other flags.", Parameter.Repeatable.NOT_REPEATABLE)
      })
  val ALL_OPTION: Memoized<FlagOption> = memoize(
      Supplier {
        FlagOption(
            "all",
            "a",
            "Lists all tasks. A shortcut for all other flags put together.", Parameter.Repeatable.NOT_REPEATABLE)
      })

  private val OPTIONS: Memoized<ImmutableList<Option>> = memoize {
    ImmutableList.of(
        BLOCKED_OPTION.value(),
        COMPLETED_OPTION.value(),
        UNBLOCKED_OPTION.value(),
        STARTED_OPTION.value(),
        ALL_OPTION.value())
  }
  private val COMMAND: Memoized<Command> = memoize {
    Command.builder()
        .canonicalName("list")
        .aliases("ls", "l")
        .parameters(ImmutableList.empty())
        .options(OPTIONS.value())
        .helpDocumentation(
            "Prints a list of tasks. By default, only lists uncompleted tasks that are "
                + "unblocked. Can also list only blocked tasks, only completed tasks, any "
                + "combination of the three, or all tasks.")
  }
}