package me.strangepan.tasks.cli.feature.add

import com.google.common.truth.Truth
import java.util.function.Function
import omnia.data.cache.Memoized.Companion.just
import omnia.data.structure.immutable.ImmutableList
import omnia.data.structure.immutable.ImmutableList.Companion.copyOf
import org.junit.Test
import org.junit.jupiter.api.Assertions
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import me.strangepan.tasks.cli.command.common.CommonArguments
import me.strangepan.tasks.cli.handler.HandlerException
import me.strangepan.tasks.cli.handler.testing.HandlerTestUtils
import me.strangepan.tasks.engine.model.ObservableTaskStore
import me.strangepan.tasks.engine.model.Task
import me.strangepan.tasks.engine.model.TaskBuilder
import me.strangepan.tasks.engine.model.impl.ObservableTaskStoreImpl.Companion.createInMemoryStorage

@RunWith(JUnit4::class)
class AddHandlerTest {
  private val taskStore: ObservableTaskStore = createInMemoryStorage()
  private val underTest = AddHandler(just(taskStore))

  @Test
  fun handle_withEmptyDescription_throwsHandlerException() {
    Assertions.assertThrows(HandlerException::class.java) { underTest.handle(addArgs("")) }
  }

  @Test
  fun handle_butNotSubscribed_doesNotCreateTask() {
    underTest.handle(addArgs("example task"))
    Truth.assertThat(taskStore.observe().blockingFirst().allTasks().isPopulated).isFalse()
  }

  @Test
  fun handle_plainTask_addsTask() {
    underTest.handle(addArgs("example task")).ignoreElement().blockingAwait()
    val allTasks = taskStore.observe().blockingFirst().allTasks()
    Truth.assertThat(allTasks.count()).isEqualTo(1)
    val addedTask = allTasks.iterator().next()
    Truth.assertThat(addedTask.label()).isEqualTo("example task")
    Truth.assertThat(addedTask.blockingTasks()).isEmpty()
    Truth.assertThat(addedTask.blockedTasks()).isEmpty()
    Truth.assertThat(addedTask.status()).isEqualTo(Task.Status.OPEN)
  }

  @Test
  fun handle_plainTask_outputsTask() {
    val output = underTest.handle(addArgs("example task")).blockingGet().toString()
    Truth.assertThat(output).contains("task created")
    Truth.assertThat(output).contains("example task")
  }

  @Test
  fun handle_taskWithBlocker_addsTask() {
    val blockingTask = createTask("blocking task")
    underTest.handle(argsWithBlockingTasks("new task", blockingTask))
        .ignoreElement()
        .blockingAwait()
    val taskStoreState = taskStore.observe().blockingFirst()
    val allTasks = taskStoreState.allTasks()
    Truth.assertThat(allTasks.count()).isEqualTo(2)
    val tasksWithBlockers = taskStoreState.allOpenTasksWithOpenBlockers()
    Truth.assertThat(tasksWithBlockers.count()).isEqualTo(1)
    val addedTask = tasksWithBlockers.iterator().next()
    Truth.assertThat(addedTask.label()).isEqualTo("new task")
    Truth.assertThat(addedTask.blockingTasks().count()).isEqualTo(1)
    Truth.assertThat(addedTask.blockingTasks().iterator().next().id()).isEqualTo(blockingTask.id())
    Truth.assertThat(addedTask.blockedTasks()).isEmpty()
    Truth.assertThat(addedTask.status()).isEqualTo(Task.Status.OPEN)
  }

  @Test
  fun handle_taskWithBlockee_addsTask() {
    val blockedTask = createTask("blocked task")
    underTest.handle(argsWithBlockedTasks("new task", blockedTask))
        .ignoreElement()
        .blockingAwait()
    val taskStoreState = taskStore.observe().blockingFirst()
    val allTasks = taskStoreState.allTasks()
    Truth.assertThat(allTasks.count()).isEqualTo(2)
    val tasksWithoutBlockers = taskStoreState.allOpenTasksWithoutOpenBlockers()
    Truth.assertThat(tasksWithoutBlockers.count()).isEqualTo(1)
    val addedTask = tasksWithoutBlockers.iterator().next()
    Truth.assertThat(addedTask.label()).isEqualTo("new task")
    Truth.assertThat(addedTask.blockingTasks()).isEmpty()
    Truth.assertThat(addedTask.blockedTasks().count()).isEqualTo(1)
    Truth.assertThat(addedTask.blockedTasks().iterator().next().id()).isEqualTo(blockedTask.id())
    Truth.assertThat(addedTask.status()).isEqualTo(Task.Status.OPEN)
  }

  @Test
  fun handle_taskWithBlockerAndBlockee_addsTask() {
    val blockingTask = createTask("blocking task")
    val blockedTask = createTask("blocked task")
    underTest.handle(
        HandlerTestUtils.commonArgs(AddArguments(
            "new task", ImmutableList.of(blockingTask), ImmutableList.of(blockedTask))))
        .ignoreElement()
        .blockingAwait()
    val taskStoreState = taskStore.observe().blockingFirst()
    val allTasks = taskStoreState.allTasks()
    Truth.assertThat(allTasks.count()).isEqualTo(3)
    val tasksWithoutBlockers = taskStoreState.allOpenTasksWithoutOpenBlockers()
    Truth.assertThat(tasksWithoutBlockers.count()).isEqualTo(1)
    val firstTask = tasksWithoutBlockers.iterator().next()
    Truth.assertThat(firstTask.id()).isEqualTo(blockingTask.id())
    val addedTask = firstTask.blockedTasks().iterator().next()
    Truth.assertThat(addedTask.label()).isEqualTo("new task")
    Truth.assertThat(addedTask.blockingTasks().count()).isEqualTo(1)
    Truth.assertThat(addedTask.blockedTasks().count()).isEqualTo(1)
    Truth.assertThat(addedTask.status()).isEqualTo(Task.Status.OPEN)
    val thirdTask = addedTask.blockedTasks().iterator().next()
    Truth.assertThat(thirdTask.id()).isEqualTo(blockedTask.id())
  }

  @Test
  fun handle_whenCreatingCircularDependency_fails() {
    val blockingTask = createTask("blocking task")
    val blockedTask = createTask("blocked task") { it.addBlockedTask(blockingTask) }
    underTest.handle(
        HandlerTestUtils.commonArgs(AddArguments(
            "new task", ImmutableList.of(blockingTask), ImmutableList.of(blockedTask))))
        .ignoreElement()
        .test()
        .assertError(IllegalStateException::class.java)
  }

  private fun createTask(label: String): Task {
    return HandlerTestUtils.createTask(taskStore, label)
  }

  private fun createTask(label: String, builderFunction: Function<TaskBuilder, TaskBuilder>): Task {
    return HandlerTestUtils.createTask(taskStore, label, builderFunction)
  }

  companion object {
    private fun addArgs(label: String): CommonArguments<AddArguments> {
      return HandlerTestUtils.commonArgs(AddArguments(label, ImmutableList.empty(), ImmutableList.empty()))
    }

    private fun argsWithBlockedTasks(
        label: String, vararg blockees: Task): CommonArguments<AddArguments> {
      return HandlerTestUtils.commonArgs(
          AddArguments(label, ImmutableList.empty(), copyOf(blockees)))
    }

    private fun argsWithBlockingTasks(
        label: String, vararg blockers: Task): CommonArguments<AddArguments> {
      return HandlerTestUtils.commonArgs(
          AddArguments(label, copyOf(blockers), ImmutableList.empty()))
    }
  }
}