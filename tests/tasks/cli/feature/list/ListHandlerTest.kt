package tasks.cli.feature.list

import com.google.common.truth.Truth
import java.util.function.Function
import java.util.regex.Pattern
import omnia.data.cache.Memoized.Companion.just
import omnia.data.structure.immutable.ImmutableList
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import tasks.cli.command.common.CommonArguments
import tasks.cli.handler.testing.HandlerTestUtils
import tasks.model.ObservableTaskStore
import tasks.model.Task
import tasks.model.TaskBuilder
import tasks.model.impl.ObservableTaskStoreImpl.Companion.createInMemoryStorage

@RunWith(JUnit4::class)
class ListHandlerTest {
  private val taskStore: ObservableTaskStore = createInMemoryStorage()
  private val underTest = ListHandler(just(taskStore))

  @Test
  fun list_withEmptyGraph_printsNothing() {
    Truth.assertThat(underTest.handle(listArgsAll()).blockingGet().toString()).isEmpty()
  }

  @Test
  fun list_withOneItem_printsItem() {
    val task = createTask("example")
    val output = underTest.handle(listArgsUnblockedOnly()).blockingGet().renderWithoutCodes()
    HandlerTestUtils.assertOutputContainsGroupedTasks(output, UNBLOCKED_TASKS_HEADER, ImmutableList.of(task))
  }

  @Test
  fun list_withThreeItems_printsItems() {
    val task1 = createTask("example 1")
    val task2 = createTask("example 2")
    val task3 = createTask("example 3")
    val output = underTest.handle(listArgsUnblockedOnly()).blockingGet().renderWithoutCodes()
    HandlerTestUtils.assertOutputContainsGroupedTasks(output, UNBLOCKED_TASKS_HEADER, ImmutableList.of(task1, task2, task3))
  }

  @Test
  fun list_withBlockedItems_withUnblockedOnlyArgs_doesNotPrintBlockedItems() {
    val task1 = createTask("example task")
    val blockedTask1 = createTask("blocked task 1") { b: TaskBuilder -> b.addBlockingTask(task1) }
    val blockedTask2 = createTask("blocked task 2") { b: TaskBuilder -> b.addBlockingTask(task1) }
    val output = underTest.handle(listArgsUnblockedOnly()).blockingGet().renderWithoutCodes()
    HandlerTestUtils.assertOutputContainsGroupedTasks(output, UNBLOCKED_TASKS_HEADER, ImmutableList.of(task1))
    Truth.assertThat(output).doesNotContain("^" + Pattern.quote(BLOCKED_TASKS_HEADER))
    Truth.assertThat(output).doesNotContain(blockedTask1.render().renderWithoutCodes())
    Truth.assertThat(output).doesNotContain(blockedTask2.render().renderWithoutCodes())
  }

  @Test
  fun list_withBlockedItems_withBlockedOnlyArgs_printsOnlyBlockedItems() {
    val task1 = createTask("example task")
    val blockedTask1 = createTask("blocked task 1") { b: TaskBuilder -> b.addBlockingTask(task1) }
    val blockedTask2 = createTask("blocked task 2") { b: TaskBuilder -> b.addBlockingTask(task1) }
    val output = underTest.handle(listArgsBlockedOnly()).blockingGet().renderWithoutCodes()
    HandlerTestUtils.assertOutputContainsGroupedTasks(
        output, BLOCKED_TASKS_HEADER, ImmutableList.of(blockedTask1, blockedTask2))
    Truth.assertThat(output).doesNotContainMatch(UNBLOCKED_TASKS_HEADER)
    Truth.assertThat(output).doesNotContain(task1.render().renderWithoutCodes())
  }

