package tasks.cli.feature.stop;

import static com.google.common.truth.Truth.assertThat;
import static java.util.regex.Pattern.DOTALL;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static tasks.cli.handler.testing.HandlerTestUtils.assertOutputContainsGroupedTasks;
import static tasks.cli.handler.testing.HandlerTestUtils.commonArgs;

import java.util.function.Function;
import java.util.regex.Pattern;
import omnia.data.cache.Memoized;
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
public final class StopHandlerTest {

  private static final String TASKS_STOPPED_HEADER = "task(s) stopped:";
  public static final String TASKS_ALREADY_STOPPED_HEADER = "task(s) already stopped:";

  private final ObservableTaskStore taskStore = ObservableTaskStoreImpl.createInMemoryStorage();

  private final StopHandler underTest = new StopHandler(Memoized.just(taskStore));

  @Test
  public void handle_withNoSpecifiedTasks_throwsException() {
    assertThrows(HandlerException.class, () -> underTest.handle(startArgs()));
  }

  @Test
  public void handle_withStartedTask_marksTaskAsOpen() {
    Task task = createTask("example task", b -> b.setStatus(Task.Status.STARTED));

    underTest.handle(startArgs(task)).ignoreElement().blockingAwait();

    assertThat(getUpdatedVersionOf(task).status()).isEqualTo(Task.Status.OPEN);
  }

  @Test
  public void handle_withStartedTask_outputsOpenTask() {
    Task task = createTask("example task", b -> b.setStatus(Task.Status.STARTED));

    String output = underTest.handle(startArgs(task)).blockingGet().toString();

    assertOutputContainsGroupedTasks(
        output, TASKS_STOPPED_HEADER, ImmutableList.of(getUpdatedVersionOf(task)));
    assertThat(output).doesNotContain(TASKS_ALREADY_STOPPED_HEADER);
  }

  @Test
  public void handle_withCompletedTask_taskIsStillCompleted() {
    Task task = createTask("example task", b -> b.setStatus(Task.Status.COMPLETED));

    underTest.handle(startArgs(task)).ignoreElement().blockingAwait();

    assertThat(getUpdatedVersionOf(task).status()).isEqualTo(Task.Status.COMPLETED);
  }

  @Test
  public void handle_withCompletedTask_outputsAlreadyCompletedTask() {
    Task task = createTask("example task", b -> b.setStatus(Task.Status.COMPLETED));

    String output = underTest.handle(startArgs(task)).blockingGet().toString();

    assertOutputContainsGroupedTasks(output, TASKS_ALREADY_STOPPED_HEADER, ImmutableList.of(task));
    assertThat(output).doesNotContain(TASKS_STOPPED_HEADER);
  }

  @Test
  public void handle_withOpenTask_taskIsStillOpen() {
    Task task = createTask("example task", b -> b.setStatus(Task.Status.OPEN));

    underTest.handle(startArgs(task)).ignoreElement().blockingAwait();

    assertThat(getUpdatedVersionOf(task).status()).isEqualTo(Task.Status.OPEN);
  }

  @Test
  public void handle_withOpenTask_outputsAlreadyOpenTask() {
    Task task = createTask("example task", b -> b.setStatus(Task.Status.OPEN));

    String output = underTest.handle(startArgs(task)).blockingGet().toString();

    assertOutputContainsGroupedTasks(output, TASKS_ALREADY_STOPPED_HEADER, ImmutableList.of(task));
    assertThat(output).doesNotContain(TASKS_STOPPED_HEADER);
  }

  @Test
  public void handle_withBoth_outputsCorrectOutput() {
    Task startedTask = createTask("started task", b -> b.setStatus(Task.Status.STARTED));
    Task openTask = createTask("open task", b -> b.setStatus(Task.Status.OPEN));
    Task completedTask = createTask("completed task", b -> b.setStatus(Task.Status.COMPLETED));

    String output =
        underTest.handle(startArgs(startedTask, openTask, completedTask)).blockingGet().toString();

    assertOutputContainsGroupedTasks(
        output, TASKS_ALREADY_STOPPED_HEADER, ImmutableList.of(openTask, completedTask));
    assertOutputContainsGroupedTasks(
        output, TASKS_STOPPED_HEADER, ImmutableList.of(getUpdatedVersionOf(startedTask)));

    assertThat(output)
        .containsMatch(
            Pattern.compile(
                Pattern.quote(TASKS_ALREADY_STOPPED_HEADER) +
                    ".*" +
                    Pattern.quote(TASKS_STOPPED_HEADER),
                DOTALL));
  }

  private Task createTask(String label) {
    return HandlerTestUtils.createTask(taskStore, label);
  }

  private Task createTask(String label, Function<TaskBuilder, TaskBuilder> builderFunction) {
    return HandlerTestUtils.createTask(taskStore, label, builderFunction);
  }

  private Task getUpdatedVersionOf(Task task) {
    return HandlerTestUtils.getUpdatedVersionOf(taskStore, task);
  }

  private static CommonArguments<StopArguments> startArgs(Task... tasks) {
    return commonArgs(new StopArguments(ImmutableList.copyOf(tasks)));
  }
}