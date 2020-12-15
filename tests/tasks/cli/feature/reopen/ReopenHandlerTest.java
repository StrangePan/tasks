package tasks.cli.feature.reopen;

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
public final class ReopenHandlerTest {

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

    assertThat(output)
        .containsMatch(Pattern.compile("task\\(s\\) reopened:.*" + task.label(), DOTALL));
    assertThat(output).doesNotContain("task(s) already open");
    assertThat(output).doesNotContain("task(s) blocked as a result");
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

    assertThat(output)
        .containsMatch(Pattern.compile("task\\(s\\) already open:.*" + task.label(), DOTALL));
    assertThat(output).doesNotContain("task(s) reopened");
    assertThat(output).doesNotContain("task(s) blocked as a result");
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

    assertThat(output)
        .containsMatch(Pattern.compile("task\\(s\\) already open:.*" + task.label(), DOTALL));
    assertThat(output).doesNotContain("task(s) reopened");
    assertThat(output).doesNotContain("task(s) blocked as a result");
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

    assertThat(output)
        .containsMatch(Pattern.compile("task\\(s\\) reopened:.*" + task.label(), DOTALL));
    assertThat(output)
        .containsMatch(
            Pattern.compile("task\\(s\\) blocked as a result:.*" + blockedTask.label(), DOTALL));
    assertThat(output).doesNotContain("task(s) already open");
  }

  @Test
  public void handle_withAllThree_outputsCorrectOutput() {
    Task task = createTask("example task", b -> b.setStatus(Task.Status.COMPLETED));
    Task blockedTask = createTask("blocked task", b -> b.addBlockingTask(task));
    Task openTask = createTask("open task", b -> b.setStatus(Task.Status.OPEN));

    String output = underTest.handle(reopenArgs(task, openTask)).blockingGet().toString();

    assertThat(output)
        .containsMatch(
            Pattern.compile(
                "task\\(s\\) already open:.*" + openTask.label()
                    + ".*task\\(s\\) reopened:.*" + task.label()
                    + ".*task\\(s\\) blocked as a result:.*" + blockedTask.label(), DOTALL));
  }

  private Task createTask(String label) {
    return createTask(label, b -> b);
  }

  private Task createTask(String label, Function<TaskBuilder, TaskBuilder> builderFunction) {
    return taskStore.createTask(label, builderFunction).blockingGet().third();
  }

  private static CommonArguments<ReopenArguments> reopenArgs(Task... tasks) {
    return commonArgs(new ReopenArguments(ImmutableList.copyOf(tasks)));
  }

  private static <T> CommonArguments<T> commonArgs(T args) {
    return new CommonArguments<>(args, /* enableColorOutput= */ true);
  }
}