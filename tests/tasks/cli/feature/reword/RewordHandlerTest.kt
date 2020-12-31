package tasks.cli.feature.reword

import com.google.common.truth.Truth
import omnia.data.cache.Memoized.Companion.just
import omnia.data.structure.immutable.ImmutableList
import org.junit.Test
import org.junit.jupiter.api.Assertions
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import tasks.cli.command.common.CommonArguments
import tasks.cli.handler.HandlerException
import tasks.cli.handler.testing.HandlerTestUtils
import tasks.model.ObservableTaskStore
import tasks.model.Task
import tasks.model.impl.ObservableTaskStoreImpl.Companion.createInMemoryStorage

@RunWith(JUnit4::class)
class RewordHandlerTest {
  private val taskStore: ObservableTaskStore = createInMemoryStorage()
  private val underTest = RewordHandler(just(taskStore))

  @Test
  fun handle_withEmptyDescription_throwsException_doesNotModifyLabel() {
    val task = createTask("example task")
    Assertions.assertThrows(HandlerException::class.java) { underTest.handle(rewordArgs(task, "  ")) }
    Truth.assertThat(getUpdatedVersionOf(task).label()).isEqualTo("example task")
  }

  @Test
  fun handle_withNewDescription_renamesTask() {
    val task = createTask("example task")
    underTest.handle(rewordArgs(task, "reworded task")).ignoreElement().blockingAwait()
    Truth.assertThat(getUpdatedVersionOf(task).label()).isEqualTo("reworded task")
  }

  @Test
  fun handle_withNewDescription_outputsRewordedTask() {
    val task = createTask("example task")
    val output = underTest.handle(rewordArgs(task, "reworded task")).blockingGet().toString()
    HandlerTestUtils.assertOutputContainsGroupedTasks(
        output, "Updated description:", ImmutableList.of(getUpdatedVersionOf(task)))
    Truth.assertThat(output).doesNotContain("example task")
  }

  private fun createTask(label: String): Task {
    return HandlerTestUtils.createTask(taskStore, label)
  }

  private fun getUpdatedVersionOf(task: Task): Task {
    return HandlerTestUtils.getUpdatedVersionOf(taskStore, task)
  }

  companion object {
    private fun rewordArgs(task: Task, label: String): CommonArguments<RewordArguments> {
      return HandlerTestUtils.commonArgs(RewordArguments(task, label))
    }
  }
}