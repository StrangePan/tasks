package tasks.cli.feature.info;

import static com.google.common.truth.Truth.assertThat;
import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.MULTILINE;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static tasks.cli.handler.testing.HandlerTestUtils.assertOutputContainsGroupedTasks;
import static tasks.cli.handler.testing.HandlerTestUtils.patternMatchingGroupedTasks;

import java.util.function.Function;
import java.util.regex.Pattern;
import omnia.data.structure.Collection;
import omnia.data.structure.immutable.ImmutableList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import tasks.cli.command.common.CommonArguments;
import tasks.cli.handler.HandlerException;
import tasks.cli.handler.testing.HandlerTestUtils;
import tasks.model.ObservableTaskStore;
import tasks.model.Task;
import tasks.model.TaskBuilder;
import tasks.model.impl.ObservableTaskStoreImpl;

@RunWith(JUnit4.class)
public final class InfoHandlerTest {

  private final ObservableTaskStore taskStore = ObservableTaskStoreImpl.createInMemoryStorage();

  private final InfoHandler underTest = new InfoHandler();

  @Test
  public void handle_noTasks_throwsException() {
    HandlerException exception =
        assertThrows(HandlerException.class, () -> underTest.handle(infoArgs()));

    assertThat(exception.getMessage()).contains("no tasks specified");
  }

  @Test
  public void handle_oneTask() {
    Task task = createTask("test task");

    String output = underTest.handle(infoArgs(task)).blockingGet().renderWithoutCodes();

    assertThat(output).startsWith(task.render().renderWithoutCodes());
    assertThat(output).doesNotContainMatch("complete|completed");
    assertThat(output).doesNotContainMatch("start|started");
  }

  @Test
  public void handle_oneTask_withBlockingTasks() {
    Task blocker0 = createTask("blocker 0");
    Task blocker1 = createTask("blocker 1");
    Task task =
        createTask(
            "test task",
            builder -> builder.addBlockingTask(blocker0).addBlockingTask(blocker1));

    String output = underTest.handle(infoArgs(task)).blockingGet().renderWithoutCodes();

    assertThat(output).startsWith(task.render().renderWithoutCodes());
    assertOutputContainsGroupedTasks(
        output, "tasks blocking this:", ImmutableList.of(blocker0, blocker1));
  }

  @Test
  public void handle_oneTask_withBlockedTasks() {
    Task blocked0 = createTask("blocked 0");
    Task blocked1 = createTask("blocked 1");
    Task task =
        createTask(
            "test task", builder -> builder.addBlockedTask(blocked0).addBlockedTask(blocked1));

    String output = underTest.handle(infoArgs(task)).blockingGet().renderWithoutCodes();

    assertThat(output).startsWith(task.render().renderWithoutCodes());
    assertOutputContainsGroupedTasks(
        output, "tasks blocked by this:", ImmutableList.of(blocked0, blocked1));
  }

  @Test
  public void handle_oneTask_withBlockingAndBlockedTasks() {
    Task blocker0 = createTask("blocker 0");
    Task blocker1 = createTask("blocker 1");
    Task blocked0 = createTask("blocked 0");
    Task blocked1 = createTask("blocked 1");
    Task task =
        createTask(
            "test task",
            builder -> builder
                .addBlockingTask(blocker0)
                .addBlockingTask(blocker1)
                .addBlockedTask(blocked0)
                .addBlockedTask(blocked1));

    String output = underTest.handle(infoArgs(task)).blockingGet().renderWithoutCodes();

    assertThat(output).startsWith(task.render().renderWithoutCodes());
    assertOutputContainsGroupedTasks(
        output, "tasks blocking this:", ImmutableList.of(blocker0, blocker1));
    assertOutputContainsGroupedTasks(
        output, "tasks blocked by this:", ImmutableList.of(blocked0, blocked1));
  }

  private Task createTask(String label) {
    return HandlerTestUtils.createTask(taskStore, label);
  }

  private Task createTask(String label, Function<TaskBuilder, TaskBuilder> builderFunction) {
    return HandlerTestUtils.createTask(taskStore, label, builderFunction);
  }

  private static CommonArguments<InfoArguments> infoArgs(Task... tasks) {
    return HandlerTestUtils.commonArgs(new InfoArguments(ImmutableList.copyOf(tasks)));
  }
}