package me.strangepan.tasks.cli.feature.stop

import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import java.util.function.Function
import java.util.regex.Pattern
import omnia.data.cache.Memoized.Companion.just
import omnia.data.structure.immutable.ImmutableList
import omnia.data.structure.immutable.ImmutableList.Companion.copyOf
import org.junit.Test
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import me.strangepan.tasks.cli.command.common.CommonArguments
import me.strangepan.tasks.cli.handler.HandlerException
import me.strangepan.tasks.cli.handler.testing.HandlerTestUtils
import me.strangepan.tasks.cli.handler.testing.HandlerTestUtils.assertOutputContainsGroupedTasks
import me.strangepan.tasks.cli.handler.testing.HandlerTestUtils.commonArgs
import me.strangepan.tasks.cli.handler.testing.HandlerTestUtils.getUpdatedVersionOf
import me.strangepan.tasks.engine.model.ObservableTaskStore
import me.strangepan.tasks.engine.model.Task
import me.strangepan.tasks.engine.model.TaskBuilder
import me.strangepan.tasks.engine.model.impl.ObservableTaskStoreImpl.Companion.createInMemoryStorage

@RunWith(JUnit4::class)
class StopHandlerTest {
  private val taskStore: ObservableTaskStore = createInMemoryStorage()
  private val underTest = StopHandler(just(taskStore))

  @Test
  fun handle_withNoSpecifiedTasks_throwsException() {
    assertThrows(HandlerException::class.java) { underTest.handle(startArgs()) }
  }

  @Test
  fun handle_withStartedTask_marksTaskAsOpen() {
    val task = createTask("example task") { it.setStatus(Task.Status.STARTED) }

    underTest.handle(startArgs(task)).ignoreElement().blockingAwait()

    assertThat(getUpdatedVersionOf(task).status()).isEqualTo(Task.Status.OPEN)
  }

  @Test
  fun handle_withStartedTask_outputsOpenTask() {
    val task = createTask("example task") { it.setStatus(Task.Status.STARTED) }

    val output = underTest.handle(startArgs(task)).blockingGet().toString()

    assertOutputContainsGroupedTasks(output, TASKS_STOPPED_HEADER, getUpdatedVersionOf(task))
    assertThat(output).doesNotContain(TASKS_ALREADY_STOPPED_HEADER)
  }

  @Test
  fun handle_withCompletedTask_taskIsStillCompleted() {
    val task = createTask("example task") { it.setStatus(Task.Status.COMPLETED) }

    underTest.handle(startArgs(task)).ignoreElement().blockingAwait()

    assertThat(getUpdatedVersionOf(task).status()).isEqualTo(Task.Status.COMPLETED)
  }

  @Test
  fun handle_withCompletedTask_outputsAlreadyCompletedTask() {
    val task = createTask("example task") { it.setStatus(Task.Status.COMPLETED) }

    val output = underTest.handle(startArgs(task)).blockingGet().toString()

    assertOutputContainsGroupedTasks(output, TASKS_ALREADY_STOPPED_HEADER, task)
    assertThat(output).doesNotContain(TASKS_STOPPED_HEADER)
  }

  @Test
  fun handle_withOpenTask_taskIsStillOpen() {
    val task = createTask("example task") { it.setStatus(Task.Status.OPEN) }

    underTest.handle(startArgs(task)).ignoreElement().blockingAwait()

    assertThat(getUpdatedVersionOf(task).status()).isEqualTo(Task.Status.OPEN)
  }

  @Test
  fun handle_withOpenTask_outputsAlreadyOpenTask() {
    val task = createTask("example task") { it.setStatus(Task.Status.OPEN) }

    val output = underTest.handle(startArgs(task)).blockingGet().toString()

    assertOutputContainsGroupedTasks(output, TASKS_ALREADY_STOPPED_HEADER, task)
    assertThat(output).doesNotContain(TASKS_STOPPED_HEADER)
  }

  @Test
  fun handle_withBoth_outputsCorrectOutput() {
    val startedTask = createTask("started task") { it.setStatus(Task.Status.STARTED) }
    val openTask = createTask("open task") { it.setStatus(Task.Status.OPEN) }
    val completedTask = createTask("completed task") { it.setStatus(Task.Status.COMPLETED) }

    val output = underTest.handle(startArgs(startedTask, openTask, completedTask)).blockingGet().toString()

    assertOutputContainsGroupedTasks(output, TASKS_ALREADY_STOPPED_HEADER, openTask, completedTask)
    assertOutputContainsGroupedTasks(output, TASKS_STOPPED_HEADER, getUpdatedVersionOf(startedTask))
    assertThat(output)
        .containsMatch(
            Pattern.compile(
                Pattern.quote(TASKS_ALREADY_STOPPED_HEADER) +
                    ".*" +
                    Pattern.quote(TASKS_STOPPED_HEADER),
                Pattern.DOTALL))
  }

  private fun createTask(label: String, builderFunction: Function<TaskBuilder, TaskBuilder>): Task {
    return HandlerTestUtils.createTask(taskStore, label, builderFunction)
  }

  private fun getUpdatedVersionOf(task: Task): Task {
    return getUpdatedVersionOf(taskStore, task)
  }

  companion object {
    private const val TASKS_STOPPED_HEADER = "task(s) stopped:"
    const val TASKS_ALREADY_STOPPED_HEADER = "task(s) already stopped:"
    private fun startArgs(vararg tasks: Task): CommonArguments<StopArguments> {
      return commonArgs(StopArguments(copyOf(tasks)))
    }
  }
}