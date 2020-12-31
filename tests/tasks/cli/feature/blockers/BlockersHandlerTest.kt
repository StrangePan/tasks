package tasks.cli.feature.blockers

import com.google.common.truth.Truth
import java.util.function.Function
import omnia.cli.out.Output.Companion.empty
import omnia.data.cache.Memoized.Companion.just
import omnia.data.structure.immutable.ImmutableList
import org.junit.Test
import org.junit.jupiter.api.Assertions
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import tasks.cli.handler.HandlerException
import tasks.cli.handler.testing.HandlerTestUtils
import tasks.model.ObservableTaskStore
import tasks.model.Task
import tasks.model.TaskBuilder
import tasks.model.impl.ObservableTaskStoreImpl.Companion.createInMemoryStorage

@RunWith(JUnit4::class)
class BlockersHandlerTest {
  private val taskStore: ObservableTaskStore = createInMemoryStorage()
  private val underTest = BlockersHandler(just(taskStore))

  @Test
  fun handle_whenBlockingSelf_throwsException() {
    val existingTask = createTask("existing task")
    Assertions.assertThrows(
        HandlerException::class.java
    ) {
      underTest.handle(
          HandlerTestUtils.commonArgs(BlockersArguments(
              existingTask,
              ImmutableList.of(existingTask),
              ImmutableList.empty(),  /* clearAllBlockers= */
              false)))
    }
  }

  @Test
  fun handle_whenRemovingAndAddingSameTask_throwsException() {
    val existingTask = createTask("existing task")
    val targetTask = createTask("target task")
    Assertions.assertThrows(
        HandlerException::class.java
    ) {
      underTest.handle(
          HandlerTestUtils.commonArgs(BlockersArguments(
              targetTask,
              ImmutableList.of(existingTask),
              ImmutableList.of(existingTask),  /* clearAllBlockers= */
              false)))
    }
  }

  @Test
  fun handle_noChanges_outputsNothing() {
    val targetTask = createTask("target task")

    /* clearAllBlockers= */underTest.handle(
        HandlerTestUtils.commonArgs(BlockersArguments(
            targetTask,
            ImmutableList.empty(),
            ImmutableList.empty(),  /* clearAllBlockers= */
            false)))
        .test()
        .assertValue(empty())
  }

  @Test
  fun handle_clearAllBlockers_clearsExistingBlockers() {
    val existingTask = createTask("existing task")
    val targetTask = createTask("target task") { b: TaskBuilder -> b.addBlockingTask(existingTask) }

    /* clearAllBlockers= */underTest.handle(
        HandlerTestUtils.commonArgs(BlockersArguments(
            targetTask,
            ImmutableList.empty(),
            ImmutableList.empty(),  /* clearAllBlockers= */
            true)))
        .ignoreElement()
        .blockingAwait()
    Truth.assertThat(
        taskStore.observe()
            .firstOrError()
            .blockingGet()
            .lookUpById(targetTask.id())
            .orElseThrow()
            .blockingTasks()
            .isPopulated)
        .isFalse()
  }

  @Test
  fun handle_clearAllBlockers_outputsRemovedBlockers() {
    val existingTask1 = createTask("existing task")
    val existingTask2 = createTask("existing task")
    val targetTask = createTask(
        "target task") { b: TaskBuilder -> b.addBlockingTask(existingTask1).addBlockingTask(existingTask2) }

    /* clearAllBlockers= */
    val output = underTest.handle(
        HandlerTestUtils.commonArgs(BlockersArguments(
            targetTask,
            ImmutableList.empty(),
            ImmutableList.empty(),  /* clearAllBlockers= */
            true)))
        .blockingGet()
        .toString()
    Truth.assertThat(output).contains(targetTask.label())
    HandlerTestUtils.assertOutputContainsGroupedTasks(
        output, "removed blockers:", ImmutableList.of(existingTask1, existingTask2))
    Truth.assertThat(output).doesNotContain("current blockers")
  }

  @Test
  fun handle_removeBlocker_blockerIsRemoved() {
    val existingTask = createTask("existing task")
    val targetTask = createTask("target task") { b: TaskBuilder -> b.addBlockingTask(existingTask) }

    /* clearAllBlockers= */underTest.handle(
        HandlerTestUtils.commonArgs(BlockersArguments(
            targetTask,
            ImmutableList.empty(),
            ImmutableList.of(existingTask),  /* clearAllBlockers= */
            false)))
        .ignoreElement()
        .blockingAwait()
    Truth.assertThat(
        taskStore.observe()
            .firstOrError()
            .blockingGet()
            .lookUpById(targetTask.id())
            .orElseThrow()
            .blockingTasks()
            .isPopulated)
        .isFalse()
  }

  @Test
  fun handle_removeBlocker_outputsRemovedBlocker() {
    val existingTask = createTask("existing task")
    val targetTask = createTask("target task") { b: TaskBuilder -> b.addBlockingTask(existingTask) }

    /* clearAllBlockers= */
    val output = underTest.handle(
        HandlerTestUtils.commonArgs(BlockersArguments(
            targetTask,
            ImmutableList.empty(),
            ImmutableList.of(existingTask),  /* clearAllBlockers= */
            false)))
        .blockingGet()
        .toString()
    Truth.assertThat(output).startsWith(targetTask.render().renderWithoutCodes())
    HandlerTestUtils.assertOutputContainsGroupedTasks(output, "removed blockers:", ImmutableList.of(existingTask))
    Truth.assertThat(output).doesNotContain("current blockers")
  }

