package me.strangepan.tasks.engine.model.impl

import com.google.common.truth.Truth
import java.util.concurrent.TimeUnit
import me.strangepan.tasks.engine.model.impl.ObservableTaskStoreImpl.Companion.createInMemoryStorage
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import me.strangepan.tasks.engine.model.Task

@RunWith(JUnit4::class)
class ObservableTaskStoreImplTest {
  private val underTest = createInMemoryStorage()

  @Test
  fun observe_whenEmpty_emitsEmptyStore() {
    underTest.observe()
        .test()
        .assertNotComplete()
        .assertValueCount(1)
        .assertValue { store: TaskStoreImpl -> !store.allTasks().isPopulated }
  }

  @Test
  fun observe_whenShutdown_emitsNothingAndCompletes() {
    underTest.shutdown().blockingAwait()
    underTest.observe().test().assertNoValues().assertComplete()
  }

  @Test
  fun observe_thenShutdown_completes() {
    val observer = underTest.observe().test()
    underTest.shutdown().blockingAwait()
    observer.assertValueCount(1).assertComplete()
  }

  @Test
  fun createTask_thenObserve_receivesLatestStateOnly() {
    val task = underTest.createTask("example task") { it }.blockingGet().third()
    underTest.observe().test().assertValue { it.allTasks().contains(task) }
  }

  @Test
  fun createTask_whenOpen_isOpen() {
    val task = underTest.createTask("example task") { it.setStatus(Task.Status.OPEN) }
        .blockingGet().third()
    Truth.assertThat(task.status.isOpen).isTrue()
    underTest.observe().test().assertValue { it.lookUpById(task.id).orElseThrow().status.isOpen }
  }

  @Test
  fun createTask_whenCompleted_isCompleted() {
    val task = underTest.createTask("example task") { it.setStatus(Task.Status.COMPLETED) }
        .blockingGet().third()
    Truth.assertThat(task.status.isCompleted).isTrue()
    underTest.observe().test().assertValue { it.lookUpById(task.id).orElseThrow().status.isCompleted }
  }

  @Test
  fun createTask_whenStarted_isStarted() {
    val task = underTest.createTask("example task") { it.setStatus(Task.Status.STARTED) }
        .blockingGet().third()
    Truth.assertThat(task.status.isStarted).isTrue()
    underTest.observe().test().assertValue { it.lookUpById(task.id).orElseThrow().status.isStarted }
  }

  @Test
  fun createTask_whenCircularDependency_fails() {
    val task: Task = underTest.createTask("example task 1") { it }.blockingGet().third()
    underTest.createTask("example task 2") { it.addBlockingTask(task).addBlockedTask(task) }
        .test()
        .assertError(CyclicalDependencyException::class.java)
  }

  @Test
  fun addItem_thenRemove_isEmpty() {
    val task = underTest.createTask("example task") { it }.blockingGet().third()
    underTest.deleteTask(task)
    underTest.observe().test().assertValue { !it.allTasks().isPopulated }
  }

  @Test
  fun mutateTask_whenReword_rewordsTask() {
    val task: Task = underTest.createTask("example task") { it }.blockingGet().third()
    val mutatedTask: Task = underTest.mutateTask(task) { it.setLabel("modified task") }.blockingGet().third()
    Truth.assertThat(mutatedTask.label).isEqualTo("modified task")
  }

  @Test
  fun mutateTask_whenReword_emitsNewState() {
    val task: Task = underTest.createTask("example task") { it }.blockingGet().third()
    val observer = underTest.observe().test()
    val mutatedTask: Task = underTest.mutateTask(task) { it.setLabel("modified task") }.blockingGet().third()
    observer.assertValueCount(2)
        .assertValueAt(1) { it.lookUpById(mutatedTask.id).orElseThrow().label == "modified task" }
  }

  @Test
  fun mutateTask_whenBlocksSelf_emitsError() {
    val task: Task = underTest.createTask("example task") { it }.blockingGet().third()
    underTest.mutateTask(task) { it.addBlockingTask(task) }
        .test()
        .assertError(CyclicalDependencyException::class.java)
  }

  @Test
  fun mutateTask_whenCyclical_emitsError() {
    val task1: Task = underTest.createTask("example task") { it }.blockingGet().third()
    val task2: Task = underTest.createTask("example task") { it }.blockingGet().third()
    underTest.mutateTask(task2) {it.addBlockingTask(task1).addBlockedTask(task1) }
        .test()
        .assertError(CyclicalDependencyException::class.java)
  }

  @Test
  fun mutateTask_whenCyclical_doesNotBreakObservers() {
    val task1: Task = underTest.createTask("example task") { it }.blockingGet().third()
    val task2: Task = underTest.createTask("example task") { it }.blockingGet().third()
    val testObserver = underTest.observe().test()
    underTest.mutateTask(task2) { it.addBlockingTask(task1).addBlockedTask(task1) }.test()
        .awaitDone(1, TimeUnit.SECONDS)
    testObserver.assertValueCount(1).assertNoErrors().assertNotComplete()
  }

  @Test
  fun mutateTask_emitsBeforeAndAfterStates() {
    val originalTask: Task = underTest.createTask("example task") { it }.blockingGet().third()
    val originalStore = underTest.observe().blockingFirst()
    val mutationResult = underTest.mutateTask(originalTask) { it.setLabel("modified task") }.blockingGet()
    Truth.assertThat(mutationResult.first()).isEqualTo(originalStore)
    Truth.assertThat(mutationResult.second()).isEqualTo(underTest.observe().blockingFirst())
    Truth.assertThat(mutationResult.second()).isNotEqualTo(mutationResult.first())
    Truth.assertThat(mutationResult.second().allTasks()).contains(mutationResult.third())
    Truth.assertThat(mutationResult.second().allTasks()).doesNotContain(originalTask)
  }
}