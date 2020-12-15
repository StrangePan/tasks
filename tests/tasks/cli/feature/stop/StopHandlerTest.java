package tasks.cli.feature.stop;

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
public final class StopHandlerTest {

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
  public void handle_withStartedTask_outputsOpenTask() {
    Task task = createTask("example task", b -> b.setStatus(Task.Status.STARTED));

    String output = underTest.handle(startArgs(task)).blockingGet().toString();

    assertThat(output)
        .containsMatch(Pattern.compile("task\\(s\\) stopped:.*" + task.label(), DOTALL));
    assertThat(output).doesNotContain("task(s) already stopped");
  }

  @Test
  public void handle_withCompletedTask_taskIsStillCompleted() {
    Task task = createTask("example task", b -> b.setStatus(Task.Status.COMPLETED));

    underTest.handle(startArgs(task)).ignoreElement().blockingAwait();

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

    String output = underTest.handle(startArgs(task)).blockingGet().toString();

    assertThat(output)
        .containsMatch(Pattern.compile("task\\(s\\) already stopped:.*" + task.label(), DOTALL));
    assertThat(output).doesNotContain("task(s) stopped");
  }

  @Test
  public void handle_withOpenTask_taskIsStillOpen() {
    Task task = createTask("example task", b -> b.setStatus(Task.Status.OPEN));

    underTest.handle(startArgs(task)).ignoreElement().blockingAwait();

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

    String output = underTest.handle(startArgs(task)).blockingGet().toString();

    assertThat(output)
        .containsMatch(Pattern.compile("task\\(s\\) already stopped:.*" + task.label(), DOTALL));
    assertThat(output).doesNotContain("task(s) stopped");
  }

  @Test
  public void handle_withBoth_outputsCorrectOutput() {
    Task startedTask = createTask("started task", b -> b.setStatus(Task.Status.STARTED));
    Task openTask = createTask("open task", b -> b.setStatus(Task.Status.OPEN));
    Task completedTask = createTask("completed task", b -> b.setStatus(Task.Status.COMPLETED));

    String output =
        underTest.handle(startArgs(startedTask, openTask, completedTask)).blockingGet().toString();

    assertThat(output)
        .containsMatch(
            Pattern.compile(
                "task\\(s\\) already stopped:(.*(open|completed) task){2}"
                    + ".*task\\(s\\) stopped:.*started task", DOTALL));
  }

  private Task createTask(String label) {
    return createTask(label, b -> b);
  }

  private Task createTask(String label, Function<TaskBuilder, TaskBuilder> builderFunction) {
    return taskStore.createTask(label, builderFunction).blockingGet().third();
  }

  private static CommonArguments<StopArguments> startArgs(Task... tasks) {
    return commonArgs(new StopArguments(ImmutableList.copyOf(tasks)));
  }

  private static <T> CommonArguments<T> commonArgs(T args) {
    return new CommonArguments<>(args, /* enableColorOutput= */ true);
  }
}