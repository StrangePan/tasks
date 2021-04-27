package me.strangepan.tasks.cli.feature.reword

import com.google.common.truth.Truth.assertThat
import omnia.data.cache.Memoized.Companion.just
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
import me.strangepan.tasks.engine.model.impl.ObservableTaskStoreImpl.Companion.createInMemoryStorage

@RunWith(JUnit4::class)
class RewordHandlerTest {
  private val taskStore: ObservableTaskStore = createInMemoryStorage()
  private val underTest = RewordHandler(just(taskStore))

  @Test
  fun handle_withEmptyDescription_throwsException_doesNotModifyLabel() {
    val task = createTask("example task")

    assertThrows(HandlerException::class.java) { underTest.handle(rewordArgs(task, "  ")) }
    assertThat(getUpdatedVersionOf(task).label()).isEqualTo("example task")
  }

  @Test
  fun handle_withNewDescription_renamesTask() {
    val task = createTask("example task")

    underTest.handle(rewordArgs(task, "reworded task")).ignoreElement().blockingAwait()

    assertThat(getUpdatedVersionOf(task).label()).isEqualTo("reworded task")
  }

  @Test
  fun handle_withNewDescription_outputsRewordedTask() {
    val task = createTask("example task")

    val output = underTest.handle(rewordArgs(task, "reworded task")).blockingGet().toString()

    assertOutputContainsGroupedTasks(output, "Updated description:", getUpdatedVersionOf(task))
    assertThat(output).doesNotContain("example task")
  }

  private fun createTask(label: String): Task {
    return HandlerTestUtils.createTask(taskStore, label)
  }

  private fun getUpdatedVersionOf(task: Task): Task {
    return getUpdatedVersionOf(taskStore, task)
  }

  companion object {
    private fun rewordArgs(task: Task, label: String): CommonArguments<RewordArguments> {
      return commonArgs(RewordArguments(task, label))
    }
  }
}