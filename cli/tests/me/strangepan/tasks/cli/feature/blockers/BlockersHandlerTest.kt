package me.strangepan.tasks.cli.feature.blockers

import com.google.common.truth.Truth.assertThat
import java.util.function.Function
import omnia.cli.out.Output.Companion.empty
import omnia.data.cache.Memoized.Companion.just
import omnia.data.structure.immutable.ImmutableList
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import me.strangepan.tasks.cli.handler.HandlerException
import me.strangepan.tasks.cli.handler.testing.HandlerTestUtils
import me.strangepan.tasks.cli.handler.testing.HandlerTestUtils.assertOutputContainsGroupedTasks
import me.strangepan.tasks.engine.model.ObservableTaskStore
import me.strangepan.tasks.engine.model.Task
import me.strangepan.tasks.engine.model.TaskBuilder
import me.strangepan.tasks.engine.model.impl.ObservableTaskStoreImpl.Companion.createInMemoryStorage

@RunWith(JUnit4::class)
class BlockersHandlerTest {
  private val taskStore: ObservableTaskStore = createInMemoryStorage()
  private val underTest = BlockersHandler(just(taskStore))

  @Test
  fun handle_whenBlockingSelf_throwsException() {
    val existingTask = createTask("existing task")
    assertThrows(HandlerException::class.java) {
      underTest.handle(
          HandlerTestUtils.commonArgs(BlockersArguments(
              ImmutableList.of(existingTask),
              ImmutableList.of(existingTask),
              ImmutableList.empty(),
              /* clearAllBlockers= */ false)))
    }
  }

  @Test
  fun handle_whenRemovingAndAddingSameTask_throwsException() {
    val existingTask = createTask("existing task")
    val targetTask = createTask("target task")
    assertThrows(HandlerException::class.java) {
      underTest.handle(
          HandlerTestUtils.commonArgs(BlockersArguments(
              ImmutableList.of(targetTask),
              ImmutableList.of(existingTask),
              ImmutableList.of(existingTask),
              /* clearAllBlockers= */ false)))
    }
  }

  @Test
  fun handle_noChanges_outputsNothing() {
    val targetTask = createTask("target task")

    underTest.handle(
        HandlerTestUtils.commonArgs(BlockersArguments(
            ImmutableList.of(targetTask),
            ImmutableList.empty(),
            ImmutableList.empty(),
            /* clearAllBlockers= */ false)))
        .test()
        .assertValue(empty())
  }

  @Test
  fun handle_clearAllBlockers_clearsExistingBlockers() {
    val existingTask = createTask("existing task")
    val targetTask = createTask("target task") { it.addBlockingTask(existingTask) }

    underTest.handle(
        HandlerTestUtils.commonArgs(BlockersArguments(
            ImmutableList.of(targetTask),
            ImmutableList.empty(),
            ImmutableList.empty(),
            /* clearAllBlockers= */ true)))
        .ignoreElement()
        .blockingAwait()

    assertThat(
      taskStore.observe()
        .firstOrError()
        .blockingGet()
        .lookUpById(targetTask.id)
        .orElseThrow()
        .blockingTasks
        .isPopulated
    )
        .isFalse()
  }

  @Test
  fun handle_clearAllBlockers_outputsRemovedBlockers() {
    val existingTask1 = createTask("existing task")
    val existingTask2 = createTask("existing task")
    val targetTask = createTask("target task") {
      it.addBlockingTask(existingTask1).addBlockingTask(existingTask2)
    }

    val output = underTest.handle(
        HandlerTestUtils.commonArgs(BlockersArguments(
            ImmutableList.of(targetTask),
            ImmutableList.empty(),
            ImmutableList.empty(),
            /* clearAllBlockers= */ true)))
        .blockingGet()
        .toString()

    assertThat(output).contains(targetTask.label)
    assertOutputContainsGroupedTasks(
        output, "removed blockers:", existingTask1, existingTask2)
    assertThat(output).doesNotContain("current blockers")
  }

  @Test
  fun handle_removeBlocker_blockerIsRemoved() {
    val existingTask = createTask("existing task")
    val targetTask = createTask("target task") { it.addBlockingTask(existingTask) }

    underTest.handle(
        HandlerTestUtils.commonArgs(BlockersArguments(
            ImmutableList.of(targetTask),
            ImmutableList.empty(),
            ImmutableList.of(existingTask),
            /* clearAllBlockers= */ false)))
        .ignoreElement()
        .blockingAwait()

    assertThat(
      taskStore.observe()
        .firstOrError()
        .blockingGet()
        .lookUpById(targetTask.id)
        .orElseThrow()
        .blockingTasks
        .isPopulated
    )
        .isFalse()
  }

  @Test
  fun handle_removeBlocker_outputsRemovedBlocker() {
    val existingTask = createTask("existing task")
    val targetTask = createTask("target task") { it.addBlockingTask(existingTask) }

    val output = underTest.handle(
        HandlerTestUtils.commonArgs(BlockersArguments(
            ImmutableList.of(targetTask),
            ImmutableList.empty(),
            ImmutableList.of(existingTask),
            /* clearAllBlockers= */ false)))
        .blockingGet()
        .toString()

    assertThat(output).startsWith(targetTask.render().renderWithoutCodes())
    assertOutputContainsGroupedTasks(output, "removed blockers:", existingTask)
    assertThat(output).doesNotContain("current blockers")
  }

