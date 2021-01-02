package tasks.cli.feature.reopen

import com.google.common.truth.Truth.assertThat
import java.util.function.Function
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
import tasks.cli.handler.testing.HandlerTestUtils.getUpdatedVersionOf
import tasks.model.ObservableTaskStore
import tasks.model.Task
import tasks.model.Task.Status.COMPLETED
import tasks.model.Task.Status.OPEN
import tasks.model.Task.Status.STARTED
import tasks.model.TaskBuilder
import tasks.model.impl.ObservableTaskStoreImpl.Companion.createInMemoryStorage

@RunWith(JUnit4::class)
class ReopenHandlerTest {
  private val taskStore: ObservableTaskStore = createInMemoryStorage()
  private val underTest = ReopenHandler(just(taskStore))

  @Test
  fun handle_withNoSpecifiedTasks_throwsException() {
    assertThrows(HandlerException::class.java) { underTest.handle(reopenArgs()) }
  }

  @Test
  fun handle_withCompletedTask_marksTaskAsOpen() {
    val task = createTask("example task") { it.setStatus(COMPLETED) }

    underTest.handle(reopenArgs(task)).ignoreElement().blockingAwait()

    assertThat(
        taskStore.observe()
            .firstOrError()
            .blockingGet()
            .lookUpById(task.id())
            .orElseThrow()
            .status())
        .isEqualTo(OPEN)
  }

  @Test
  fun handle_withCompletedTask_outputsOpenedTask() {
    val task = createTask("example task") { it.setStatus(COMPLETED) }

    val output = underTest.handle(reopenArgs(task)).blockingGet().toString()

    assertOutputContainsGroupedTasks(output, TASKS_REOPENED_HEADER, getUpdatedVersionOf(task))
    assertThat(output).doesNotContain(TASKS_ALREADY_OPEN_HEADER)
    assertThat(output).doesNotContain(TASKS_BLOCKED_AS_A_RESULT)
  }

  @Test
  fun handle_withOpenTask_taskIsStillOpen() {
    val task = createTask("example task") { it.setStatus(OPEN) }

    underTest.handle(reopenArgs(task)).ignoreElement().blockingAwait()

    assertThat(
        taskStore.observe()
            .firstOrError()
            .blockingGet()
            .lookUpById(task.id())
            .orElseThrow()
            .status())
        .isEqualTo(OPEN)
  }

  @Test
  fun handle_withOpenTask_outputsAlreadyOpenTask() {
    val task = createTask("example task") { it.setStatus(OPEN) }

    val output = underTest.handle(reopenArgs(task)).blockingGet().toString()

    assertOutputContainsGroupedTasks(output, TASKS_ALREADY_OPEN_HEADER, task)
    assertThat(output).doesNotContain(TASKS_REOPENED_HEADER)
    assertThat(output).doesNotContain(TASKS_BLOCKED_AS_A_RESULT)
  }

  @Test
  fun handle_withStartedTask_taskIsStillStarted() {
    val task = createTask("example task") { it.setStatus(STARTED) }

    underTest.handle(reopenArgs(task)).ignoreElement().blockingAwait()

    assertThat(
        taskStore.observe()
            .firstOrError()
            .blockingGet()
            .lookUpById(task.id())
            .orElseThrow()
            .status())
        .isEqualTo(STARTED)
  }

  @Test
  fun handle_withStartedTask_outputsAlreadyStartedTask() {
    val task = createTask("example task") { it.setStatus(STARTED) }

    val output = underTest.handle(reopenArgs(task)).blockingGet().toString()

    assertOutputContainsGroupedTasks(output, TASKS_ALREADY_OPEN_HEADER, task)
    assertThat(output).doesNotContain(TASKS_REOPENED_HEADER)
    assertThat(output).doesNotContain(TASKS_BLOCKED_AS_A_RESULT)
  }

  @Test
  fun handle_withCompletedTask_withBlockedTask_blocksBlockedTask() {
    val task = createTask("example task") { it.setStatus(COMPLETED) }
    val blockedTask = createTask("blocked task") { it.addBlockingTask(task) }

    underTest.handle(reopenArgs(task)).ignoreElement().blockingAwait()

    assertThat(
        taskStore.observe()
            .firstOrError()
            .blockingGet()
            .lookUpById(blockedTask.id())
            .orElseThrow()
            .isUnblocked)
        .isFalse()
  }

  @Test
  fun handle_withCompletedTask_withBlockedTask_outputsBlockedTask() {
    val task = createTask("example task") { it.setStatus(COMPLETED) }
    val blockedTask = createTask("blocked task") { it.addBlockingTask(task) }

    val output = underTest.handle(reopenArgs(task)).blockingGet().toString()

    assertOutputContainsGroupedTasks(output, TASKS_REOPENED_HEADER, getUpdatedVersionOf(task))
    assertOutputContainsGroupedTasks(output, TASKS_BLOCKED_AS_A_RESULT, blockedTask)
    assertThat(output).doesNotContain(TASKS_ALREADY_OPEN_HEADER)
  }

  @Test
  fun handle_withAllThree_outputsCorrectOutput() {
    val task = createTask("example task") { it.setStatus(COMPLETED) }
    val blockedTask = createTask("blocked task") { it.addBlockingTask(task) }
    val openTask = createTask("open task") { it.setStatus(OPEN) }

    val output = underTest.handle(reopenArgs(task, openTask)).blockingGet().toString()

    assertOutputContainsGroupedTasks(output, TASKS_ALREADY_OPEN_HEADER, openTask)
    assertOutputContainsGroupedTasks(output, TASKS_REOPENED_HEADER, getUpdatedVersionOf(task))
    assertOutputContainsGroupedTasks(output, TASKS_BLOCKED_AS_A_RESULT, blockedTask)
    assertThat(output)
        .containsMatch(
            Pattern.compile(
                Pattern.quote(TASKS_ALREADY_OPEN_HEADER) +
                    ".*" +
                    Pattern.quote(TASKS_REOPENED_HEADER) +
                    ".*" +
                    Pattern.quote(TASKS_BLOCKED_AS_A_RESULT),
                Pattern.DOTALL))
  }

  private fun createTask(label: String, builderFunction: Function<TaskBuilder, TaskBuilder>): Task {
    return HandlerTestUtils.createTask(taskStore, label, builderFunction)
  }

  private fun getUpdatedVersionOf(task: Task): Task {
    return getUpdatedVersionOf(taskStore, task)
  }

  companion object {
    private const val TASKS_REOPENED_HEADER = "task(s) reopened:"
    private const val TASKS_ALREADY_OPEN_HEADER = "task(s) already open:"
    private const val TASKS_BLOCKED_AS_A_RESULT = "task(s) blocked as a result:"
    private fun reopenArgs(vararg tasks: Task): CommonArguments<ReopenArguments> {
      return commonArgs(ReopenArguments(copyOf(tasks)))
    }
  }
}