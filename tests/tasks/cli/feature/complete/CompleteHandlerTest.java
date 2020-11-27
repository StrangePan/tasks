package tasks.cli.feature.complete;

import static com.google.common.truth.Truth.assertThat;
import static java.util.regex.Pattern.DOTALL;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.function.Function;
import java.util.regex.Pattern;
import omnia.data.cache.Memoized;
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
public final class CompleteHandlerTest {

  private final ObservableTaskStore taskStore = ObservableTaskStoreImpl.createInMemoryStorage();

  private final CompleteHandler underTest = new CompleteHandler(Memoized.just(taskStore));

  @Test
  public void handle_withNoSpecifiedTasks_throwsException() {
    assertThrows(
        HandlerException.class,
        () -> underTest.handle(commonArgs(new CompleteArguments(ImmutableList.empty()))));
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

    assertThat(output)
        .containsMatch(Pattern.compile("task\\(s\\) completed:.*" + task.label(), DOTALL));
    assertThat(output).doesNotContain("task(s) already completed");
    assertThat(output).doesNotContain("task(s) unblocked as a result");
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

    assertThat(output)
        .containsMatch(Pattern.compile("task\\(s\\) completed:.*" + task.label(), DOTALL));
    assertThat(output).doesNotContain("task(s) already completed");
    assertThat(output).doesNotContain("task(s) unblocked as a result");
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

    assertThat(output)
        .containsMatch(Pattern.compile("task\\(s\\) already completed:.*" + task.label(), DOTALL));
    assertThat(output).doesNotContain("task(s) completed");
    assertThat(output).doesNotContain("task(s) unblocked as a result");
  }

  @Test
  public void handle_withOpenTask_withBlockee_unblocksBlockedTask() {
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
  public void handle_withOpenTask_withBlockee_outputsUnblockedTask() {
    Task task = createTask("example task", b -> b.setStatus(Task.Status.OPEN));
    Task blockedTask = createTask("blocked task", b -> b.addBlockingTask(task));

    String output = underTest.handle(completeArgs(task)).blockingGet().toString();

    assertThat(output)
        .containsMatch(Pattern.compile("task\\(s\\) completed:.*" + task.label(), DOTALL));
    assertThat(output)
        .containsMatch(
            Pattern.compile("task\\(s\\) unblocked as a result:.*" + blockedTask.label(), DOTALL));
    assertThat(output).doesNotContain("task(s) already completed");
  }

  @Test
  public void handle_withOpenTask_withBlockee_andAlreadyCompleted_outputsInCorrectOrder() {
    Task task = createTask("example task", b -> b.setStatus(Task.Status.OPEN));
    Task blockedTask = createTask("blocked task", b -> b.addBlockingTask(task));
    Task completedTask = createTask("completed task", b -> b.setStatus(Task.Status.COMPLETED));

    String output = underTest.handle(completeArgs(task, completedTask)).blockingGet().toString();

    assertThat(output)
        .containsMatch(Pattern.compile("task\\(s\\) completed:.*" + task.label(), DOTALL));
    assertThat(output)
        .containsMatch(
            Pattern.compile("task\\(s\\) unblocked as a result:.*" + blockedTask.label(), DOTALL));
    assertThat(output)
        .containsMatch(
            Pattern.compile("task\\(s\\) already completed:.*" + completedTask.label(), DOTALL));
    assertThat(output)
        .containsMatch(
            Pattern.compile(
                "task\\(s\\) already completed:.*" +
                    "task\\(s\\) completed:.*" +
                    "task\\(s\\) unblocked as a result:.*",
                DOTALL));
  }

  private Task createTask(String label) {
    return createTask(label, b -> b);
  }

  private Task createTask(String label, Function<TaskBuilder, TaskBuilder> builderFunction) {
    return taskStore.createTask(label, builderFunction).blockingGet().third();
  }

  private static CommonArguments<CompleteArguments> completeArgs(Task... tasks) {
    return commonArgs(new CompleteArguments(ImmutableList.copyOf(tasks)));
  }

  private static <T> CommonArguments<T> commonArgs(T args) {
    return new CommonArguments<>(args, /* enableColorOutput= */ true);
  }
}