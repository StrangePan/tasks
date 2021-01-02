package tasks.cli.feature.remove

import com.google.common.truth.OptionalSubject
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth8.assertThat
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.util.function.Consumer
import java.util.regex.Pattern
import omnia.data.cache.Memoized.Companion.just
import omnia.data.structure.immutable.ImmutableList.Companion.copyOf
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import tasks.cli.command.common.CommonArguments
import tasks.cli.handler.HandlerException
import tasks.cli.handler.testing.HandlerTestUtils
import tasks.cli.handler.testing.HandlerTestUtils.assertOutputContainsGroupedTasks
import tasks.cli.handler.testing.HandlerTestUtils.commonArgs
import tasks.cli.input.TestReader
import tasks.cli.output.Printer
import tasks.model.ObservableTaskStore
import tasks.model.Task
import tasks.model.impl.ObservableTaskStoreImpl.Companion.createInMemoryStorage

@RunWith(JUnit4::class)
class RemoveHandlerTest {
  private val taskStore: ObservableTaskStore = createInMemoryStorage()
  private val outputStream = ByteArrayOutputStream()
  private val inputReader: TestReader = TestReader.create()
  private val underTest = RemoveHandler(
      just(taskStore), Printer.Factory { PrintStream(outputStream) }, just(inputReader))

  @Test
  fun handle_withNoTasks_throwsException() {
    val exception = assertThrows(HandlerException::class.java) {
      underTest.handle(removeArgsForcefully())
    }
    assertThat(exception).hasMessageThat().isEqualTo("no tasks specified")
  }

  @Test
  fun handle_whenForced_withOneTask_removesTask_printsRemovedTask() {
    val task = createTask("example task")

    val output = underTest.handle(removeArgsForcefully(task)).blockingGet().renderWithoutCodes()

    assertOutputContainsGroupedTasks(output, TASKS_DELETED_HEADER, task)
    assertTasksDeleted(task)
  }

  @Test
  fun handle_whenForced_withThreeTasks_removesTasks_printsRemovedTasks() {
    val task1 = createTask("example task 1")
    val task2 = createTask("example task 2")
    val task3 = createTask("example task 3")

    val output = underTest.handle(removeArgsForcefully(task1, task2, task3))
        .blockingGet()
        .renderWithoutCodes()

    assertOutputContainsGroupedTasks(output, TASKS_DELETED_HEADER, task1, task2, task3)
    assertTasksDeleted(task1, task2, task3)
  }

  @Test
  fun handle_whenPrompted_withOneTask_printsConfirmationPrompt_doesNotDeleteTaskYet() {
    val task = createTask("example task")

    underTest.handle(removeArgsWithPrompt(task)).test().assertNotComplete()

    assertThat(outputStream.toString()).containsMatch(
        Pattern.compile(
            Pattern.quote(task.render().renderWithoutCodes()) +
                "\\s*" +
                Pattern.quote(DELETE_THIS_TASK_PROMPT),
            Pattern.MULTILINE))
    assertTasksNotDeleted(task)
  }

  @Test
  fun handle_whenPrompted_withThreeTasks_printsFirstConfirmationPrompt_doesNotDeleteTasksYet() {
    val task1 = createTask("example task 1")
    val task2 = createTask("example task 2")
    val task3 = createTask("example task 3")

    underTest.handle(removeArgsWithPrompt(task1, task2, task3)).test().assertNotComplete()

    assertThat(outputStream.toString()).containsMatch(
        Pattern.compile(
            Pattern.quote(task1.render().renderWithoutCodes()) +
                "\\s*" +
                Pattern.quote(DELETE_THIS_TASK_PROMPT),
            Pattern.MULTILINE))
    assertThat(outputStream.toString()).doesNotContain(task2.render().renderWithoutCodes())
    assertThat(outputStream.toString()).doesNotContain(task3.render().renderWithoutCodes())
    assertTasksNotDeleted(task1, task2, task3)
  }

  @Test
  fun handle_whenPrompted_withOneTask_thenConfirm_deletesTask_printsDeletedTask() {
    val task = createTask("example task")

    val subscription = underTest.handle(removeArgsWithPrompt(task)).test().assertNotComplete()
    inputReader.putLine("y")

    assertOutputContainsGroupedTasks(
        subscription.assertComplete().values()[0].renderWithoutCodes(),
        TASKS_DELETED_HEADER,
        task)
    assertTasksDeleted(task)
  }

