package tasks.cli.feature.complete;

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
public final class CompleteHandlerTest {

  private static final String TASKS_COMPLETED_HEADER = "task(s) completed:";
  private static final String TASKS_ALREADY_COMPLETED_HEADER = "task(s) already completed:";
  private static final String TASKS_UNBLOCKED_HEADER = "task(s) unblocked as a result:";

  private final ObservableTaskStore taskStore = ObservableTaskStoreImpl.createInMemoryStorage();

  private final CompleteHandler underTest = new CompleteHandler(Memoized.just(taskStore));

  @Test
  public void handle_withNoSpecifiedTasks_throwsException() {
    assertThrows(HandlerException.class, () -> underTest.handle(completeArgs()));
  }

  @Test
  public void handle_withOpenTask_marksTaskAsComplete() {
    Task task = createTask("example task", b -> b.setStatus(Task.Status.OPEN));

    underTest.handle(completeArgs(task)).ignoreElement().blockingAwait();

    assertThat(
        taskStore.observe()
            .firstOrError()
            .blockingGet()
            .lookUpById(task.id())
            .orElseThrow()
            .status())
        .isEqualTo(Task.Status.COMPLETED);
  }

  @Test
  public void handle_withOpenTask_outputsCompletedTask() {
    Task task = createTask("example task", b -> b.setStatus(Task.Status.OPEN));

    String output = underTest.handle(completeArgs(task)).blockingGet().toString();

    Task completedTask = HandlerTestUtils.getUpdatedVersionOf(taskStore, task);
    assertOutputContainsGroupedTasks(
        output, TASKS_COMPLETED_HEADER, ImmutableList.of(completedTask));
  }

  @Test
  public void handle_withStartedTask_marksTaskAsComplete() {
    Task task = createTask("example task", b -> b.setStatus(Task.Status.STARTED));

    underTest.handle(completeArgs(task)).ignoreElement().blockingAwait();

    assertThat(
        taskStore.observe()
            .firstOrError()
            .blockingGet()
            .lookUpById(task.id())
            .orElseThrow()
            .status())
        .isEqualTo(Task.Status.COMPLETED);
  }

  @Test
  public void handle_withStartedTask_outputsCompletedTask() {
    Task task = createTask("example task", b -> b.setStatus(Task.Status.STARTED));

    String output = underTest.handle(completeArgs(task)).blockingGet().toString();

    Task completedTask = HandlerTestUtils.getUpdatedVersionOf(taskStore, task);
    assertOutputContainsGroupedTasks(
        output, TASKS_COMPLETED_HEADER, ImmutableList.of(completedTask));
    assertThat(output).doesNotContain(TASKS_ALREADY_COMPLETED_HEADER);
    assertThat(output).doesNotContain(TASKS_UNBLOCKED_HEADER);
  }

  @Test
  public void handle_withCompletedTask_taskIsStillMarkedAsComplete() {
    Task task = createTask("example task", b -> b.setStatus(Task.Status.COMPLETED));

    underTest.handle(completeArgs(task)).ignoreElement().blockingAwait();

    assertThat(
        taskStore.observe()
            .firstOrError()
            .blockingGet()
            .lookUpById(task.id())
            .orElseThrow()
            .status())
        .isEqualTo(Task.Status.COMPLETED);
  }

  @Test
  public void handle_withCompletedTask_outputsAlreadyCompletedTask() {
    Task task = createTask("example task", b -> b.setStatus(Task.Status.COMPLETED));

    String output = underTest.handle(completeArgs(task)).blockingGet().toString();

    assertOutputContainsGroupedTasks(
        output, TASKS_ALREADY_COMPLETED_HEADER, ImmutableList.of(task));
    assertThat(output).doesNotContain(TASKS_COMPLETED_HEADER);
    assertThat(output).doesNotContain(TASKS_UNBLOCKED_HEADER);
  }

  @Test
  public void handle_withOpenTask_withBlockedTask_unblocksBlockedTask() {
    Task task = createTask("example task", b -> b.setStatus(Task.Status.OPEN));
    Task blockedTask = createTask("blocked task", b -> b.addBlockingTask(task));

    underTest.handle(completeArgs(task)).ignoreElement().blockingAwait();

    assertThat(
        taskStore.observe()
            .firstOrError()
            .blockingGet()
            .lookUpById(blockedTask.id())
            .orElseThrow()
            .isUnblocked())
        .isTrue();
  }

  @Test
  public void handle_withOpenTask_withBlockedTask_outputsUnblockedTask() {
    Task task = createTask("example task", b -> b.setStatus(Task.Status.OPEN));
    Task blockedTask = createTask("blocked task", b -> b.addBlockingTask(task));

    String output = underTest.handle(completeArgs(task)).blockingGet().toString();

    Task completedTask = HandlerTestUtils.getUpdatedVersionOf(taskStore, task);
    assertOutputContainsGroupedTasks(
        output, TASKS_COMPLETED_HEADER, ImmutableList.of(completedTask));
    assertOutputContainsGroupedTasks(output, TASKS_UNBLOCKED_HEADER, ImmutableList.of(blockedTask));
    assertThat(output).doesNotContain(TASKS_ALREADY_COMPLETED_HEADER);
  }

  @Test
  public void handle_withOpenTask_withBlockedTasks_andAlreadyCompleted_outputsInCorrectOrder() {
    Task task = createTask("example task", b -> b.setStatus(Task.Status.OPEN));
    Task blockedTask = createTask("blocked task", b -> b.addBlockingTask(task));
    Task alreadyCompletedTask = createTask("completed task", b -> b.setStatus(Task.Status.COMPLETED));

    String output = underTest.handle(completeArgs(task, alreadyCompletedTask)).blockingGet().toString();

    Task newlyCompletedTask = HandlerTestUtils.getUpdatedVersionOf(taskStore, task);
    assertOutputContainsGroupedTasks(
        output, TASKS_ALREADY_COMPLETED_HEADER, ImmutableList.of(alreadyCompletedTask));
    assertOutputContainsGroupedTasks(
        output, TASKS_COMPLETED_HEADER, ImmutableList.of(newlyCompletedTask));
    assertOutputContainsGroupedTasks(
        output, TASKS_UNBLOCKED_HEADER, ImmutableList.of(blockedTask));
    assertThat(output)
        .containsMatch(
            Pattern.compile(
                Pattern.quote(TASKS_ALREADY_COMPLETED_HEADER) +
                    ".*" +
                    Pattern.quote(TASKS_COMPLETED_HEADER) +
                    ".*" +
                    Pattern.quote(TASKS_UNBLOCKED_HEADER),
                DOTALL));
  }

  private Task createTask(String label) {
    return HandlerTestUtils.createTask(taskStore, label);
  }

  private Task createTask(String label, Function<TaskBuilder, TaskBuilder> builderFunction) {
    return HandlerTestUtils.createTask(taskStore, label, builderFunction);
  }

  private static CommonArguments<CompleteArguments> completeArgs(Task... tasks) {
    return commonArgs(new CompleteArguments(ImmutableList.copyOf(tasks)));
  }
}