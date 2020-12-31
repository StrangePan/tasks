package tasks.cli.feature.stop

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
class StopHandlerTest {
  private val taskStore: ObservableTaskStore = createInMemoryStorage()
  private val underTest = StopHandler(just(taskStore))

  @Test
  fun handle_withNoSpecifiedTasks_throwsException() {
    Assertions.assertThrows(HandlerException::class.java) { underTest.handle(startArgs()) }
  }

  @Test
  fun handle_withStartedTask_marksTaskAsOpen() {
    val task = createTask("example task") { b: TaskBuilder -> b.setStatus(Task.Status.STARTED) }
    underTest.handle(startArgs(task)).ignoreElement().blockingAwait()
    Truth.assertThat(getUpdatedVersionOf(task).status()).isEqualTo(Task.Status.OPEN)
  }

  @Test
  fun handle_withStartedTask_outputsOpenTask() {
    val task = createTask("example task") { b: TaskBuilder -> b.setStatus(Task.Status.STARTED) }
    val output = underTest.handle(startArgs(task)).blockingGet().toString()
    HandlerTestUtils.assertOutputContainsGroupedTasks(
        output, TASKS_STOPPED_HEADER, ImmutableList.of(getUpdatedVersionOf(task)))
    Truth.assertThat(output).doesNotContain(TASKS_ALREADY_STOPPED_HEADER)
  }

  @Test
  fun handle_withCompletedTask_taskIsStillCompleted() {
    val task = createTask("example task") { b: TaskBuilder -> b.setStatus(Task.Status.COMPLETED) }
    underTest.handle(startArgs(task)).ignoreElement().blockingAwait()
    Truth.assertThat(getUpdatedVersionOf(task).status()).isEqualTo(Task.Status.COMPLETED)
  }

  @Test
  fun handle_withCompletedTask_outputsAlreadyCompletedTask() {
    val task = createTask("example task") { b: TaskBuilder -> b.setStatus(Task.Status.COMPLETED) }
    val output = underTest.handle(startArgs(task)).blockingGet().toString()
    HandlerTestUtils.assertOutputContainsGroupedTasks(output, TASKS_ALREADY_STOPPED_HEADER, ImmutableList.of(task))
    Truth.assertThat(output).doesNotContain(TASKS_STOPPED_HEADER)
  }

  @Test
  fun handle_withOpenTask_taskIsStillOpen() {
    val task = createTask("example task") { b: TaskBuilder -> b.setStatus(Task.Status.OPEN) }
    underTest.handle(startArgs(task)).ignoreElement().blockingAwait()
    Truth.assertThat(getUpdatedVersionOf(task).status()).isEqualTo(Task.Status.OPEN)
  }

  @Test
  fun handle_withOpenTask_outputsAlreadyOpenTask() {
    val task = createTask("example task") { b: TaskBuilder -> b.setStatus(Task.Status.OPEN) }
    val output = underTest.handle(startArgs(task)).blockingGet().toString()
    HandlerTestUtils.assertOutputContainsGroupedTasks(output, TASKS_ALREADY_STOPPED_HEADER, ImmutableList.of(task))
    Truth.assertThat(output).doesNotContain(TASKS_STOPPED_HEADER)
  }

  @Test
  fun handle_withBoth_outputsCorrectOutput() {
    val startedTask = createTask("started task") { b: TaskBuilder -> b.setStatus(Task.Status.STARTED) }
    val openTask = createTask("open task") { b: TaskBuilder -> b.setStatus(Task.Status.OPEN) }
    val completedTask = createTask("completed task") { b: TaskBuilder -> b.setStatus(Task.Status.COMPLETED) }
    val output = underTest.handle(startArgs(startedTask, openTask, completedTask)).blockingGet().toString()
    HandlerTestUtils.assertOutputContainsGroupedTasks(
        output, TASKS_ALREADY_STOPPED_HEADER, ImmutableList.of(openTask, completedTask))
    HandlerTestUtils.assertOutputContainsGroupedTasks(
        output, TASKS_STOPPED_HEADER, ImmutableList.of(getUpdatedVersionOf(startedTask)))
    Truth.assertThat(output)
        .containsMatch(
            Pattern.compile(
                Pattern.quote(TASKS_ALREADY_STOPPED_HEADER) +
                    ".*" +
                    Pattern.quote(TASKS_STOPPED_HEADER),
                Pattern.DOTALL))
  }

  private fun createTask(label: String): Task {
    return HandlerTestUtils.createTask(taskStore, label)
  }

  private fun createTask(label: String, builderFunction: Function<TaskBuilder, TaskBuilder>): Task {
    return HandlerTestUtils.createTask(taskStore, label, builderFunction)
  }

  private fun getUpdatedVersionOf(task: Task): Task {
    return HandlerTestUtils.getUpdatedVersionOf(taskStore, task)
  }

  companion object {
    private const val TASKS_STOPPED_HEADER = "task(s) stopped:"
    const val TASKS_ALREADY_STOPPED_HEADER = "task(s) already stopped:"
    private fun startArgs(vararg tasks: Task): CommonArguments<StopArguments> {
      return HandlerTestUtils.commonArgs(StopArguments(copyOf(tasks)))
    }
  }
}