  @Test
  fun handle_whenPrompted_withThreeTasks_thenConfirmsAll_deletesTasks_printsDeletedTasks() {
    val task1 = createTask("example task 1")
    val task2 = createTask("example task 2")
    val task3 = createTask("example task 3")

    val subscription = underTest.handle(removeArgsWithPrompt(task1, task2, task3))
        .test()
        .assertNotComplete()
    inputReader.putLine("y").putLine("y").putLine("y")

    assertOutputContainsGroupedTasks(
        subscription.assertComplete().values()[0].renderWithoutCodes(),
        TASKS_DELETED_HEADER,
        task1, task2, task3)
    assertTasksDeleted(task1, task2, task3)
  }

  @Test
  fun handle_whenPrompted_withOneTask_thenDeny_doesNotDeleteTask_outputsNothing() {
    val task = createTask("example task")

    val subscription = underTest.handle(removeArgsWithPrompt(task)).test().assertNotComplete()
    inputReader.putLine("n")

    assertThat(subscription.assertComplete().values()[0].renderWithoutCodes()).isEmpty()
    assertTasksNotDeleted(task)
  }

  @Test
  fun handle_whenPrompted_withThreeTasks_thenDenyAll_doesNotDeleteTasks_outputsNothing() {
    val task1 = createTask("example task 1")
    val task2 = createTask("example task 2")
    val task3 = createTask("example task 3")

    val subscription = underTest.handle(removeArgsWithPrompt(task1, task2, task3))
        .test()
        .assertNotComplete()
    inputReader.putLine("n").putLine("n").putLine("n")

    assertThat(subscription.assertComplete().values()[0].renderWithoutCodes()).isEmpty()
    assertTasksNotDeleted(task1, task2, task3)
  }

  @Test
  fun handle_whenPrompted_withFourTasks_denyAndConfirm_behavesCorrectly() {
    val task1 = createTask("example task 1")
    val task2 = createTask("example task 2")
    val task3 = createTask("example task 3")
    val task4 = createTask("example task 4")

    val subscription = underTest.handle(removeArgsWithPrompt(task1, task2, task3, task4))
        .test()
        .assertNotComplete()
    inputReader.putLine("y").putLine("n").putLine("y").putLine("n")
    val output = subscription.assertComplete().values()[0].renderWithoutCodes()

    assertOutputContainsGroupedTasks(output, TASKS_DELETED_HEADER, task1, task3)
    assertThat(output).doesNotContain(task2.render().renderWithoutCodes())
    assertThat(output).doesNotContain(task4.render().renderWithoutCodes())
    assertTasksDeleted(task1, task3)
    assertTasksNotDeleted(task2, task4)
  }

  @Test
  fun handle_whenPrompted_withOneTask_unrecognizedInput_threeTimes_doesNotDeleteTask() {
    val task = createTask("example task")
    val subscription = underTest.handle(removeArgsWithPrompt(task)).test().assertNotComplete()
    inputReader.putLine("hello").putLine("darkness").putLine("my old friend")

    val intermediateOutput = outputStream.toString()

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
            Pattern.MULTILINE))
    assertThat(subscription.assertComplete().values()[0].renderWithoutCodes()).isEmpty()
    assertTasksNotDeleted(task)
  }

  private fun assertTasksNotDeleted(vararg tasks: Task) {
    assertThatEachTask(OptionalSubject::isPresent, *tasks)
  }

  private fun assertTasksDeleted(vararg tasks: Task) {
    assertThatEachTask(OptionalSubject::isEmpty, *tasks)
  }

  private fun assertThatEachTask(optionalAssertions: Consumer<OptionalSubject>, vararg tasks: Task) {
    val taskStoreState = taskStore.observe().blockingFirst()
    for (task in tasks) {
      optionalAssertions.accept(assertThat(taskStoreState.lookUpById(task.id())))
    }
  }

  private fun createTask(label: String): Task {
    return HandlerTestUtils.createTask(taskStore, label)
  }

  companion object {
    private const val TASKS_DELETED_HEADER = "tasks deleted:"
    const val DELETE_THIS_TASK_PROMPT = "Delete this task [Y/n]:"
    const val UNRECOGNIZED_RESPONSE_PROMPT = "Unrecognized answer."
    private fun removeArgsWithPrompt(vararg tasks: Task): CommonArguments<RemoveArguments> {
      return removeArgs(force = false, *tasks)
    }

    private fun removeArgsForcefully(vararg tasks: Task): CommonArguments<RemoveArguments> {
      return removeArgs(force = true, *tasks)
    }

    private fun removeArgs(force: Boolean, vararg tasks: Task): CommonArguments<RemoveArguments> {
      return commonArgs(RemoveArguments(copyOf(tasks), force))
    }
  }
}