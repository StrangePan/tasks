package me.strangepan.tasks.cli.feature.list

import java.util.function.Supplier
import omnia.data.cache.Memoized
import omnia.data.cache.Memoized.Companion.memoize
import omnia.data.structure.immutable.ImmutableList
import me.strangepan.tasks.cli.command.Command
import me.strangepan.tasks.cli.command.FlagOption
import me.strangepan.tasks.cli.command.Option
import me.strangepan.tasks.cli.command.Parameter

/** Canonical definition for the List command.  */
object ListCommand {
  fun registration(): Command {
    return COMMAND.value()
  }

  val BLOCKED_OPTION: Memoized<FlagOption> = memoize(
      Supplier {
        FlagOption(
            "blocked",
            "b", "List all me.strangepan.tasks.engine.tasks that are uncompleted, but blocked by other me.strangepan.tasks.engine.tasks. Can "
            + "be combined with other flags.", Parameter.Repeatable.NOT_REPEATABLE)
      })
  val COMPLETED_OPTION: Memoized<FlagOption> = memoize(
      Supplier {
        FlagOption(
            "completed",
            "c", "List all me.strangepan.tasks.engine.tasks already marked as completed. Can be combined with "
            + "other flags.", Parameter.Repeatable.NOT_REPEATABLE)
      })
  val UNBLOCKED_OPTION: Memoized<FlagOption> = memoize(
      Supplier {
        FlagOption(
            "unblocked",
            "u",
            "List all unblocked me.strangepan.tasks.engine.tasks. Can be combined with other flags.", Parameter.Repeatable.NOT_REPEATABLE)
      })
  val STARTED_OPTION: Memoized<FlagOption> = memoize(
      Supplier {
        FlagOption(
            "started",
            "s",
            "List all started me.strangepan.tasks.engine.tasks. Can be combined with other flags.", Parameter.Repeatable.NOT_REPEATABLE)
      })
  val ALL_OPTION: Memoized<FlagOption> = memoize(
      Supplier {
        FlagOption(
            "all",
            "a",
            "Lists all me.strangepan.tasks.engine.tasks. A shortcut for all other flags put together.", Parameter.Repeatable.NOT_REPEATABLE)
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
            "Prints a list of me.strangepan.tasks.engine.tasks. By default, only lists uncompleted me.strangepan.tasks.engine.tasks that are "
                + "unblocked. Can also list only blocked me.strangepan.tasks.engine.tasks, only completed me.strangepan.tasks.engine.tasks, any "
                + "combination of the three, or all me.strangepan.tasks.engine.tasks.")
  }
}