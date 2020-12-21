package tasks.cli.feature.remove;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static java.util.regex.Pattern.MULTILINE;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static tasks.cli.handler.testing.HandlerTestUtils.assertOutputContainsGroupedTasks;
import static tasks.cli.handler.testing.HandlerTestUtils.commonArgs;

import com.google.common.truth.OptionalSubject;
import io.reactivex.observers.TestObserver;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import omnia.cli.out.Output;
import omnia.data.cache.Memoized;
import omnia.data.structure.immutable.ImmutableList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import tasks.cli.command.common.CommonArguments;
import tasks.cli.handler.HandlerException;
import tasks.cli.handler.testing.HandlerTestUtils;
import tasks.cli.input.TestReader;
import tasks.cli.output.Printer;
import tasks.model.ObservableTaskStore;
import tasks.model.Task;
import tasks.model.TaskStore;
import tasks.model.impl.ObservableTaskStoreImpl;

@RunWith(JUnit4.class)
public final class RemoveHandlerTest {

  private static final String TASKS_DELETED_HEADER = "tasks deleted:";
  public static final String DELETE_THIS_TASK_PROMPT = "Delete this task? [Y/n]:";
  public static final String UNRECOGNIZED_RESPONSE_PROMPT = "Unrecognized answer.";

  private final ObservableTaskStore taskStore = ObservableTaskStoreImpl.createInMemoryStorage();

  private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

  private final TestReader inputReader = TestReader.create();

  private final RemoveHandler underTest =
      new RemoveHandler(
          Memoized.just(taskStore),
          new Printer.Factory(() -> new PrintStream(outputStream)),
          Memoized.just(inputReader));

  @Test
  public void handle_withNoTasks_throwsException() {
    HandlerException exception =
        assertThrows(HandlerException.class, () -> underTest.handle(removeArgsForcefully()));

    assertThat(exception).hasMessageThat().isEqualTo("no tasks specified");
  }

  @Test
  public void handle_whenForced_withOneTask_removesTask_printsRemovedTask() {
    Task task = createTask("example task");

    String output = underTest.handle(removeArgsForcefully(task)).blockingGet().renderWithoutCodes();

    assertOutputContainsGroupedTasks(output, TASKS_DELETED_HEADER, ImmutableList.of(task));
    assertTasksDeleted(task);
  }

  @Test
  public void handle_whenForced_withThreeTasks_removesTasks_printsRemovedTasks() {
    Task task1 = createTask("example task 1");
    Task task2 = createTask("example task 2");
    Task task3 = createTask("example task 3");

    String output =
        underTest.handle(removeArgsForcefully(task1, task2, task3))
            .blockingGet()
            .renderWithoutCodes();

    assertOutputContainsGroupedTasks(
        output, TASKS_DELETED_HEADER, ImmutableList.of(task1, task2, task3));
    assertTasksDeleted(task1, task2, task3);
  }

  @Test
  public void handle_whenPrompted_withOneTask_printsConfirmationPrompt_doesNotDeleteTaskYet() {
    Task task = createTask("example task");

    underTest.handle(removeArgsWithPrompt(task)).test().assertNotComplete();

    assertThat(outputStream.toString()).containsMatch(
        Pattern.compile(
            Pattern.quote(task.render().renderWithoutCodes()) +
                "\\s*" +
                Pattern.quote(DELETE_THIS_TASK_PROMPT),
            MULTILINE));
    assertTasksNotDeleted(task);
  }

  @Test
  public void handle_whenPrompted_withThreeTasks_printsFirstConfirmationPrompt_doesNotDeleteTasksYet() {
    Task task1 = createTask("example task 1");
    Task task2 = createTask("example task 2");
    Task task3 = createTask("example task 3");

    underTest.handle(removeArgsWithPrompt(task1, task2, task3)).test().assertNotComplete();

    assertThat(outputStream.toString()).containsMatch(
        Pattern.compile(
            Pattern.quote(task1.render().renderWithoutCodes()) +
                "\\s*" +
                Pattern.quote(DELETE_THIS_TASK_PROMPT),
            MULTILINE));
    assertThat(outputStream.toString()).doesNotContain(task2.render().renderWithoutCodes());
    assertThat(outputStream.toString()).doesNotContain(task3.render().renderWithoutCodes());
    assertTasksNotDeleted(task1, task2, task3);
  }

  @Test
  public void handle_whenPrompted_withOneTask_thenConfirm_deletesTask_printsDeletedTask() {
    Task task = createTask("example task");

    TestObserver<Output> subscription =
        underTest.handle(removeArgsWithPrompt(task)).test().assertNotComplete();
    inputReader.putLine("y");

    assertOutputContainsGroupedTasks(
        subscription.assertComplete().values().get(0).renderWithoutCodes(),
        TASKS_DELETED_HEADER,
        ImmutableList.of(task));
    assertTasksDeleted(task);
  }

