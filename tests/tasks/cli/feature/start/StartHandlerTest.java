package tasks.cli.feature.start;

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
public final class StartHandlerTest {

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
  public void handle_withOpenTask_outputsStartedTask() {
    Task task = createTask("example task", b -> b.setStatus(Task.Status.OPEN));

    String output = underTest.handle(startArgs(task)).blockingGet().toString();

    assertThat(output)
        .containsMatch(Pattern.compile("task\\(s\\) started:.*" + task.label(), DOTALL));
    assertThat(output).doesNotContain("task(s) already started");
  }

  @Test
  public void handle_withCompletedTask_marksTaskAsStarted() {
    Task task = createTask("example task", b -> b.setStatus(Task.Status.COMPLETED));

    underTest.handle(startArgs(task)).ignoreElement().blockingAwait();

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
  public void handle_withCompletedTask_outputsStartedTask() {
    Task task = createTask("example task", b -> b.setStatus(Task.Status.COMPLETED));

    String output = underTest.handle(startArgs(task)).blockingGet().toString();

    assertThat(output)
        .containsMatch(Pattern.compile("task\\(s\\) started:.*" + task.label(), DOTALL));
    assertThat(output).doesNotContain("task(s) already started");
  }

  @Test
  public void handle_withStartedTask_taskIsStillStarted() {
    Task task = createTask("example task", b -> b.setStatus(Task.Status.STARTED));

    underTest.handle(startArgs(task)).ignoreElement().blockingAwait();

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

    String output = underTest.handle(startArgs(task)).blockingGet().toString();

    assertThat(output)
        .containsMatch(Pattern.compile("task\\(s\\) already started:.*" + task.label(), DOTALL));
    assertThat(output).doesNotContain("task(s) started");
  }

  @Test
  public void handle_withCompletedTask_withBlockedTask_blocksBlockedTask() {
    Task task = createTask("example task", b -> b.setStatus(Task.Status.COMPLETED));
    Task blockedTask = createTask("blocked task", b -> b.addBlockingTask(task));

    underTest.handle(startArgs(task)).ignoreElement().blockingAwait();

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

    String output = underTest.handle(startArgs(task)).blockingGet().toString();

    assertThat(output)
        .containsMatch(Pattern.compile("task\\(s\\) started:.*" + task.label(), DOTALL));
    assertThat(output)
        .containsMatch(
            Pattern.compile("task\\(s\\) blocked as a result:.*" + blockedTask.label(), DOTALL));
  }

  @Test
  public void handle_withAllThree_outputsCorrectOutput() {
    Task task = createTask("example task", b -> b.setStatus(Task.Status.COMPLETED));
    Task blockedTask = createTask("blocked task", b -> b.addBlockingTask(task));
    Task startedTask = createTask("open task", b -> b.setStatus(Task.Status.STARTED));

    String output = underTest.handle(startArgs(task, startedTask)).blockingGet().toString();

    assertThat(output)
        .containsMatch(
            Pattern.compile(
                "task\\(s\\) already started:.*" + startedTask.label()
                    + ".*task\\(s\\) started:.*" + task.label()
                    + ".*task\\(s\\) blocked as a result:.*" + blockedTask.label(), DOTALL));
  }

  private Task createTask(String label) {
    return createTask(label, b -> b);
  }

  private Task createTask(String label, Function<TaskBuilder, TaskBuilder> builderFunction) {
    return taskStore.createTask(label, builderFunction).blockingGet().third();
  }

  private static CommonArguments<StartArguments> startArgs(Task... tasks) {
    return commonArgs(new StartArguments(ImmutableList.copyOf(tasks)));
  }

  private static <T> CommonArguments<T> commonArgs(T args) {
    return new CommonArguments<>(args, /* enableColorOutput= */ true);
  }
}