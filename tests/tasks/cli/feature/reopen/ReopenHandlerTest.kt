package tasks.cli.feature.reopen

import com.google.common.truth.Truth
import java.util.function.Function
import java.util.regex.Pattern
import omnia.data.cache.Memoized.Companion.just
import omnia.data.structure.immutable.ImmutableList
import omnia.data.structure.immutable.ImmutableList.Companion.copyOf
import org.junit.Test
import org.junit.jupiter.api.Assertions
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import tasks.cli.command.common.CommonArguments
import tasks.cli.handler.HandlerException
import tasks.cli.handler.testing.HandlerTestUtils
import tasks.model.ObservableTaskStore
import tasks.model.Task
import tasks.model.TaskBuilder
import tasks.model.impl.ObservableTaskStoreImpl.Companion.createInMemoryStorage

@RunWith(JUnit4::class)
class ReopenHandlerTest {
  private val taskStore: ObservableTaskStore = createInMemoryStorage()
  private val underTest = ReopenHandler(just(taskStore))

  @Test
  fun handle_withNoSpecifiedTasks_throwsException() {
    Assertions.assertThrows(HandlerException::class.java) { underTest.handle(reopenArgs()) }
  }

  @Test
  fun handle_withCompletedTask_marksTaskAsOpen() {
    val task = createTask("example task") { b: TaskBuilder -> b.setStatus(Task.Status.COMPLETED) }
    underTest.handle(reopenArgs(task)).ignoreElement().blockingAwait()
    Truth.assertThat(
        taskStore.observe()
            .firstOrError()
            .blockingGet()
            .lookUpById(task.id())
            .orElseThrow()
            .status())
        .isEqualTo(Task.Status.OPEN)
  }

  @Test
  fun handle_withCompletedTask_outputsOpenedTask() {
    val task = createTask("example task") { b: TaskBuilder -> b.setStatus(Task.Status.COMPLETED) }
    val output = underTest.handle(reopenArgs(task)).blockingGet().toString()
    HandlerTestUtils.assertOutputContainsGroupedTasks(
        output, TASKS_REOPENED_HEADER, ImmutableList.of(getUpdatedVersionOf(task)))
    Truth.assertThat(output).doesNotContain(TASKS_ALREADY_OPEN_HEADER)
    Truth.assertThat(output).doesNotContain(TASKS_BLOCKED_AS_A_RESULT)
  }

  @Test
  fun handle_withOpenTask_taskIsStillOpen() {
    val task = createTask("example task") { b: TaskBuilder -> b.setStatus(Task.Status.OPEN) }
    underTest.handle(reopenArgs(task)).ignoreElement().blockingAwait()
    Truth.assertThat(
        taskStore.observe()
            .firstOrError()
            .blockingGet()
            .lookUpById(task.id())
            .orElseThrow()
            .status())
        .isEqualTo(Task.Status.OPEN)
  }

  @Test
  fun handle_withOpenTask_outputsAlreadyOpenTask() {
    val task = createTask("example task") { b: TaskBuilder -> b.setStatus(Task.Status.OPEN) }
    val output = underTest.handle(reopenArgs(task)).blockingGet().toString()
    HandlerTestUtils.assertOutputContainsGroupedTasks(output, TASKS_ALREADY_OPEN_HEADER, ImmutableList.of(task))
    Truth.assertThat(output).doesNotContain(TASKS_REOPENED_HEADER)
    Truth.assertThat(output).doesNotContain(TASKS_BLOCKED_AS_A_RESULT)
  }

  @Test
  fun handle_withStartedTask_taskIsStillStarted() {
    val task = createTask("example task") { b: TaskBuilder -> b.setStatus(Task.Status.STARTED) }
    underTest.handle(reopenArgs(task)).ignoreElement().blockingAwait()
    Truth.assertThat(
        taskStore.observe()
            .firstOrError()
            .blockingGet()
            .lookUpById(task.id())
            .orElseThrow()
            .status())
        .isEqualTo(Task.Status.STARTED)
  }

  @Test
  fun handle_withStartedTask_outputsAlreadyStartedTask() {
    val task = createTask("example task") { b: TaskBuilder -> b.setStatus(Task.Status.STARTED) }
    val output = underTest.handle(reopenArgs(task)).blockingGet().toString()
    HandlerTestUtils.assertOutputContainsGroupedTasks(output, TASKS_ALREADY_OPEN_HEADER, ImmutableList.of(task))
    Truth.assertThat(output).doesNotContain(TASKS_REOPENED_HEADER)
    Truth.assertThat(output).doesNotContain(TASKS_BLOCKED_AS_A_RESULT)
  }

  @Test
  fun handle_withCompletedTask_withBlockedTask_blocksBlockedTask() {
    val task = createTask("example task") { b: TaskBuilder -> b.setStatus(Task.Status.COMPLETED) }
    val blockedTask = createTask("blocked task") { b: TaskBuilder -> b.addBlockingTask(task) }
    underTest.handle(reopenArgs(task)).ignoreElement().blockingAwait()
    Truth.assertThat(
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
    val task = createTask("example task") { b: TaskBuilder -> b.setStatus(Task.Status.COMPLETED) }
    val blockedTask = createTask("blocked task") { b: TaskBuilder -> b.addBlockingTask(task) }
    val output = underTest.handle(reopenArgs(task)).blockingGet().toString()
    HandlerTestUtils.assertOutputContainsGroupedTasks(
        output, TASKS_REOPENED_HEADER, ImmutableList.of(getUpdatedVersionOf(task)))
    HandlerTestUtils.assertOutputContainsGroupedTasks(
        output, TASKS_BLOCKED_AS_A_RESULT, ImmutableList.of(blockedTask))
    Truth.assertThat(output).doesNotContain(TASKS_ALREADY_OPEN_HEADER)
  }

  @Test
  fun handle_withAllThree_outputsCorrectOutput() {
    val task = createTask("example task") { b: TaskBuilder -> b.setStatus(Task.Status.COMPLETED) }
    val blockedTask = createTask("blocked task") { b: TaskBuilder -> b.addBlockingTask(task) }
    val openTask = createTask("open task") { b: TaskBuilder -> b.setStatus(Task.Status.OPEN) }
    val output = underTest.handle(reopenArgs(task, openTask)).blockingGet().toString()
    HandlerTestUtils.assertOutputContainsGroupedTasks(output, TASKS_ALREADY_OPEN_HEADER, ImmutableList.of(openTask))
    HandlerTestUtils.assertOutputContainsGroupedTasks(
        output, TASKS_REOPENED_HEADER, ImmutableList.of(getUpdatedVersionOf(task)))
    HandlerTestUtils.assertOutputContainsGroupedTasks(
        output, TASKS_BLOCKED_AS_A_RESULT, ImmutableList.of(blockedTask))
    Truth.assertThat(output)
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
    return HandlerTestUtils.getUpdatedVersionOf(taskStore, task)
  }

  companion object {
    private const val TASKS_REOPENED_HEADER = "task(s) reopened:"
    private const val TASKS_ALREADY_OPEN_HEADER = "task(s) already open:"
    private const val TASKS_BLOCKED_AS_A_RESULT = "task(s) blocked as a result:"
    private fun reopenArgs(vararg tasks: Task): CommonArguments<ReopenArguments> {
      return HandlerTestUtils.commonArgs<ReopenArguments>(ReopenArguments(copyOf(tasks)))
    }
  }
}