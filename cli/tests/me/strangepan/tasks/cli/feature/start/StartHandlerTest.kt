package me.strangepan.tasks.cli.feature.start

import com.google.common.truth.Truth.assertThat
import java.util.function.Function
import java.util.regex.Pattern
import omnia.data.cache.Memoized.Companion.just
import omnia.data.structure.immutable.ImmutableList.Companion.copyOf
import org.junit.Test
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
import me.strangepan.tasks.engine.model.Task.Status.COMPLETED
import me.strangepan.tasks.engine.model.Task.Status.OPEN
import me.strangepan.tasks.engine.model.Task.Status.STARTED
import me.strangepan.tasks.engine.model.TaskBuilder
import me.strangepan.tasks.engine.model.impl.ObservableTaskStoreImpl.Companion.createInMemoryStorage

@RunWith(JUnit4::class)
class StartHandlerTest {
  private val taskStore: ObservableTaskStore = createInMemoryStorage()
  private val underTest = StartHandler(just(taskStore))

  @Test
  fun handle_withNoSpecifiedTasks_throwsException() {
    assertThrows(HandlerException::class.java) { underTest.handle(startArgs()) }
  }

  @Test
  fun handle_withOpenTask_marksTaskAsStarted() {
    val task = createTask("example task") { it.setStatus(OPEN) }

    underTest.handle(startArgs(task)).ignoreElement().blockingAwait()

    assertThat(getUpdatedVersionOf(task).status).isEqualTo(STARTED)
  }

  @Test
  fun handle_withOpenTask_outputsStartedTask() {
    val task = createTask("example task") { it.setStatus(OPEN) }

    val output = underTest.handle(startArgs(task)).blockingGet().toString()

    assertOutputContainsGroupedTasks(output, TASKS_STARTED_HEADER, getUpdatedVersionOf(task))
    assertThat(output).doesNotContain(TASKS_ALREADY_STARTED_HEADER)
  }

  @Test
  fun handle_withCompletedTask_marksTaskAsStarted() {
    val task = createTask("example task") { it.setStatus(COMPLETED) }

    underTest.handle(startArgs(task)).ignoreElement().blockingAwait()

    assertThat(getUpdatedVersionOf(task).status).isEqualTo(STARTED)
  }

  @Test
  fun handle_withCompletedTask_outputsStartedTask() {
    val task = createTask("example task") { it.setStatus(COMPLETED) }

    val output = underTest.handle(startArgs(task)).blockingGet().toString()

    assertOutputContainsGroupedTasks(output, TASKS_STARTED_HEADER, getUpdatedVersionOf(task))
    assertThat(output).doesNotContain(TASKS_ALREADY_STARTED_HEADER)
    assertThat(output).doesNotContain(TASKS_BLOCKED_AS_A_RESULT_HEADER)
  }

  @Test
  fun handle_withStartedTask_taskIsStillStarted() {
    val task = createTask("example task") { it.setStatus(STARTED) }

    underTest.handle(startArgs(task)).ignoreElement().blockingAwait()

    assertThat(getUpdatedVersionOf(task).status).isEqualTo(STARTED)
  }

  @Test
  fun handle_withStartedTask_outputsAlreadyStartedTask() {
    val task = createTask("example task") { it.setStatus(STARTED) }

    val output = underTest.handle(startArgs(task)).blockingGet().toString()

    assertOutputContainsGroupedTasks(output, TASKS_ALREADY_STARTED_HEADER, task)
    assertThat(output).doesNotContain(TASKS_STARTED_HEADER)
  }

  @Test
  fun handle_withCompletedTask_withBlockedTask_blocksBlockedTask() {
    val task = createTask("example task") { it.setStatus(COMPLETED) }
    val blockedTask = createTask("blocked task") { it.addBlockingTask(task) }

    underTest.handle(startArgs(task)).ignoreElement().blockingAwait()

    assertThat(getUpdatedVersionOf(blockedTask).isUnblocked).isFalse()
  }

  @Test
  fun handle_withCompletedTask_withBlockedTask_outputsBlockedTask() {
    val task = createTask("example task") { it.setStatus(COMPLETED) }
    val blockedTask = createTask("blocked task") { it.addBlockingTask(task) }

    val output = underTest.handle(startArgs(task)).blockingGet().toString()

    assertOutputContainsGroupedTasks(output, TASKS_STARTED_HEADER, getUpdatedVersionOf(task))
    assertOutputContainsGroupedTasks(
        output,
        TASKS_BLOCKED_AS_A_RESULT_HEADER,
        getUpdatedVersionOf(blockedTask))
    assertThat(output).doesNotContain(TASKS_ALREADY_STARTED_HEADER)
  }

  @Test
  fun handle_withAllThree_outputsCorrectOutput() {
    val task = createTask("example task") { it.setStatus(COMPLETED) }
    val blockedTask = createTask("blocked task") { it.addBlockingTask(task) }
    val startedTask = createTask("open task") { it.setStatus(STARTED) }

    val output = underTest.handle(startArgs(task, startedTask)).blockingGet().toString()

    assertOutputContainsGroupedTasks(output, TASKS_STARTED_HEADER, getUpdatedVersionOf(task))
    assertOutputContainsGroupedTasks(
        output,
        TASKS_BLOCKED_AS_A_RESULT_HEADER,
        getUpdatedVersionOf(blockedTask))
    assertOutputContainsGroupedTasks(output, TASKS_ALREADY_STARTED_HEADER, startedTask)
    assertThat(output)
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
    return getUpdatedVersionOf(taskStore, task)
  }

  companion object {
    private const val TASKS_STARTED_HEADER = "task(s) started:"
    private const val TASKS_ALREADY_STARTED_HEADER = "task(s) already started:"
    private const val TASKS_BLOCKED_AS_A_RESULT_HEADER = "task(s) blocked as a result:"
    private fun startArgs(vararg tasks: Task): CommonArguments<StartArguments> {
      return commonArgs(StartArguments(copyOf(tasks)))
    }
  }
}