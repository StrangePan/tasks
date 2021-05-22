package me.strangepan.tasks.cli.feature.list

import com.google.common.truth.Truth.assertThat
import java.util.function.Function
import java.util.regex.Pattern
import omnia.data.cache.Memoized.Companion.just
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import me.strangepan.tasks.cli.command.common.CommonArguments
import me.strangepan.tasks.cli.handler.testing.HandlerTestUtils
import me.strangepan.tasks.cli.handler.testing.HandlerTestUtils.assertOutputContainsGroupedTasks
import me.strangepan.tasks.cli.handler.testing.HandlerTestUtils.commonArgs
import me.strangepan.tasks.cli.model.render
import me.strangepan.tasks.engine.model.ObservableTaskStore
import me.strangepan.tasks.engine.model.Task
import me.strangepan.tasks.engine.model.Task.Status.COMPLETED
import me.strangepan.tasks.engine.model.Task.Status.STARTED
import me.strangepan.tasks.engine.model.TaskBuilder
import me.strangepan.tasks.engine.model.impl.ObservableTaskStoreImpl.Companion.createInMemoryStorage

@RunWith(JUnit4::class)
class ListHandlerTest {
  private val taskStore: ObservableTaskStore = createInMemoryStorage()
  private val underTest = ListHandler(just(taskStore))

  @Test
  fun list_withEmptyGraph_printsNothing() {
    assertThat(underTest.handle(listArgsAll()).blockingGet().toString()).isEmpty()
  }

  @Test
  fun list_withOneItem_printsItem() {
    val task = createTask("example")

    val output = underTest.handle(listArgsUnblockedOnly()).blockingGet().renderWithoutCodes()

    assertOutputContainsGroupedTasks(output, UNBLOCKED_TASKS_HEADER, task)
  }

  @Test
  fun list_withThreeItems_printsItems() {
    val task1 = createTask("example 1")
    val task2 = createTask("example 2")
    val task3 = createTask("example 3")

    val output = underTest.handle(listArgsUnblockedOnly()).blockingGet().renderWithoutCodes()

    assertOutputContainsGroupedTasks(output, UNBLOCKED_TASKS_HEADER, task1, task2, task3)
  }

  @Test
  fun list_withBlockedItems_withUnblockedOnlyArgs_doesNotPrintBlockedItems() {
    val task1 = createTask("example task")
    val blockedTask1 = createTask("blocked task 1") { b: TaskBuilder -> b.addBlockingTask(task1) }
    val blockedTask2 = createTask("blocked task 2") { b: TaskBuilder -> b.addBlockingTask(task1) }

    val output = underTest.handle(listArgsUnblockedOnly()).blockingGet().renderWithoutCodes()

    assertOutputContainsGroupedTasks(output, UNBLOCKED_TASKS_HEADER, task1)
    assertThat(output).doesNotContain("^" + Pattern.quote(BLOCKED_TASKS_HEADER))
    assertThat(output).doesNotContain(blockedTask1.render().renderWithoutCodes())
    assertThat(output).doesNotContain(blockedTask2.render().renderWithoutCodes())
  }

  @Test
  fun list_withBlockedItems_withBlockedOnlyArgs_printsOnlyBlockedItems() {
    val task1 = createTask("example task")
    val blockedTask1 = createTask("blocked task 1") { it.addBlockingTask(task1) }
    val blockedTask2 = createTask("blocked task 2") { it.addBlockingTask(task1) }

    val output = underTest.handle(listArgsBlockedOnly()).blockingGet().renderWithoutCodes()

    assertOutputContainsGroupedTasks(output, BLOCKED_TASKS_HEADER, blockedTask1, blockedTask2)
    assertThat(output).doesNotContainMatch(UNBLOCKED_TASKS_HEADER)
    assertThat(output).doesNotContain(task1.render().renderWithoutCodes())
  }

  @Test
  fun list_withCompletedItems_withCompletedOnlyArgs_printsOnlyCompletedItems() {
    val completedTask1 = createTask("completed task 1") { it.setStatus(COMPLETED) }
    val completedTask2 = createTask("completed task 2") { it.setStatus(COMPLETED) }

    val output = underTest.handle(listArgsCompletedOnly()).blockingGet().renderWithoutCodes()

    assertOutputContainsGroupedTasks(output, COMPLETED_TASKS_HEADER, completedTask1, completedTask2)
    assertThat(output).doesNotContain(UNBLOCKED_TASKS_HEADER)
    assertThat(output).doesNotContain(BLOCKED_TASKS_HEADER)
  }

