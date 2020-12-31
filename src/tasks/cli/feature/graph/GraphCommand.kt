package tasks.cli.feature.graph

import omnia.data.cache.Memoized
import omnia.data.cache.Memoized.Companion.memoize
import omnia.data.structure.immutable.ImmutableList
import tasks.cli.command.Command
import tasks.cli.command.FlagOption
import tasks.cli.command.Parameter

object GraphCommand {
  fun registration(): Command {
    return COMMAND.value()
  }

  val ALL_OPTION: Memoized<FlagOption> = memoize {
    FlagOption(
        "all",
        "a",
        "Lists all tasks, including completed tasks. By default, completed tasks are not " +
            "included in the graph.",
        Parameter.Repeatable.NOT_REPEATABLE)
  }
  private val COMMAND: Memoized<Command> = memoize {
    Command.builder()
        .canonicalName("graph")
        .aliases("xl")
        .parameters(ImmutableList.empty())
        .options(ImmutableList.of(ALL_OPTION.value()))
        .helpDocumentation("Prints all tasks in graph format.")
  }
}