  @Test
  fun list_withCompletedItems_withCompletedOnlyArgs_printsOnlyCompletedItems() {
    val completedTask1 = createTask("completed task 1") { b: TaskBuilder -> b.setStatus(Task.Status.COMPLETED) }
    val completedTask2 = createTask("completed task 2") { b: TaskBuilder -> b.setStatus(Task.Status.COMPLETED) }
    val output = underTest.handle(listArgsCompletedOnly()).blockingGet().renderWithoutCodes()
    HandlerTestUtils.assertOutputContainsGroupedTasks(
        output, COMPLETED_TASKS_HEADER, ImmutableList.of(completedTask1, completedTask2))
    Truth.assertThat(output).doesNotContain(UNBLOCKED_TASKS_HEADER)
    Truth.assertThat(output).doesNotContain(BLOCKED_TASKS_HEADER)
  }

  @Test
  fun list_withCompletedItems_withUnblockedOnlyArgs_doesNotPrintCompletedItems() {
    val completedTask1 = createTask("completed task 1") { b: TaskBuilder -> b.setStatus(Task.Status.COMPLETED) }
    val completedTask2 = createTask("completed task 2") { b: TaskBuilder -> b.setStatus(Task.Status.COMPLETED) }
    val output = underTest.handle(listArgsBlockedOnly()).blockingGet().renderWithoutCodes()
    Truth.assertThat(output).doesNotContain(COMPLETED_TASKS_HEADER)
    Truth.assertThat(output).doesNotContain(completedTask1.render().renderWithoutCodes())
    Truth.assertThat(output).doesNotContain(completedTask2.render().renderWithoutCodes())
  }

  @Test
  fun list_withAllTypes_withAllArgs_printsAllItems() {
    val unblockedTask = createTask("unblocked task")
    val blockedTask = createTask("blocked task") { b: TaskBuilder -> b.addBlockingTask(unblockedTask) }
    val unblockedStartedTask = createTask("unblocked started task") { b: TaskBuilder -> b.setStatus(Task.Status.STARTED) }
    val blockedStartedTask = createTask(
        "blocked started task"
    ) { b: TaskBuilder -> b.addBlockingTask(unblockedStartedTask).setStatus(Task.Status.STARTED) }
    val unblockedCompletedTask = createTask("unblocked completed task") { b: TaskBuilder -> b.setStatus(Task.Status.COMPLETED) }
    val blockedCompletedTask = createTask(
        "blocked completed task") { b: TaskBuilder -> b.addBlockingTask(unblockedTask).setStatus(Task.Status.COMPLETED) }
    val output = underTest.handle(listArgsAll()).blockingGet().renderWithoutCodes()
    HandlerTestUtils.assertOutputContainsGroupedTasks(
        output, UNBLOCKED_TASKS_HEADER, ImmutableList.of(unblockedTask, unblockedStartedTask))
    HandlerTestUtils.assertOutputContainsGroupedTasks(
        output, BLOCKED_TASKS_HEADER, ImmutableList.of(blockedTask, blockedStartedTask))
    HandlerTestUtils.assertOutputContainsGroupedTasks(
        output,
        COMPLETED_TASKS_HEADER,
        ImmutableList.of(unblockedCompletedTask, blockedCompletedTask))
    Truth.assertThat(output).containsMatch(
        Pattern.compile(
            Pattern.quote(UNBLOCKED_TASKS_HEADER) +
                ".*" +
                Pattern.quote(BLOCKED_TASKS_HEADER) +
                ".*" +
                Pattern.quote(COMPLETED_TASKS_HEADER),
            Pattern.DOTALL))
  }

