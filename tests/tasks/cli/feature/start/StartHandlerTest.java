package tasks.cli.feature.start;

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
public final class StartHandlerTest {

  private static final String TASKS_STARTED_HEADER = "task(s) started:";
  private static final String TASKS_ALREADY_STARTED_HEADER = "task(s) already started:";
  private static final String TASKS_BLOCKED_AS_A_RESULT_HEADER = "task(s) blocked as a result:";

  private final ObservableTaskStore taskStore = ObservableTaskStoreImpl.createInMemoryStorage();

  private final StartHandler underTest = new StartHandler(Memoized.just(taskStore));

  @Test
  public void handle_withNoSpecifiedTasks_throwsException() {
    assertThrows(HandlerException.class, () -> underTest.handle(startArgs()));
  }

  @Test
  public void handle_withOpenTask_marksTaskAsStarted() {
    Task task = createTask("example task", b -> b.setStatus(Task.Status.OPEN));

    underTest.handle(startArgs(task)).ignoreElement().blockingAwait();

    assertThat(getUpdatedVersionOf(task).status()).isEqualTo(Task.Status.STARTED);
  }

  @Test
  public void handle_withOpenTask_outputsStartedTask() {
    Task task = createTask("example task", b -> b.setStatus(Task.Status.OPEN));

    String output = underTest.handle(startArgs(task)).blockingGet().toString();

    HandlerTestUtils.assertOutputContainsGroupedTasks(
        output, TASKS_STARTED_HEADER, ImmutableList.of(getUpdatedVersionOf(task)));
    assertThat(output).doesNotContain(TASKS_ALREADY_STARTED_HEADER);
  }

  @Test
  public void handle_withCompletedTask_marksTaskAsStarted() {
    Task task = createTask("example task", b -> b.setStatus(Task.Status.COMPLETED));

    underTest.handle(startArgs(task)).ignoreElement().blockingAwait();

    assertThat(getUpdatedVersionOf(task).status()).isEqualTo(Task.Status.STARTED);
  }

  @Test
  public void handle_withCompletedTask_outputsStartedTask() {
    Task task = createTask("example task", b -> b.setStatus(Task.Status.COMPLETED));

    String output = underTest.handle(startArgs(task)).blockingGet().toString();

    assertOutputContainsGroupedTasks(
        output, TASKS_STARTED_HEADER, ImmutableList.of(getUpdatedVersionOf(task)));
    assertThat(output).doesNotContain(TASKS_ALREADY_STARTED_HEADER);
    assertThat(output).doesNotContain(TASKS_BLOCKED_AS_A_RESULT_HEADER);
  }

  @Test
  public void handle_withStartedTask_taskIsStillStarted() {
    Task task = createTask("example task", b -> b.setStatus(Task.Status.STARTED));

    underTest.handle(startArgs(task)).ignoreElement().blockingAwait();

    assertThat(getUpdatedVersionOf(task).status()).isEqualTo(Task.Status.STARTED);
  }

  @Test
  public void handle_withStartedTask_outputsAlreadyStartedTask() {
    Task task = createTask("example task", b -> b.setStatus(Task.Status.STARTED));

    String output = underTest.handle(startArgs(task)).blockingGet().toString();

    assertOutputContainsGroupedTasks(output, TASKS_ALREADY_STARTED_HEADER, ImmutableList.of(task));
    assertThat(output).doesNotContain(TASKS_STARTED_HEADER);
  }

  @Test
  public void handle_withCompletedTask_withBlockedTask_blocksBlockedTask() {
    Task task = createTask("example task", b -> b.setStatus(Task.Status.COMPLETED));
    Task blockedTask = createTask("blocked task", b -> b.addBlockingTask(task));

    underTest.handle(startArgs(task)).ignoreElement().blockingAwait();

    assertThat(getUpdatedVersionOf(blockedTask).isUnblocked()).isFalse();
  }

  @Test
  public void handle_withCompletedTask_withBlockedTask_outputsBlockedTask() {
    Task task = createTask("example task", b -> b.setStatus(Task.Status.COMPLETED));
    Task blockedTask = createTask("blocked task", b -> b.addBlockingTask(task));

    String output = underTest.handle(startArgs(task)).blockingGet().toString();

    assertOutputContainsGroupedTasks(
        output, TASKS_STARTED_HEADER, ImmutableList.of(getUpdatedVersionOf(task)));
    assertOutputContainsGroupedTasks(
        output,
        TASKS_BLOCKED_AS_A_RESULT_HEADER,
        ImmutableList.of(getUpdatedVersionOf(blockedTask)));
    assertThat(output).doesNotContain(TASKS_ALREADY_STARTED_HEADER);
  }

  @Test
  public void handle_withAllThree_outputsCorrectOutput() {
    Task task = createTask("example task", b -> b.setStatus(Task.Status.COMPLETED));
    Task blockedTask = createTask("blocked task", b -> b.addBlockingTask(task));
    Task startedTask = createTask("open task", b -> b.setStatus(Task.Status.STARTED));

    String output = underTest.handle(startArgs(task, startedTask)).blockingGet().toString();

    assertOutputContainsGroupedTasks(
        output, TASKS_STARTED_HEADER, ImmutableList.of(getUpdatedVersionOf(task)));
    assertOutputContainsGroupedTasks(
        output,
        TASKS_BLOCKED_AS_A_RESULT_HEADER,
        ImmutableList.of(getUpdatedVersionOf(blockedTask)));
    assertOutputContainsGroupedTasks(
        output, TASKS_ALREADY_STARTED_HEADER, ImmutableList.of(startedTask));
    assertThat(output)
        .containsMatch(
            Pattern.compile(
                Pattern.quote(TASKS_ALREADY_STARTED_HEADER) +
                    ".*" +
                    Pattern.quote(TASKS_STARTED_HEADER) +
                    ".*" +
                    Pattern.quote(TASKS_BLOCKED_AS_A_RESULT_HEADER),
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

  private static CommonArguments<StartArguments> startArgs(Task... tasks) {
    return commonArgs(new StartArguments(ImmutableList.copyOf(tasks)));
  }
}