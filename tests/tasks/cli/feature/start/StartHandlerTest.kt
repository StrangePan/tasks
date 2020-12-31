package tasks.cli.feature.start

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
class StartHandlerTest {
  private val taskStore: ObservableTaskStore = createInMemoryStorage()
  private val underTest = StartHandler(just(taskStore))

  @Test
  fun handle_withNoSpecifiedTasks_throwsException() {
    Assertions.assertThrows(HandlerException::class.java) { underTest.handle(startArgs()) }
  }

  @Test
  fun handle_withOpenTask_marksTaskAsStarted() {
    val task = createTask("example task") { b: TaskBuilder -> b.setStatus(Task.Status.OPEN) }
    underTest.handle(startArgs(task)).ignoreElement().blockingAwait()
    Truth.assertThat(getUpdatedVersionOf(task).status()).isEqualTo(Task.Status.STARTED)
  }

  @Test
  fun handle_withOpenTask_outputsStartedTask() {
    val task = createTask("example task") { b: TaskBuilder -> b.setStatus(Task.Status.OPEN) }
    val output = underTest.handle(startArgs(task)).blockingGet().toString()
    HandlerTestUtils.assertOutputContainsGroupedTasks(
        output, TASKS_STARTED_HEADER, ImmutableList.of(getUpdatedVersionOf(task)))
    Truth.assertThat(output).doesNotContain(TASKS_ALREADY_STARTED_HEADER)
  }

  @Test
  fun handle_withCompletedTask_marksTaskAsStarted() {
    val task = createTask("example task") { b: TaskBuilder -> b.setStatus(Task.Status.COMPLETED) }
    underTest.handle(startArgs(task)).ignoreElement().blockingAwait()
    Truth.assertThat(getUpdatedVersionOf(task).status()).isEqualTo(Task.Status.STARTED)
  }

  @Test
  fun handle_withCompletedTask_outputsStartedTask() {
    val task = createTask("example task") { b: TaskBuilder -> b.setStatus(Task.Status.COMPLETED) }
    val output = underTest.handle(startArgs(task)).blockingGet().toString()
    HandlerTestUtils.assertOutputContainsGroupedTasks(
        output, TASKS_STARTED_HEADER, ImmutableList.of(getUpdatedVersionOf(task)))
    Truth.assertThat(output).doesNotContain(TASKS_ALREADY_STARTED_HEADER)
    Truth.assertThat(output).doesNotContain(TASKS_BLOCKED_AS_A_RESULT_HEADER)
  }

  @Test
  fun handle_withStartedTask_taskIsStillStarted() {
    val task = createTask("example task") { b: TaskBuilder -> b.setStatus(Task.Status.STARTED) }
    underTest.handle(startArgs(task)).ignoreElement().blockingAwait()
    Truth.assertThat(getUpdatedVersionOf(task).status()).isEqualTo(Task.Status.STARTED)
  }

  @Test
  fun handle_withStartedTask_outputsAlreadyStartedTask() {
    val task = createTask("example task") { b: TaskBuilder -> b.setStatus(Task.Status.STARTED) }
    val output = underTest.handle(startArgs(task)).blockingGet().toString()
    HandlerTestUtils.assertOutputContainsGroupedTasks(output, TASKS_ALREADY_STARTED_HEADER, ImmutableList.of(task))
    Truth.assertThat(output).doesNotContain(TASKS_STARTED_HEADER)
  }

  @Test
  fun handle_withCompletedTask_withBlockedTask_blocksBlockedTask() {
    val task = createTask("example task") { b: TaskBuilder -> b.setStatus(Task.Status.COMPLETED) }
    val blockedTask = createTask("blocked task") { b: TaskBuilder -> b.addBlockingTask(task) }
    underTest.handle(startArgs(task)).ignoreElement().blockingAwait()
    Truth.assertThat(getUpdatedVersionOf(blockedTask).isUnblocked).isFalse()
  }

  @Test
  fun handle_withCompletedTask_withBlockedTask_outputsBlockedTask() {
    val task = createTask("example task") { b: TaskBuilder -> b.setStatus(Task.Status.COMPLETED) }
    val blockedTask = createTask("blocked task") { b: TaskBuilder -> b.addBlockingTask(task) }
    val output = underTest.handle(startArgs(task)).blockingGet().toString()
    HandlerTestUtils.assertOutputContainsGroupedTasks(
        output, TASKS_STARTED_HEADER, ImmutableList.of(getUpdatedVersionOf(task)))
    HandlerTestUtils.assertOutputContainsGroupedTasks(
        output,
        TASKS_BLOCKED_AS_A_RESULT_HEADER,
        ImmutableList.of(getUpdatedVersionOf(blockedTask)))
    Truth.assertThat(output).doesNotContain(TASKS_ALREADY_STARTED_HEADER)
  }

  @Test
  fun handle_withAllThree_outputsCorrectOutput() {
    val task = createTask("example task") { b: TaskBuilder -> b.setStatus(Task.Status.COMPLETED) }
    val blockedTask = createTask("blocked task") { b: TaskBuilder -> b.addBlockingTask(task) }
    val startedTask = createTask("open task") { b: TaskBuilder -> b.setStatus(Task.Status.STARTED) }
    val output = underTest.handle(startArgs(task, startedTask)).blockingGet().toString()
    HandlerTestUtils.assertOutputContainsGroupedTasks(
        output, TASKS_STARTED_HEADER, ImmutableList.of(getUpdatedVersionOf(task)))
    HandlerTestUtils.assertOutputContainsGroupedTasks(
        output,
        TASKS_BLOCKED_AS_A_RESULT_HEADER,
        ImmutableList.of(getUpdatedVersionOf(blockedTask)))
    HandlerTestUtils.assertOutputContainsGroupedTasks(
        output, TASKS_ALREADY_STARTED_HEADER, ImmutableList.of(startedTask))
    Truth.assertThat(output)
        .containsMatch(
            Pattern.compile(
                Pattern.quote(TASKS_ALREADY_STARTED_HEADER) +
                    ".*" +
                    Pattern.quote(TASKS_STARTED_HEADER) +
                    ".*" +
                    Pattern.quote(TASKS_BLOCKED_AS_A_RESULT_HEADER),
                Pattern.DOTALL))
  }

  private fun createTask(label: String, builderFunction: Function<TaskBuilder, TaskBuilder>): Task {
    return HandlerTestUtils.createTask(taskStore, label, builderFunction)
  }

  private fun getUpdatedVersionOf(task: Task): Task {
    return HandlerTestUtils.getUpdatedVersionOf(taskStore, task)
  }

  companion object {
    private const val TASKS_STARTED_HEADER = "task(s) started:"
    private const val TASKS_ALREADY_STARTED_HEADER = "task(s) already started:"
    private const val TASKS_BLOCKED_AS_A_RESULT_HEADER = "task(s) blocked as a result:"
    private fun startArgs(vararg tasks: Task): CommonArguments<StartArguments> {
      return HandlerTestUtils.commonArgs<StartArguments>(StartArguments(copyOf(tasks)))
    }
  }
}