  @Test
  fun list_withAllTypes_withAllArgs_withStartedOnlyFlag_printsOnlyStartedItems() {
    val unblockedTask = createTask("unblocked task")
    val blockedTask = createTask("blocked task") { b: TaskBuilder -> b.addBlockingTask(unblockedTask) }
    val unblockedStartedTask = createTask("unblocked started task") { b: TaskBuilder -> b.setStatus(Task.Status.STARTED) }
    val blockedStartedTask = createTask(
        "blocked started task"
    ) { b: TaskBuilder -> b.addBlockingTask(unblockedStartedTask).setStatus(Task.Status.STARTED) }
    val unblockedCompletedTask = createTask("unblocked completed task") { b: TaskBuilder -> b.setStatus(Task.Status.COMPLETED) }
    val blockedCompletedTask = createTask(
        "blocked completed task") { b: TaskBuilder -> b.addBlockingTask(unblockedTask).setStatus(Task.Status.COMPLETED) }
    val output = underTest.handle(listArgsAllStartedOnly()).blockingGet().renderWithoutCodes()
    HandlerTestUtils.assertOutputContainsGroupedTasks(
        output, UNBLOCKED_TASKS_HEADER, ImmutableList.of(unblockedStartedTask))
    HandlerTestUtils.assertOutputContainsGroupedTasks(
        output, BLOCKED_TASKS_HEADER, ImmutableList.of(blockedStartedTask))
    Truth.assertThat(output).containsMatch(
        Pattern.compile(
            Pattern.quote(UNBLOCKED_TASKS_HEADER) +
                ".*" +
                Pattern.quote(BLOCKED_TASKS_HEADER),
            Pattern.DOTALL))
    Truth.assertThat(output).doesNotContain(COMPLETED_TASKS_HEADER)
    Truth.assertThat(output).doesNotContain(unblockedTask.render().renderWithoutCodes())
    Truth.assertThat(output).doesNotContain(blockedTask.render().renderWithoutCodes())
    Truth.assertThat(output).doesNotContain(blockedCompletedTask.render().renderWithoutCodes())
    Truth.assertThat(output).doesNotContain(unblockedCompletedTask.render().renderWithoutCodes())
  }

  private fun createTask(label: String): Task {
    return HandlerTestUtils.createTask(taskStore, label)
  }

  private fun createTask(label: String, builderFunction: Function<TaskBuilder, TaskBuilder>): Task {
    return HandlerTestUtils.createTask(taskStore, label, builderFunction)
  }

  companion object {
    const val UNBLOCKED_TASKS_HEADER = "unblocked tasks:"
    const val BLOCKED_TASKS_HEADER = "blocked tasks:"
    const val COMPLETED_TASKS_HEADER = "completed tasks:"
    private fun listArgsAll(): CommonArguments<ListArguments> {
      return HandlerTestUtils.commonArgs<ListArguments>(
          ListArguments( /* isUnblockedSet= */
              true,  /* isBlockedSet= */
              true,  /* isCompletedSet= */
              true,  /* isStartedSet= */
              false))
    }

    private fun listArgsAllStartedOnly(): CommonArguments<ListArguments> {
      return HandlerTestUtils.commonArgs<ListArguments>(
          ListArguments( /* isUnblockedSet= */
              true,  /* isBlockedSet= */
              true,  /* isCompletedSet= */
              true,  /* isStartedSet= */
              true))
    }

    private fun listArgsUnblockedOnly(): CommonArguments<ListArguments> {
      return HandlerTestUtils.commonArgs<ListArguments>(
          ListArguments( /* isUnblockedSet= */
              true,  /* isBlockedSet= */
              false,  /* isCompletedSet= */
              false,  /* isStartedSet= */
              false))
    }

    private fun listArgsBlockedOnly(): CommonArguments<ListArguments> {
      return HandlerTestUtils.commonArgs<ListArguments>(
          ListArguments( /* isUnblockedSet= */
              false,  /* isBlockedSet= */
              true,  /* isCompletedSet= */
              false,  /* isStartedSet= */
              false))
    }

    private fun listArgsCompletedOnly(): CommonArguments<ListArguments> {
      return HandlerTestUtils.commonArgs<ListArguments>(
          ListArguments( /* isUnblockedSet= */
              false,  /* isBlockedSet= */
              false,  /* isCompletedSet= */
              true,  /* isStartedSet= */
              false))
    }
  }
}