package tasks.cli.feature.info

import com.google.common.truth.Truth.assertThat
import java.util.function.Function
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
import tasks.model.TaskBuilder
import tasks.model.impl.ObservableTaskStoreImpl.Companion.createInMemoryStorage

@RunWith(JUnit4::class)
class InfoHandlerTest {
  private val taskStore: ObservableTaskStore = createInMemoryStorage()
  private val underTest = InfoHandler()

  @Test
  fun handle_noTasks_throwsException() {
    val exception = assertThrows(HandlerException::class.java) { underTest.handle(infoArgs()) }
    assertThat(exception.message).contains("no tasks specified")
  }

  @Test
  fun handle_oneTask() {
    val task = createTask("test task")

    val output = underTest.handle(infoArgs(task)).blockingGet().renderWithoutCodes()

    assertThat(output).startsWith(task.render().renderWithoutCodes())
    assertThat(output).doesNotContainMatch("complete|completed")
    assertThat(output).doesNotContainMatch("start|started")
  }

  @Test
  fun handle_oneTask_withBlockingTasks() {
    val blocker0 = createTask("blocker 0")
    val blocker1 = createTask("blocker 1")
    val task = createTask("test task") { it.addBlockingTask(blocker0).addBlockingTask(blocker1) }

    val output = underTest.handle(infoArgs(task)).blockingGet().renderWithoutCodes()

    assertThat(output).startsWith(task.render().renderWithoutCodes())
    assertOutputContainsGroupedTasks(output, "tasks blocking this:", blocker0, blocker1)
  }

  @Test
  fun handle_oneTask_withBlockedTasks() {
    val blocked0 = createTask("blocked 0")
    val blocked1 = createTask("blocked 1")
    val task = createTask("test task") { it.addBlockedTask(blocked0).addBlockedTask(blocked1) }

    val output = underTest.handle(infoArgs(task)).blockingGet().renderWithoutCodes()

    assertThat(output).startsWith(task.render().renderWithoutCodes())
    assertOutputContainsGroupedTasks(output, "tasks blocked by this:", blocked0, blocked1)
  }

  @Test
  fun handle_oneTask_withBlockingAndBlockedTasks() {
    val blocker0 = createTask("blocker 0")
    val blocker1 = createTask("blocker 1")
    val blocked0 = createTask("blocked 0")
    val blocked1 = createTask("blocked 1")
    val task = createTask("test task") {
      it.addBlockingTask(blocker0)
          .addBlockingTask(blocker1)
          .addBlockedTask(blocked0)
          .addBlockedTask(blocked1)
    }

    val output = underTest.handle(infoArgs(task)).blockingGet().renderWithoutCodes()

    assertThat(output).startsWith(task.render().renderWithoutCodes())
    assertOutputContainsGroupedTasks(output, "tasks blocking this:", blocker0, blocker1)
    assertOutputContainsGroupedTasks(output, "tasks blocked by this:", blocked0, blocked1)
  }

  private fun createTask(label: String): Task {
    return HandlerTestUtils.createTask(taskStore, label)
  }

  private fun createTask(label: String, builderFunction: Function<TaskBuilder, TaskBuilder>): Task {
    return HandlerTestUtils.createTask(taskStore, label, builderFunction)
  }

  companion object {
    private fun infoArgs(vararg tasks: Task): CommonArguments<InfoArguments> {
      return commonArgs(InfoArguments(copyOf(tasks)))
    }
  }
}