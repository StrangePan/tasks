package tasks.cli.feature.info;

import static com.google.common.truth.Truth.assertThat;
import static java.util.regex.Pattern.DOTALL;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.function.Function;
import java.util.regex.Pattern;
import omnia.data.structure.immutable.ImmutableList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import tasks.cli.command.common.CommonArguments;
import tasks.cli.handler.HandlerException;
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
    assertThat(output).contains("test task");
    assertThat(output).doesNotMatch("complete|completed");
    assertThat(output).doesNotMatch("start|started");
    assertThat(output).contains(task.id().toString());
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
    assertThat(output)
        .containsMatch(
            Pattern.compile("test task.*tasks blocking this(.*blocker [01]){2}", DOTALL));
    assertThat(output).contains("blocker 0");
    assertThat(output).contains("blocker 1");
    assertThat(output).contains(task.id().toString());
    assertThat(output).contains(blocker0.id().toString());
    assertThat(output).contains(blocker1.id().toString());
  }

  @Test
  public void handle_oneTask_withBlockedTasks() {
    Task blocked0 = createTask("blocked 0");
    Task blocked1 = createTask("blocked 1");
    Task task =
        createTask(
            "test task", builder -> builder.addBlockedTask(blocked0).addBlockedTask(blocked1));

    String output = underTest.handle(infoArgs(task)).blockingGet().renderWithoutCodes();
    assertThat(output)
        .containsMatch(
            Pattern.compile("test task.*tasks blocked by this(.*blocked [01]){2}", DOTALL));
    assertThat(output).contains("blocked 0");
    assertThat(output).contains("blocked 1");
    assertThat(output).contains(task.id().toString());
    assertThat(output).contains(blocked0.id().toString());
    assertThat(output).contains(blocked1.id().toString());
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
    assertThat(output)
        .containsMatch(
            Pattern.compile(
                "test task.*tasks blocking this(.*blocker [01]){2}" +
                    ".*tasks blocked by this(.*blocked [01]){2}",
                DOTALL));
    assertThat(output).contains("blocker 0");
    assertThat(output).contains("blocker 1");
    assertThat(output).contains("blocked 0");
    assertThat(output).contains("blocked 1");
    assertThat(output).contains(task.id().toString());
    assertThat(output).contains(blocker0.id().toString());
    assertThat(output).contains(blocker1.id().toString());
    assertThat(output).contains(blocked0.id().toString());
    assertThat(output).contains(blocked1.id().toString());
  }

  private Task createTask(String label) {
    return createTask(label, b -> b);
  }

  private Task createTask(String label, Function<TaskBuilder, TaskBuilder> builderFunction) {
    return taskStore.createTask(label, builderFunction).blockingGet().third();
  }

  private static CommonArguments<InfoArguments> infoArgs(Task... tasks) {
    return new CommonArguments<>(
        new InfoArguments(ImmutableList.copyOf(tasks)), /* enableColorOutput= */ true);
  }
}