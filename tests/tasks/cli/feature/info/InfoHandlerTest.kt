package tasks.cli.feature.info

import com.google.common.truth.Truth
import java.util.function.Function
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
class InfoHandlerTest {
  private val taskStore: ObservableTaskStore = createInMemoryStorage()
  private val underTest = InfoHandler()

  @Test
  fun handle_noTasks_throwsException() {
    val exception = Assertions.assertThrows(HandlerException::class.java) { underTest.handle(infoArgs()) }
    Truth.assertThat(exception.message).contains("no tasks specified")
  }

  @Test
  fun handle_oneTask() {
    val task = createTask("test task")
    val output = underTest.handle(infoArgs(task)).blockingGet().renderWithoutCodes()
    Truth.assertThat(output).startsWith(task.render().renderWithoutCodes())
    Truth.assertThat(output).doesNotContainMatch("complete|completed")
    Truth.assertThat(output).doesNotContainMatch("start|started")
  }

  @Test
  fun handle_oneTask_withBlockingTasks() {
    val blocker0 = createTask("blocker 0")
    val blocker1 = createTask("blocker 1")
    val task = createTask(
        "test task"
    ) { builder: TaskBuilder -> builder.addBlockingTask(blocker0).addBlockingTask(blocker1) }
    val output = underTest.handle(infoArgs(task)).blockingGet().renderWithoutCodes()
    Truth.assertThat(output).startsWith(task.render().renderWithoutCodes())
    HandlerTestUtils.assertOutputContainsGroupedTasks(
        output, "tasks blocking this:", ImmutableList.of(blocker0, blocker1))
  }

  @Test
  fun handle_oneTask_withBlockedTasks() {
    val blocked0 = createTask("blocked 0")
    val blocked1 = createTask("blocked 1")
    val task = createTask(
        "test task") { builder: TaskBuilder -> builder.addBlockedTask(blocked0).addBlockedTask(blocked1) }
    val output = underTest.handle(infoArgs(task)).blockingGet().renderWithoutCodes()
    Truth.assertThat(output).startsWith(task.render().renderWithoutCodes())
    HandlerTestUtils.assertOutputContainsGroupedTasks(
        output, "tasks blocked by this:", ImmutableList.of(blocked0, blocked1))
  }

  @Test
  fun handle_oneTask_withBlockingAndBlockedTasks() {
    val blocker0 = createTask("blocker 0")
    val blocker1 = createTask("blocker 1")
    val blocked0 = createTask("blocked 0")
    val blocked1 = createTask("blocked 1")
    val task = createTask(
        "test task"
    ) { builder: TaskBuilder ->
      builder
          .addBlockingTask(blocker0)
          .addBlockingTask(blocker1)
          .addBlockedTask(blocked0)
          .addBlockedTask(blocked1)
    }
    val output = underTest.handle(infoArgs(task)).blockingGet().renderWithoutCodes()
    Truth.assertThat(output).startsWith(task.render().renderWithoutCodes())
    HandlerTestUtils.assertOutputContainsGroupedTasks(
        output, "tasks blocking this:", ImmutableList.of(blocker0, blocker1))
    HandlerTestUtils.assertOutputContainsGroupedTasks(
        output, "tasks blocked by this:", ImmutableList.of(blocked0, blocked1))
  }

  private fun createTask(label: String): Task {
    return HandlerTestUtils.createTask(taskStore, label)
  }

  private fun createTask(label: String, builderFunction: Function<TaskBuilder, TaskBuilder>): Task {
    return HandlerTestUtils.createTask(taskStore, label, builderFunction)
  }

  companion object {
    private fun infoArgs(vararg tasks: Task): CommonArguments<InfoArguments> {
      return HandlerTestUtils.commonArgs<InfoArguments>(InfoArguments(copyOf(tasks)))
    }
  }
}