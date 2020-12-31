package tasks.cli.handler.testing

import com.google.common.truth.Truth
import java.util.function.Consumer
import java.util.function.Function
import java.util.regex.Pattern
import java.util.stream.Collectors
import omnia.cli.out.Output
import omnia.data.stream.Collectors.toImmutableList
import omnia.data.structure.Collection
import omnia.data.structure.immutable.ImmutableList
import tasks.cli.command.common.CommonArguments
import tasks.model.ObservableTaskStore
import tasks.model.Task
import tasks.model.TaskBuilder

/**
 * Utilities for [tasks.cli.handler.ArgumentHandler] unit tests.
 */
object HandlerTestUtils {
  /**
   * Creates a [CommonArguments] instance wrapping the provided specific args and setting
   * [CommonArguments.enableOutputFormatting] to `false`.
   */
  fun <T> commonArgs(specificArgs: T): CommonArguments<T> {
    return CommonArguments(specificArgs,  /* enableColorOutput= */false)
  }
  /**
   * Creates a new task with the provided label in the provided task store, using the provided task
   * builder function to set up the new task. Blocks the calling thread until it returns the created
   * [Task].
   */
  /**
   * Creates a new task with the provided label in the provided task store. Blocks the calling
   * thread until it returns the created [Task].
   */
  @JvmOverloads
  fun createTask(taskStore: ObservableTaskStore, label: String, builderFunction: Function<TaskBuilder, TaskBuilder> = Function { b: TaskBuilder -> b }): Task {
    return taskStore.createTask(label, builderFunction).blockingGet().third()
  }

  /**
   * Assert that the given output contains a grouping of the provided tasks with only whitespace
   * between each task.
   */
  fun assertOutputContainsGroupedTasks(
      output: String, tasks: Collection<out Task>) {
    assertOutputContainsGroupedTasks(output, "", tasks)
  }

  /**
   * Assert that the given output contains a grouping of the provided tasks with only whitespace
   * between each task, and that this grouping is immediately preceded by the given prefix, which
   * again is only separated from the task group by whitespace.
   */
  fun assertOutputContainsGroupedTasks(
      output: String, prefix: String, tasks: Collection<out Task>) {
    if (!tasks.isPopulated) {
      return  // nothing to assert
    }
    val renderedTasks = renderTasks(tasks)
    Truth.assertThat(output).contains(prefix)
    renderedTasks.forEach(Consumer { renderedTask: String -> Truth.assertThat(output).contains(renderedTask) })
    Truth.assertWithMessage("tasks are not grouped together in the output")
        .that(output)
        .containsMatch(
            Pattern.compile(
                Pattern.quote(prefix) + patternMatchingGroupedSubstrings(renderedTasks),
                Pattern.MULTILINE))
  }

  /**
   * Returns a RegEx-compatible pattern that matches the provided tasks and asserts that each task
   * is separated only be whitespace.
   *
   *
   * This pattern cannot check that each each task exists exactly once, and will match if the
   * same task is repeated multiple times, or if the expected task cluster is immediately preceded
   * or followed by one or more tasks.
   */
  fun patternMatchingGroupedTasks(tasks: Collection<out Task>): String {
    return patternMatchingGroupedSubstrings(renderTasks(tasks))
  }

  private fun renderTasks(tasks: Collection<out Task>): ImmutableList<String> {
    return tasks.stream()
        .map { obj: Task -> obj.render() }
        .map { obj: Output -> obj.renderWithoutCodes() }
        .collect(toImmutableList())
  }

  private fun patternMatchingGroupedSubstrings(substrings: Collection<String>): String {
    return if (substrings.isPopulated) String.format(
        "(\\s*(%s)){%d}",
        substrings.stream().map { s: String -> Pattern.quote(s) }.collect(Collectors.joining("|", "(", ")")),
        substrings.count()) else ""
  }

  /**
   * Looks up the provided [Task] in the provided [ObservableTaskStore], returning the
   * latest immutable version of the task.
   */
  fun getUpdatedVersionOf(taskStore: ObservableTaskStore, task: Task): Task {
    return taskStore.observe().blockingFirst().lookUpById(task.id()).orElseThrow()
  }
}