  @Test
  fun handle_addBlocker_blockerIsAdded() {
    val existingTask = createTask("existing task")
    val targetTask = createTask("target task")

    /* clearAllBlockers= */underTest.handle(
        HandlerTestUtils.commonArgs(BlockersArguments(
            targetTask,
            ImmutableList.of(existingTask),
            ImmutableList.empty(),  /* clearAllBlockers= */
            false)))
        .ignoreElement()
        .blockingAwait()
    val updatedTask = taskStore.observe()
        .firstOrError()
        .blockingGet()
        .lookUpById(targetTask.id())
        .orElseThrow()
    Truth.assertThat(updatedTask.blockingTasks().count()).isEqualTo(1)
    Truth.assertThat(updatedTask.blockingTasks().iterator().next().id()).isEqualTo(existingTask.id())
  }

  @Test
  fun handle_addBlocker_outputsNewBlocker() {
    val existingTask = createTask("existing task")
    val targetTask = createTask("target task")

    /* clearAllBlockers= */
    val output = underTest.handle(
        HandlerTestUtils.commonArgs(BlockersArguments(
            targetTask,
            ImmutableList.of(existingTask),
            ImmutableList.empty(),  /* clearAllBlockers= */
            false)))
        .blockingGet()
        .toString()
    Truth.assertThat(output).startsWith(targetTask.render().renderWithoutCodes())
    HandlerTestUtils.assertOutputContainsGroupedTasks(output, "current blockers:", ImmutableList.of(existingTask))
    Truth.assertThat(output).doesNotContain("removed blockers")
  }

  @Test
  fun handle_addBlocker_removeBlocker_blockersAreAddedAndRemoved() {
    val existingBlocker = createTask("existing blocker")
    val blockerToAdd = createTask("blocker to add")
    val targetTask = createTask("target task") { b: TaskBuilder -> b.addBlockingTask(existingBlocker) }

    /* clearAllBlockers= */underTest.handle(
        HandlerTestUtils.commonArgs(BlockersArguments(
            targetTask,
            ImmutableList.of(blockerToAdd),
            ImmutableList.of(existingBlocker),  /* clearAllBlockers= */
            false)))
        .ignoreElement()
        .blockingAwait()
    val updatedTask = taskStore.observe()
        .firstOrError()
        .blockingGet()
        .lookUpById(targetTask.id())
        .orElseThrow()
    Truth.assertThat(updatedTask.blockingTasks().count()).isEqualTo(1)
    Truth.assertThat(updatedTask.blockingTasks().iterator().next().id()).isEqualTo(blockerToAdd.id())
  }

  @Test
  fun handle_addBlocker_removeBlocker_outputsAllTasks() {
    val existingBlocker = createTask("existing blocker")
    val blockerToAdd = createTask("blocker to add")
    val targetTask = createTask("target task") { b: TaskBuilder -> b.addBlockingTask(existingBlocker) }

    /* clearAllBlockers= */
    val output = underTest.handle(
        HandlerTestUtils.commonArgs(BlockersArguments(
            targetTask,
            ImmutableList.of(blockerToAdd),
            ImmutableList.of(existingBlocker),  /* clearAllBlockers= */
            false)))
        .blockingGet()
        .toString()
    Truth.assertThat(output).startsWith(targetTask.render().renderWithoutCodes())
    HandlerTestUtils.assertOutputContainsGroupedTasks(
        output, "removed blockers:", ImmutableList.of(existingBlocker))
    HandlerTestUtils.assertOutputContainsGroupedTasks(output, "current blockers:", ImmutableList.of(blockerToAdd))
  }

  @Test
  fun handle_addBlocker_removeBlocker_withoutSubscribing_doesNothing() {
    val existingBlocker = createTask("existing blocker")
    val blockerToAdd = createTask("blocker to add")
    val targetTask = createTask("target task") { b: TaskBuilder -> b.addBlockingTask(existingBlocker) }

    /* clearAllBlockers= */underTest.handle(
        HandlerTestUtils.commonArgs(BlockersArguments(
            targetTask,
            ImmutableList.of(blockerToAdd),
            ImmutableList.of(existingBlocker),  /* clearAllBlockers= */
            false)))
    val unchangedTask = taskStore.observe()
        .firstOrError()
        .blockingGet()
        .lookUpById(targetTask.id())
        .orElseThrow()
    Truth.assertThat(unchangedTask.blockingTasks().count()).isEqualTo(1)
    Truth.assertThat(unchangedTask.blockingTasks().iterator().next().id())
        .isEqualTo(existingBlocker.id())
  }

  @Test
  fun handle_addBlocker_whenResultsInCircularDependency_emitsError() {
    val blockerToAdd = createTask("blocker to add")
    val blockee = createTask("existing blocker") { b: TaskBuilder -> b.addBlockedTask(blockerToAdd) }
    val targetTask = createTask("target task") { b: TaskBuilder -> b.addBlockedTask(blockee) }

    /* clearAllBlockers= */underTest.handle(
        HandlerTestUtils.commonArgs(BlockersArguments(
            targetTask,
            ImmutableList.of(blockerToAdd),
            ImmutableList.empty(),  /* clearAllBlockers= */
            false)))
        .test()
        .assertError(IllegalStateException::class.java)
  }

  private fun createTask(label: String): Task {
    return HandlerTestUtils.createTask(taskStore, label)
  }

  private fun createTask(label: String, builderFunction: Function<TaskBuilder, TaskBuilder>): Task {
    return HandlerTestUtils.createTask(taskStore, label, builderFunction)
  }
}