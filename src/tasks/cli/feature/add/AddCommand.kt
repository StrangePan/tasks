package tasks.cli.feature.add

import java.util.function.Supplier
import omnia.data.cache.Memoized
import omnia.data.cache.Memoized.Companion.memoize
import omnia.data.structure.immutable.ImmutableList
import tasks.cli.command.Command
import tasks.cli.command.Option
import tasks.cli.command.Parameter
import tasks.cli.command.StringParameter
import tasks.cli.command.TaskOption

/** Canonical definition for the Add command.  */
object AddCommand {
  fun registration(): Command {
    return COMMAND.value()
  }

  val COMMAND_PARAMETERS: Memoized<ImmutableList<Parameter>> = memoize { ImmutableList.of(StringParameter("description", Parameter.Repeatable.NOT_REPEATABLE)) }
  val AFTER_OPTION: Memoized<TaskOption> = memoize(
      Supplier {
        TaskOption(
            "after",
            "a",
            "The tasks this one comes after. Tasks listed here will be blocking this task.", Parameter.Repeatable.REPEATABLE)
      })
  val BEFORE_OPTION: Memoized<TaskOption> = memoize(
      Supplier {
        TaskOption(
            "before",
            "b",
            "The tasks this one comes before. Tasks listed here will be blocked by this task.", Parameter.Repeatable.REPEATABLE)
      })

  private val OPTIONS: Memoized<ImmutableList<Option>> = memoize { ImmutableList.of(AFTER_OPTION.value(), BEFORE_OPTION.value()) }
  private val COMMAND: Memoized<Command> = memoize(
      Supplier {
        Command.builder()
            .canonicalName("add")
            .aliases()
            .parameters(COMMAND_PARAMETERS.value())
            .options(OPTIONS.value())
            .helpDocumentation("Creates a new task.")
      })
}