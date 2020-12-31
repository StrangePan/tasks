package tasks.cli.handler.testing;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static omnia.data.stream.Collectors.toImmutableList;

import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import omnia.cli.out.Output;
import omnia.data.structure.Collection;
import omnia.data.structure.immutable.ImmutableList;
import tasks.cli.command.common.CommonArguments;
import tasks.model.ObservableTaskStore;
import tasks.model.Task;
import tasks.model.TaskBuilder;

/**
 * Utilities for {@link tasks.cli.handler.ArgumentHandler} unit tests.
 */
public final class HandlerTestUtils {

  /**
   * Creates a {@link CommonArguments} instance wrapping the provided specific args and setting
   * {@link CommonArguments#enableOutputFormatting()} to {@code false}.
   */
  public static <T> CommonArguments<T> commonArgs(T specificArgs) {
    return new CommonArguments<>(specificArgs, /* enableColorOutput= */ false);
  }

  /**
   * Creates a new task with the provided label in the provided task store. Blocks the calling
   * thread until it returns the created {@link Task}.
   */
  public static Task createTask(ObservableTaskStore taskStore, String label) {
    return createTask(taskStore, label, b -> b);
  }

  /**
   * Creates a new task with the provided label in the provided task store, using the provided task
   * builder function to set up the new task. Blocks the calling thread until it returns the created
   * {@link Task}.
   */
  public static Task createTask(ObservableTaskStore taskStore, String label, Function<TaskBuilder, TaskBuilder> builderFunction) {
    return taskStore.createTask(label, builderFunction).blockingGet().third();
  }

  /**
   * Assert that the given output contains a grouping of the provided tasks with only whitespace
   * between each task.
   */
  public static void assertOutputContainsGroupedTasks(
      String output, Collection<? extends Task> tasks) {
    assertOutputContainsGroupedTasks(output, "", tasks);
  }

  /**
   * Assert that the given output contains a grouping of the provided tasks with only whitespace
   * between each task, and that this grouping is immediately preceded by the given prefix, which
   * again is only separated from the task group by whitespace.
   */
  public static void assertOutputContainsGroupedTasks(
      String output, String prefix, Collection<? extends Task> tasks) {
    if (!tasks.isPopulated()) {
      return; // nothing to assert
    }

    ImmutableList<String> renderedTasks = renderTasks(tasks);

    assertThat(output).contains(prefix);
    renderedTasks.forEach(renderedTask -> assertThat(output).contains(renderedTask));

    assertWithMessage("tasks are not grouped together in the output")
        .that(output)
        .containsMatch(
            Pattern.compile(
                Pattern.quote(prefix) + patternMatchingGroupedSubstrings(renderedTasks),
                Pattern.MULTILINE));
  }

  /**
   * Returns a RegEx-compatible pattern that matches the provided tasks and asserts that each task
   * is separated only be whitespace.
   *
   * <p>This pattern cannot check that each each task exists exactly once, and will match if the
   * same task is repeated multiple times, or if the expected task cluster is immediately preceded
   * or followed by one or more tasks.
   */
  public static String patternMatchingGroupedTasks(Collection<? extends Task> tasks) {
    return patternMatchingGroupedSubstrings(renderTasks(tasks));
  }

  private static ImmutableList<String> renderTasks(Collection<? extends Task> tasks) {
    return tasks.stream()
        .map(Task::render)
        .map(Output::renderWithoutCodes)
        .collect(toImmutableList());
  }

  private static String patternMatchingGroupedSubstrings(Collection<String> substrings) {
    return substrings.isPopulated()
        ? String.format(
            "(\\s*(%s)){%d}",
            substrings.stream().map(Pattern::quote).collect(Collectors.joining("|", "(", ")")),
            substrings.count())
        : "";
  }

  /**
   * Looks up the provided {@link Task} in the provided {@link ObservableTaskStore}, returning the
   * latest immutable version of the task.
   */
  public static Task getUpdatedVersionOf(ObservableTaskStore taskStore, Task task) {
    return taskStore.observe().blockingFirst().lookUpById(task.id()).orElseThrow();
  }
}
