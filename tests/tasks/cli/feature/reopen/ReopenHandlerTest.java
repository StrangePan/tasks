package tasks.cli.feature.reopen;

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
public final class ReopenHandlerTest {

  private static final String TASKS_REOPENED_HEADER = "task(s) reopened:";
  private static final String TASKS_ALREADY_OPEN_HEADER = "task(s) already open:";
  private static final String TASKS_BLOCKED_AS_A_RESULT = "task(s) blocked as a result:";

  private final ObservableTaskStore taskStore = ObservableTaskStoreImpl.createInMemoryStorage();

  private final ReopenHandler underTest = new ReopenHandler(Memoized.just(taskStore));

  @Test
  public void handle_withNoSpecifiedTasks_throwsException() {
    assertThrows(HandlerException.class, () -> underTest.handle(reopenArgs()));
  }

  @Test
  public void handle_withCompletedTask_marksTaskAsOpen() {
    Task task = createTask("example task", b -> b.setStatus(Task.Status.COMPLETED));

    underTest.handle(reopenArgs(task)).ignoreElement().blockingAwait();

    assertThat(
        taskStore.observe()
            .firstOrError()
            .blockingGet()
            .lookUpById(task.id())
            .orElseThrow()
            .status())
        .isEqualTo(Task.Status.OPEN);
  }

  @Test
  public void handle_withCompletedTask_outputsOpenedTask() {
    Task task = createTask("example task", b -> b.setStatus(Task.Status.COMPLETED));

    String output = underTest.handle(reopenArgs(task)).blockingGet().toString();

    assertOutputContainsGroupedTasks(
        output, TASKS_REOPENED_HEADER, ImmutableList.of(getUpdatedVersionOf(task)));
    assertThat(output).doesNotContain(TASKS_ALREADY_OPEN_HEADER);
    assertThat(output).doesNotContain(TASKS_BLOCKED_AS_A_RESULT);
  }

  @Test
  public void handle_withOpenTask_taskIsStillOpen() {
    Task task = createTask("example task", b -> b.setStatus(Task.Status.OPEN));

    underTest.handle(reopenArgs(task)).ignoreElement().blockingAwait();

    assertThat(
        taskStore.observe()
            .firstOrError()
            .blockingGet()
            .lookUpById(task.id())
            .orElseThrow()
            .status())
        .isEqualTo(Task.Status.OPEN);
  }

  @Test
  public void handle_withOpenTask_outputsAlreadyOpenTask() {
    Task task = createTask("example task", b -> b.setStatus(Task.Status.OPEN));

    String output = underTest.handle(reopenArgs(task)).blockingGet().toString();

    assertOutputContainsGroupedTasks(output, TASKS_ALREADY_OPEN_HEADER, ImmutableList.of(task));
    assertThat(output).doesNotContain(TASKS_REOPENED_HEADER);
    assertThat(output).doesNotContain(TASKS_BLOCKED_AS_A_RESULT);
  }

  @Test
  public void handle_withStartedTask_taskIsStillStarted() {
    Task task = createTask("example task", b -> b.setStatus(Task.Status.STARTED));

    underTest.handle(reopenArgs(task)).ignoreElement().blockingAwait();

    assertThat(
        taskStore.observe()
            .firstOrError()
            .blockingGet()
            .lookUpById(task.id())
            .orElseThrow()
            .status())
        .isEqualTo(Task.Status.STARTED);
  }

  @Test
  public void handle_withStartedTask_outputsAlreadyStartedTask() {
    Task task = createTask("example task", b -> b.setStatus(Task.Status.STARTED));

    String output = underTest.handle(reopenArgs(task)).blockingGet().toString();

    assertOutputContainsGroupedTasks(output, TASKS_ALREADY_OPEN_HEADER, ImmutableList.of(task));
    assertThat(output).doesNotContain(TASKS_REOPENED_HEADER);
    assertThat(output).doesNotContain(TASKS_BLOCKED_AS_A_RESULT);
  }

  @Test
  public void handle_withCompletedTask_withBlockedTask_blocksBlockedTask() {
    Task task = createTask("example task", b -> b.setStatus(Task.Status.COMPLETED));
    Task blockedTask = createTask("blocked task", b -> b.addBlockingTask(task));

    underTest.handle(reopenArgs(task)).ignoreElement().blockingAwait();

    assertThat(
        taskStore.observe()
            .firstOrError()
            .blockingGet()
            .lookUpById(blockedTask.id())
            .orElseThrow()
            .isUnblocked())
        .isFalse();
  }

  @Test
  public void handle_withCompletedTask_withBlockedTask_outputsBlockedTask() {
    Task task = createTask("example task", b -> b.setStatus(Task.Status.COMPLETED));
    Task blockedTask = createTask("blocked task", b -> b.addBlockingTask(task));

    String output = underTest.handle(reopenArgs(task)).blockingGet().toString();

    assertOutputContainsGroupedTasks(
        output, TASKS_REOPENED_HEADER, ImmutableList.of(getUpdatedVersionOf(task)));
    assertOutputContainsGroupedTasks(
        output, TASKS_BLOCKED_AS_A_RESULT, ImmutableList.of(blockedTask));
    assertThat(output).doesNotContain(TASKS_ALREADY_OPEN_HEADER);
  }

  @Test
  public void handle_withAllThree_outputsCorrectOutput() {
    Task task = createTask("example task", b -> b.setStatus(Task.Status.COMPLETED));
    Task blockedTask = createTask("blocked task", b -> b.addBlockingTask(task));
    Task openTask = createTask("open task", b -> b.setStatus(Task.Status.OPEN));

    String output = underTest.handle(reopenArgs(task, openTask)).blockingGet().toString();

    assertOutputContainsGroupedTasks(output, TASKS_ALREADY_OPEN_HEADER, ImmutableList.of(openTask));
    assertOutputContainsGroupedTasks(
        output, TASKS_REOPENED_HEADER, ImmutableList.of(getUpdatedVersionOf(task)));
    assertOutputContainsGroupedTasks(
        output, TASKS_BLOCKED_AS_A_RESULT, ImmutableList.of(blockedTask));

    assertThat(output)
        .containsMatch(
            Pattern.compile(
                Pattern.quote(TASKS_ALREADY_OPEN_HEADER) +
                    ".*" +
                    Pattern.quote(TASKS_REOPENED_HEADER) +
                    ".*" +
                    Pattern.quote(TASKS_BLOCKED_AS_A_RESULT),
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

  private static CommonArguments<ReopenArguments> reopenArgs(Task... tasks) {
    return commonArgs(new ReopenArguments(ImmutableList.copyOf(tasks)));
  }
}