  @Test
  fun handle_addBlocker_blockerIsAdded() {
    val existingTask = createTask("existing task")
    val targetTask = createTask("target task")

    underTest.handle(
        HandlerTestUtils.commonArgs(BlockersArguments(
            ImmutableList.of(targetTask),
            ImmutableList.of(existingTask),
            ImmutableList.empty(),
            /* clearAllBlockers= */ false)))
        .ignoreElement()
        .blockingAwait()

    val updatedTask = taskStore.observe()
        .firstOrError()
        .blockingGet()
        .lookUpById(targetTask.id)
        .orElseThrow()
    assertThat(updatedTask.blockingTasks.count()).isEqualTo(1)
    assertThat(updatedTask.blockingTasks.iterator().next().id).isEqualTo(existingTask.id)
  }

  @Test
  fun handle_whenMultipleTargets_addBlocker_blockerIsAdded() {
    val blockedTask = createTask("existing task")
    val targetTask1 = createTask("target task 1")
    val targetTask2 = createTask("target task 2")
    val targetTask3 = createTask("target task 3")

    underTest.handle(
        HandlerTestUtils.commonArgs(BlockersArguments(
            ImmutableList.of(targetTask1, targetTask2, targetTask3),
            ImmutableList.of(blockedTask),
            ImmutableList.empty(),
            /* clearAllBlockers= */ false)))
        .ignoreElement()
        .blockingAwait()

    val updatedTask1 = HandlerTestUtils.getUpdatedVersionOf(taskStore, targetTask1)
    val updatedTask2 = HandlerTestUtils.getUpdatedVersionOf(taskStore, targetTask2)
    val updatedTask3 = HandlerTestUtils.getUpdatedVersionOf(taskStore, targetTask3)
    assertThat(updatedTask1.blockingTasks.map { it.id }).containsExactly(blockedTask.id)
    assertThat(updatedTask2.blockingTasks.map { it.id }).containsExactly(blockedTask.id)
    assertThat(updatedTask3.blockingTasks.map { it.id }).containsExactly(blockedTask.id)
  }

  @Test
  fun handle_addBlocker_outputsNewBlocker() {
    val existingTask = createTask("existing task")
    val targetTask = createTask("target task")

    val output = underTest.handle(
        HandlerTestUtils.commonArgs(BlockersArguments(
            ImmutableList.of(targetTask),
            ImmutableList.of(existingTask),
            ImmutableList.empty(),
            /* clearAllBlockers= */ false)))
        .blockingGet()
        .toString()

    assertThat(output).startsWith(targetTask.render().renderWithoutCodes())
    assertOutputContainsGroupedTasks(output, "current blockers:", existingTask)
    assertThat(output).doesNotContain("removed blockers")
  }

  @Test
  fun handle_addBlocker_removeBlocker_blockersAreAddedAndRemoved() {
    val existingBlocker = createTask("existing blocker")
    val blockerToAdd = createTask("blocker to add")
    val targetTask = createTask("target task") { it.addBlockingTask(existingBlocker) }

    underTest.handle(
        HandlerTestUtils.commonArgs(BlockersArguments(
            ImmutableList.of(targetTask),
            ImmutableList.of(blockerToAdd),
            ImmutableList.of(existingBlocker),
            /* clearAllBlockers= */ false)))
        .ignoreElement()
        .blockingAwait()

    val updatedTask = taskStore.observe()
        .firstOrError()
        .blockingGet()
        .lookUpById(targetTask.id)
        .orElseThrow()
    assertThat(updatedTask.blockingTasks.count()).isEqualTo(1)
    assertThat(updatedTask.blockingTasks.iterator().next().id).isEqualTo(blockerToAdd.id)
  }

  @Test
  fun handle_addBlocker_removeBlocker_outputsAllTasks() {
    val existingBlocker = createTask("existing blocker")
    val blockerToAdd = createTask("blocker to add")
    val targetTask = createTask("target task") { it.addBlockingTask(existingBlocker) }

    val output = underTest.handle(
        HandlerTestUtils.commonArgs(BlockersArguments(
            ImmutableList.of(targetTask),
            ImmutableList.of(blockerToAdd),
            ImmutableList.of(existingBlocker),
            /* clearAllBlockers= */ false)))
        .blockingGet()
        .toString()

    assertThat(output).startsWith(targetTask.render().renderWithoutCodes())
    assertOutputContainsGroupedTasks(output, "removed blockers:", existingBlocker)
    assertOutputContainsGroupedTasks(output, "current blockers:", blockerToAdd)
  }

  @Test
  fun handle_addBlocker_removeBlocker_withoutSubscribing_doesNothing() {
    val existingBlocker = createTask("existing blocker")
    val blockerToAdd = createTask("blocker to add")
    val targetTask = createTask("target task") { it.addBlockingTask(existingBlocker) }

    underTest.handle(
        HandlerTestUtils.commonArgs(BlockersArguments(
            ImmutableList.of(targetTask),
            ImmutableList.of(blockerToAdd),
            ImmutableList.of(existingBlocker),
            /* clearAllBlockers= */ false)))

    val unchangedTask = taskStore.observe()
        .firstOrError()
        .blockingGet()
        .lookUpById(targetTask.id)
        .orElseThrow()
    assertThat(unchangedTask.blockingTasks.count()).isEqualTo(1)
    assertThat(unchangedTask.blockingTasks.iterator().next().id)
        .isEqualTo(existingBlocker.id)
  }

  @Test
  fun handle_addBlocker_whenResultsInCircularDependency_emitsError() {
    val blockingTask = createTask("blocker to add")
    val blockedTask = createTask("existing blocker") { it.addBlockedTask(blockingTask) }
    val targetTask = createTask("target task") { it.addBlockedTask(blockedTask) }

    underTest.handle(
        HandlerTestUtils.commonArgs(BlockersArguments(
            ImmutableList.of(targetTask),
            ImmutableList.of(blockingTask),
            ImmutableList.empty(),
            /* clearAllBlockers= */ false)))
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