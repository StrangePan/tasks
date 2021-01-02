package tasks.cli.feature.complete

import com.google.common.truth.Truth.assertThat
import java.util.function.Function
import java.util.regex.Pattern
import omnia.data.cache.Memoized.Companion.just
import omnia.data.structure.immutable.ImmutableList
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
import tasks.model.ObservableTaskStore
import tasks.model.Task
import tasks.model.Task.Status.COMPLETED
import tasks.model.Task.Status.OPEN
import tasks.model.Task.Status.STARTED
import tasks.model.TaskBuilder
import tasks.model.impl.ObservableTaskStoreImpl.Companion.createInMemoryStorage

@RunWith(JUnit4::class)
class CompleteHandlerTest {
  private val taskStore: ObservableTaskStore = createInMemoryStorage()
  private val underTest = CompleteHandler(just(taskStore))

  @Test
  fun handle_withNoSpecifiedTasks_throwsException() {
    assertThrows(HandlerException::class.java) { underTest.handle(completeArgs()) }
  }

  @Test
  fun handle_withOpenTask_marksTaskAsComplete() {
    val task = createTask("example task") { it.setStatus(OPEN) }

    underTest.handle(completeArgs(task)).ignoreElement().blockingAwait()

    assertThat(
        taskStore.observe()
            .firstOrError()
            .blockingGet()
            .lookUpById(task.id())
            .orElseThrow()
            .status())
        .isEqualTo(COMPLETED)
  }

  @Test
  fun handle_withOpenTask_outputsCompletedTask() {
    val task = createTask("example task") { it.setStatus(OPEN) }

    val output = underTest.handle(completeArgs(task)).blockingGet().toString()

    assertOutputContainsGroupedTasks(output, TASKS_COMPLETED_HEADER, getUpdatedVersionOf(task))
  }

  @Test
  fun handle_withStartedTask_marksTaskAsComplete() {
    val task = createTask("example task") { it.setStatus(STARTED) }

    underTest.handle(completeArgs(task)).ignoreElement().blockingAwait()

    assertThat(
        taskStore.observe()
            .firstOrError()
            .blockingGet()
            .lookUpById(task.id())
            .orElseThrow()
            .status())
        .isEqualTo(COMPLETED)
  }

  @Test
  fun handle_withStartedTask_outputsCompletedTask() {
    val task = createTask("example task") { it.setStatus(STARTED) }

    val output = underTest.handle(completeArgs(task)).blockingGet().toString()

    assertOutputContainsGroupedTasks(output, TASKS_COMPLETED_HEADER, getUpdatedVersionOf(task))
    assertThat(output).doesNotContain(TASKS_ALREADY_COMPLETED_HEADER)
    assertThat(output).doesNotContain(TASKS_UNBLOCKED_HEADER)
  }

  @Test
  fun handle_withCompletedTask_taskIsStillMarkedAsComplete() {
    val task = createTask("example task") { it.setStatus(COMPLETED) }

    underTest.handle(completeArgs(task)).ignoreElement().blockingAwait()

    assertThat(
        taskStore.observe()
            .firstOrError()
            .blockingGet()
            .lookUpById(task.id())
            .orElseThrow()
            .status())
        .isEqualTo(COMPLETED)
  }

  @Test
  fun handle_withCompletedTask_outputsAlreadyCompletedTask() {
    val task = createTask("example task") { it.setStatus(COMPLETED) }

    val output = underTest.handle(completeArgs(task)).blockingGet().toString()

    assertOutputContainsGroupedTasks(output, TASKS_ALREADY_COMPLETED_HEADER, task)
    assertThat(output).doesNotContain(TASKS_COMPLETED_HEADER)
    assertThat(output).doesNotContain(TASKS_UNBLOCKED_HEADER)
  }

  @Test
  fun handle_withOpenTask_withBlockedTask_unblocksBlockedTask() {
    val task = createTask("example task") { it.setStatus(OPEN) }

    val blockedTask = createTask("blocked task") { it.addBlockingTask(task) }

    underTest.handle(completeArgs(task)).ignoreElement().blockingAwait()
    assertThat(
        taskStore.observe()
            .firstOrError()
            .blockingGet()
            .lookUpById(blockedTask.id())
            .orElseThrow()
            .isUnblocked)
        .isTrue()
  }

  @Test
  fun handle_withOpenTask_withBlockedTask_outputsUnblockedTask() {
    val task = createTask("example task") { it.setStatus(OPEN) }
    val blockedTask = createTask("blocked task") { it.addBlockingTask(task) }

    val output = underTest.handle(completeArgs(task)).blockingGet().toString()

    assertOutputContainsGroupedTasks(output, TASKS_COMPLETED_HEADER, getUpdatedVersionOf(task))
    assertOutputContainsGroupedTasks(output, TASKS_UNBLOCKED_HEADER, blockedTask)
    assertThat(output).doesNotContain(TASKS_ALREADY_COMPLETED_HEADER)
  }

  @Test
  fun handle_withOpenTask_withBlockedTasks_andAlreadyCompleted_outputsInCorrectOrder() {
    val task = createTask("example task") { it.setStatus(OPEN) }
    val blockedTask = createTask("blocked task") { it.addBlockingTask(task) }
    val alreadyCompletedTask = createTask("completed task") { it.setStatus(COMPLETED) }

    val output = underTest.handle(completeArgs(task, alreadyCompletedTask)).blockingGet().toString()

    assertOutputContainsGroupedTasks(output, TASKS_ALREADY_COMPLETED_HEADER, alreadyCompletedTask)
    assertOutputContainsGroupedTasks(output, TASKS_COMPLETED_HEADER, getUpdatedVersionOf(task))
    assertOutputContainsGroupedTasks(output, TASKS_UNBLOCKED_HEADER, blockedTask)
    assertThat(output)
        .containsMatch(
            Pattern.compile(
                Pattern.quote(TASKS_ALREADY_COMPLETED_HEADER) +
                    ".*" +
                    Pattern.quote(TASKS_COMPLETED_HEADER) +
                    ".*" +
                    Pattern.quote(TASKS_UNBLOCKED_HEADER),
                Pattern.DOTALL))
  }

  private fun createTask(label: String, builderFunction: Function<TaskBuilder, TaskBuilder>): Task {
    return HandlerTestUtils.createTask(taskStore, label, builderFunction)
  }

  private fun getUpdatedVersionOf(task: Task): Task {
    return HandlerTestUtils.getUpdatedVersionOf(taskStore, task)
  }

  companion object {
    private const val TASKS_COMPLETED_HEADER = "task(s) completed:"
    private const val TASKS_ALREADY_COMPLETED_HEADER = "task(s) already completed:"
    private const val TASKS_UNBLOCKED_HEADER = "task(s) unblocked as a result:"
    private fun completeArgs(vararg tasks: Task): CommonArguments<CompleteArguments> {
      return commonArgs(CompleteArguments(copyOf(tasks)))
    }
  }
}