  @Test
  public void handle_whenPrompted_withThreeTasks_thenConfirmsAll_deletesTasks_printsDeletedTasks() {
    Task task1 = createTask("example task 1");
    Task task2 = createTask("example task 2");
    Task task3 = createTask("example task 3");

    TestObserver<Output> subscription =
        underTest.handle(removeArgsWithPrompt(task1, task2, task3)).test().assertNotComplete();
    inputReader.putLine("y").putLine("y").putLine("y");

    assertOutputContainsGroupedTasks(
        subscription.assertComplete().values().get(0).renderWithoutCodes(),
        TASKS_DELETED_HEADER,
        ImmutableList.of(task1, task2, task3));
    assertTasksDeleted(task1, task2, task3);
  }

  @Test
  public void handle_whenPrompted_withOneTask_thenDeny_doesNotDeleteTask_outputsNothing() {
    Task task = createTask("example task");

    TestObserver<Output> subscription =
        underTest.handle(removeArgsWithPrompt(task)).test().assertNotComplete();
    inputReader.putLine("n");

    assertThat(subscription.assertComplete().values().get(0).renderWithoutCodes()).isEmpty();
    assertTasksNotDeleted(task);
  }

  @Test
  public void handle_whenPrompted_withThreeTasks_thenDenyAll_doesNotDeleteTasks_outputsNothing() {
    Task task1 = createTask("example task 1");
    Task task2 = createTask("example task 2");
    Task task3 = createTask("example task 3");

    TestObserver<Output> subscription =
        underTest.handle(removeArgsWithPrompt(task1, task2, task3)).test().assertNotComplete();
    inputReader.putLine("n").putLine("n").putLine("n");

    assertThat(subscription.assertComplete().values().get(0).renderWithoutCodes()).isEmpty();
    assertTasksNotDeleted(task1, task2, task3);
  }

  @Test
  public void handle_whenPrompted_withFourTasks_denyAndConfirm_behavesCorrectly() {
    Task task1 = createTask("example task 1");
    Task task2 = createTask("example task 2");
    Task task3 = createTask("example task 3");
    Task task4 = createTask("example task 4");

    TestObserver<Output> subscription =
        underTest.handle(removeArgsWithPrompt(task1, task2, task3, task4))
            .test()
            .assertNotComplete();
    inputReader.putLine("y").putLine("n").putLine("y").putLine("n");

    String output = subscription.assertComplete().values().get(0).renderWithoutCodes();
    assertOutputContainsGroupedTasks(output, TASKS_DELETED_HEADER, ImmutableList.of(task1, task3));
    assertThat(output).doesNotContain(task2.render().renderWithoutCodes());
    assertThat(output).doesNotContain(task4.render().renderWithoutCodes());
    assertTasksDeleted(task1, task3);
    assertTasksNotDeleted(task2, task4);
  }

  @Test
  public void handle_whenPrompted_withOneTask_unrecognizedInput_threeTimes_doesNotDeleteTask() {
    Task task = createTask("example task");

    TestObserver<Output> subscription =
        underTest.handle(removeArgsWithPrompt(task)).test().assertNotComplete();
    inputReader.putLine("hello").putLine("darkness").putLine("my old friend");

    String intermediateOutput = outputStream.toString();
    assertThat(intermediateOutput).matches(
        Pattern.compile(
            Pattern.quote(task.render().renderWithoutCodes()) +
                "\\s*" +
                Pattern.quote(DELETE_THIS_TASK_PROMPT) +
                "\\s*" +
                Pattern.quote(UNRECOGNIZED_RESPONSE_PROMPT) +
                "\\s*" +
                Pattern.quote(DELETE_THIS_TASK_PROMPT) +
                "\\s*" +
                Pattern.quote(UNRECOGNIZED_RESPONSE_PROMPT) +
                "\\s*" +
                Pattern.quote(DELETE_THIS_TASK_PROMPT) +
                "\\s*" +
                Pattern.quote(UNRECOGNIZED_RESPONSE_PROMPT) +
                "\\s*" +
                Pattern.quote("Assuming \"No\".") +
                "\\s*",
                MULTILINE));

    assertThat(subscription.assertComplete().values().get(0).renderWithoutCodes()).isEmpty();
    assertTasksNotDeleted(task);
  }

  private static CommonArguments<RemoveArguments> removeArgsWithPrompt(Task... tasks) {
    return removeArgs(/* force= */ false, tasks);
  }

  private static CommonArguments<RemoveArguments> removeArgsForcefully(Task... tasks) {
    return removeArgs(/* force= */ true, tasks);
  }

  private static CommonArguments<RemoveArguments> removeArgs(boolean force, Task... tasks) {
    return commonArgs(new RemoveArguments(ImmutableList.copyOf(tasks), force));
  }

  private void assertTasksNotDeleted(Task... tasks) {
    assertThatEachTask(OptionalSubject::isPresent, tasks);
  }

  private void assertTasksDeleted(Task... tasks) {
    assertThatEachTask(OptionalSubject::isEmpty, tasks);
  }

  private void assertThatEachTask(Consumer<OptionalSubject> optionalAssertions, Task... tasks) {
    TaskStore taskStoreState = taskStore.observe().blockingFirst();
    for (Task task : tasks) {
      optionalAssertions.accept(assertThat(taskStoreState.lookUpById(task.id())));
    }
  }

  private Task createTask(String label) {
    return HandlerTestUtils.createTask(taskStore, label);
  }
}