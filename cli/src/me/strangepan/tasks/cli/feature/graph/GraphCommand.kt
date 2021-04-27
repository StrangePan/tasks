package me.strangepan.tasks.cli.feature.graph

import omnia.data.cache.Memoized
import omnia.data.cache.Memoized.Companion.memoize
import omnia.data.structure.immutable.ImmutableList
import me.strangepan.tasks.cli.command.Command
import me.strangepan.tasks.cli.command.FlagOption
import me.strangepan.tasks.cli.command.Parameter
import me.strangepan.tasks.cli.command.TaskOption

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

  val RELATED_TASKS_OPTION: Memoized<TaskOption> = memoize {
    TaskOption(
      "related",
      "r",
      "Restricts the output to only include tasks that are related to the tasks specified here. " +
          "When defined, the output will only print tasks that come before or after the tasks " +
          "listed here AND any tasks that come before or after those tasks, recursively. Use this " +
          "option to restrict the output and focus your attention.",
      Parameter.Repeatable.REPEATABLE)
  }

  val BEFORE_TASKS_OPTION: Memoized<TaskOption> = memoize {
    TaskOption(
      "before",
      "b",
      "Restricts the output to only include tasks that come before (are blocking) the tasks " +
          "specified here. When defined, the output will only print tasks that come before the " +
          "tasks listed here AND any tasks that come before those tasks recursively. Use this " +
          "option to restrict the output and focus your attention.",
      Parameter.Repeatable.REPEATABLE)
  }

  private val COMMAND: Memoized<Command> = memoize {
    Command.builder()
      .canonicalName("graph")
      .aliases("xl")
      .parameters(ImmutableList.empty())
      .options(
        ImmutableList.of(
          ALL_OPTION.value(), RELATED_TASKS_OPTION.value(), BEFORE_TASKS_OPTION.value()))
      .helpDocumentation("Prints all tasks in graph format.")
  }
}