  @Test
  fun list_withCompletedItems_withUnblockedOnlyArgs_doesNotPrintCompletedItems() {
    val completedTask1 = createTask("completed task 1") { it.setStatus(COMPLETED) }
    val completedTask2 = createTask("completed task 2") { it.setStatus(COMPLETED) }

    val output = underTest.handle(listArgsBlockedOnly()).blockingGet().renderWithoutCodes()

    assertThat(output).doesNotContain(COMPLETED_TASKS_HEADER)
    assertThat(output).doesNotContain(completedTask1.render().renderWithoutCodes())
    assertThat(output).doesNotContain(completedTask2.render().renderWithoutCodes())
  }

  @Test
  fun list_withAllTypes_withAllArgs_printsAllItems() {
    val unblockedTask = createTask("unblocked task")
    val blockedTask = createTask("blocked task") { it.addBlockingTask(unblockedTask) }
    val unblockedStartedTask = createTask("unblocked started task") { it.setStatus(STARTED) }
    val blockedStartedTask = createTask("blocked started task") {
      it.addBlockingTask(unblockedStartedTask).setStatus(STARTED)
    }
    val unblockedCompletedTask = createTask("unblocked completed task") { it.setStatus(COMPLETED) }
    val blockedCompletedTask = createTask("blocked completed task") {
      it.addBlockingTask(unblockedTask).setStatus(COMPLETED)
    }

    val output = underTest.handle(listArgsAll()).blockingGet().renderWithoutCodes()

    assertOutputContainsGroupedTasks(
        output, UNBLOCKED_TASKS_HEADER, unblockedTask, unblockedStartedTask)
    assertOutputContainsGroupedTasks(output, BLOCKED_TASKS_HEADER, blockedTask, blockedStartedTask)
    assertOutputContainsGroupedTasks(
        output, COMPLETED_TASKS_HEADER, unblockedCompletedTask, blockedCompletedTask)
    assertThat(output).containsMatch(
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
    val blockedTask = createTask("blocked task") { it.addBlockingTask(unblockedTask) }
    val unblockedStartedTask = createTask("unblocked started task") { it.setStatus(STARTED) }
    val blockedStartedTask = createTask("blocked started task") {
      it.addBlockingTask(unblockedStartedTask).setStatus(STARTED)
    }
    val unblockedCompletedTask = createTask("unblocked completed task") { it.setStatus(COMPLETED) }
    val blockedCompletedTask = createTask("blocked completed task") {
      it.addBlockingTask(unblockedTask).setStatus(COMPLETED)
    }

    val output = underTest.handle(listArgsAllStartedOnly()).blockingGet().renderWithoutCodes()

    assertOutputContainsGroupedTasks(output, UNBLOCKED_TASKS_HEADER, unblockedStartedTask)
    assertOutputContainsGroupedTasks(output, BLOCKED_TASKS_HEADER, blockedStartedTask)
    assertThat(output).containsMatch(
        Pattern.compile(
            Pattern.quote(UNBLOCKED_TASKS_HEADER) +
                ".*" +
                Pattern.quote(BLOCKED_TASKS_HEADER),
            Pattern.DOTALL))
    assertThat(output).doesNotContain(COMPLETED_TASKS_HEADER)
    assertThat(output).doesNotContain(unblockedTask.render().renderWithoutCodes())
    assertThat(output).doesNotContain(blockedTask.render().renderWithoutCodes())
    assertThat(output).doesNotContain(blockedCompletedTask.render().renderWithoutCodes())
    assertThat(output).doesNotContain(unblockedCompletedTask.render().renderWithoutCodes())
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
      return commonArgs(
          ListArguments(
              isUnblockedSet = true,
              isBlockedSet = true,
              isCompletedSet = true,
              isStartedSet = false))
    }

    private fun listArgsAllStartedOnly(): CommonArguments<ListArguments> {
      return commonArgs(
          ListArguments(
              isUnblockedSet = true,
              isBlockedSet = true,
              isCompletedSet = true,
              isStartedSet = true))
    }

    private fun listArgsUnblockedOnly(): CommonArguments<ListArguments> {
      return commonArgs(
          ListArguments(
              isUnblockedSet = true,
              isBlockedSet = false,
              isCompletedSet = false,
              isStartedSet = false))
    }

    private fun listArgsBlockedOnly(): CommonArguments<ListArguments> {
      return commonArgs(
          ListArguments(
              isUnblockedSet = false,
              isBlockedSet = true,
              isCompletedSet = false,
              isStartedSet = false))
    }

    private fun listArgsCompletedOnly(): CommonArguments<ListArguments> {
      return commonArgs(
          ListArguments(
              isUnblockedSet = false,
              isBlockedSet = false,
              isCompletedSet = true,
              isStartedSet = false))
    }
  }
}