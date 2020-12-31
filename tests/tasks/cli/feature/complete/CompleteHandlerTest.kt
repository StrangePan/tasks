package tasks.cli.feature.complete

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
class CompleteHandlerTest {
  private val taskStore: ObservableTaskStore = createInMemoryStorage()
  private val underTest = CompleteHandler(just(taskStore))

  @Test
  fun handle_withNoSpecifiedTasks_throwsException() {
    Assertions.assertThrows(HandlerException::class.java) { underTest.handle(completeArgs()) }
  }

  @Test
  fun handle_withOpenTask_marksTaskAsComplete() {
    val task = createTask("example task") { b: TaskBuilder -> b.setStatus(Task.Status.OPEN) }
    underTest.handle(completeArgs(task)).ignoreElement().blockingAwait()
    Truth.assertThat(
        taskStore.observe()
            .firstOrError()
            .blockingGet()
            .lookUpById(task.id())
            .orElseThrow()
            .status())
        .isEqualTo(Task.Status.COMPLETED)
  }

  @Test
  fun handle_withOpenTask_outputsCompletedTask() {
    val task = createTask("example task") { b: TaskBuilder -> b.setStatus(Task.Status.OPEN) }
    val output = underTest.handle(completeArgs(task)).blockingGet().toString()
    HandlerTestUtils.assertOutputContainsGroupedTasks(
        output, TASKS_COMPLETED_HEADER, ImmutableList.of(getUpdatedVersionOf(task)))
  }

  @Test
  fun handle_withStartedTask_marksTaskAsComplete() {
    val task = createTask("example task") { b: TaskBuilder -> b.setStatus(Task.Status.STARTED) }
    underTest.handle(completeArgs(task)).ignoreElement().blockingAwait()
    Truth.assertThat(
        taskStore.observe()
            .firstOrError()
            .blockingGet()
            .lookUpById(task.id())
            .orElseThrow()
            .status())
        .isEqualTo(Task.Status.COMPLETED)
  }

  @Test
  fun handle_withStartedTask_outputsCompletedTask() {
    val task = createTask("example task") { b: TaskBuilder -> b.setStatus(Task.Status.STARTED) }
    val output = underTest.handle(completeArgs(task)).blockingGet().toString()
    HandlerTestUtils.assertOutputContainsGroupedTasks(
        output, TASKS_COMPLETED_HEADER, ImmutableList.of(getUpdatedVersionOf(task)))
    Truth.assertThat(output).doesNotContain(TASKS_ALREADY_COMPLETED_HEADER)
    Truth.assertThat(output).doesNotContain(TASKS_UNBLOCKED_HEADER)
  }

  @Test
  fun handle_withCompletedTask_taskIsStillMarkedAsComplete() {
    val task = createTask("example task") { b: TaskBuilder -> b.setStatus(Task.Status.COMPLETED) }
    underTest.handle(completeArgs(task)).ignoreElement().blockingAwait()
    Truth.assertThat(
        taskStore.observe()
            .firstOrError()
            .blockingGet()
            .lookUpById(task.id())
            .orElseThrow()
            .status())
        .isEqualTo(Task.Status.COMPLETED)
  }

  @Test
  fun handle_withCompletedTask_outputsAlreadyCompletedTask() {
    val task = createTask("example task") { b: TaskBuilder -> b.setStatus(Task.Status.COMPLETED) }
    val output = underTest.handle(completeArgs(task)).blockingGet().toString()
    HandlerTestUtils.assertOutputContainsGroupedTasks(
        output, TASKS_ALREADY_COMPLETED_HEADER, ImmutableList.of(task))
    Truth.assertThat(output).doesNotContain(TASKS_COMPLETED_HEADER)
    Truth.assertThat(output).doesNotContain(TASKS_UNBLOCKED_HEADER)
  }

  @Test
  fun handle_withOpenTask_withBlockedTask_unblocksBlockedTask() {
    val task = createTask("example task") { b: TaskBuilder -> b.setStatus(Task.Status.OPEN) }
    val blockedTask = createTask("blocked task") { b: TaskBuilder -> b.addBlockingTask(task) }
    underTest.handle(completeArgs(task)).ignoreElement().blockingAwait()
    Truth.assertThat(
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
    val task = createTask("example task") { b: TaskBuilder -> b.setStatus(Task.Status.OPEN) }
    val blockedTask = createTask("blocked task") { b: TaskBuilder -> b.addBlockingTask(task) }
    val output = underTest.handle(completeArgs(task)).blockingGet().toString()
    HandlerTestUtils.assertOutputContainsGroupedTasks(
        output, TASKS_COMPLETED_HEADER, ImmutableList.of(getUpdatedVersionOf(task)))
    HandlerTestUtils.assertOutputContainsGroupedTasks(output, TASKS_UNBLOCKED_HEADER, ImmutableList.of(blockedTask))
    Truth.assertThat(output).doesNotContain(TASKS_ALREADY_COMPLETED_HEADER)
  }

  @Test
  fun handle_withOpenTask_withBlockedTasks_andAlreadyCompleted_outputsInCorrectOrder() {
    val task = createTask("example task") { b: TaskBuilder -> b.setStatus(Task.Status.OPEN) }
    val blockedTask = createTask("blocked task") { b: TaskBuilder -> b.addBlockingTask(task) }
    val alreadyCompletedTask = createTask("completed task") { b: TaskBuilder -> b.setStatus(Task.Status.COMPLETED) }
    val output = underTest.handle(completeArgs(task, alreadyCompletedTask)).blockingGet().toString()
    HandlerTestUtils.assertOutputContainsGroupedTasks(
        output, TASKS_ALREADY_COMPLETED_HEADER, ImmutableList.of(alreadyCompletedTask))
    HandlerTestUtils.assertOutputContainsGroupedTasks(
        output, TASKS_COMPLETED_HEADER, ImmutableList.of(getUpdatedVersionOf(task)))
    HandlerTestUtils.assertOutputContainsGroupedTasks(
        output, TASKS_UNBLOCKED_HEADER, ImmutableList.of(blockedTask))
    Truth.assertThat(output)
        .containsMatch(
            Pattern.compile(
                Pattern.quote(TASKS_ALREADY_COMPLETED_HEADER) +
                    ".*" +
                    Pattern.quote(TASKS_COMPLETED_HEADER) +
                    ".*" +
                    Pattern.quote(TASKS_UNBLOCKED_HEADER),
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
    private const val TASKS_COMPLETED_HEADER = "task(s) completed:"
    private const val TASKS_ALREADY_COMPLETED_HEADER = "task(s) already completed:"
    private const val TASKS_UNBLOCKED_HEADER = "task(s) unblocked as a result:"
    private fun completeArgs(vararg tasks: Task): CommonArguments<CompleteArguments> {
      return HandlerTestUtils.commonArgs<CompleteArguments>(CompleteArguments(copyOf(tasks)))
    }
  }
}