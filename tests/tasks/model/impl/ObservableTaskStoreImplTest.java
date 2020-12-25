package tasks.model.impl;

import static com.google.common.truth.Truth.assertThat;

import io.reactivex.observers.TestObserver;
import omnia.data.structure.tuple.Triple;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import tasks.model.Task;
import tasks.model.TaskStore;

@RunWith(JUnit4.class)
public final class ObservableTaskStoreImplTest {

  private final ObservableTaskStoreImpl underTest = ObservableTaskStoreImpl.createInMemoryStorage();

  @Test
  public void observe_whenEmpty_emitsEmptyStore() {
    underTest.observe()
        .test()
        .assertNotComplete()
        .assertValueCount(1)
        .assertValue(store -> !store.allTasks().isPopulated());
  }

  @Test
  public void observe_whenShutdown_emitsNothingAndCompletes() {
    underTest.shutdown().blockingAwait();

    underTest.observe().test().assertNoValues().assertComplete();
  }

  @Test
  public void observe_thenShutdown_completes() {
    TestObserver<TaskStoreImpl> observer = underTest.observe().test();

    underTest.shutdown().blockingAwait();

    observer.assertValueCount(1).assertComplete();
  }

  @Test
  public void createTask_thenObserve_receivesLatestStateOnly() {
    TaskImpl task = underTest.createTask("example task", b -> b).blockingGet().third();

    underTest.observe().test().assertValue(store -> store.allTasks().contains(task));
  }

  @Test
  public void createTask_whenOpen_isOpen() {
    TaskImpl task =
        underTest.createTask("example task", b -> b.setStatus(Task.Status.OPEN))
            .blockingGet().third();

    assertThat(task.status().isOpen()).isTrue();

    underTest.observe().test().assertValue(
        store -> store.lookUpById(task.id()).orElseThrow().status().isOpen());
  }

  @Test
  public void createTask_whenCompleted_isCompleted() {
    TaskImpl task =
        underTest.createTask("example task", b -> b.setStatus(Task.Status.COMPLETED))
            .blockingGet().third();

    assertThat(task.status().isCompleted()).isTrue();

    underTest.observe().test().assertValue(
        store -> store.lookUpById(task.id()).orElseThrow().status().isCompleted());
  }

  @Test
  public void createTask_whenStarted_isStarted() {
    TaskImpl task =
        underTest.createTask("example task", b -> b.setStatus(Task.Status.STARTED))
            .blockingGet().third();

    assertThat(task.status().isStarted()).isTrue();

    underTest.observe().test().assertValue(
        store -> store.lookUpById(task.id()).orElseThrow().status().isStarted());
  }

  @Test
  public void createTask_whenCircularDependency_fails() {
    Task task = underTest.createTask("example task 1", b -> b).blockingGet().third();

    underTest.createTask("example task 2", b -> b.addBlockingTask(task).addBlockedTask(task))
        .test()
        .assertError(CyclicalDependencyException.class);
  }

  @Test
  public void addItem_thenRemove_isEmpty() {
    TaskImpl task = underTest.createTask("example task", b -> b).blockingGet().third();

    underTest.deleteTask(task);

    underTest.observe().test().assertValue(store -> !store.allTasks().isPopulated());
  }

  @Test
  public void mutateTask_whenReword_rewordsTask() {
    Task task = underTest.createTask("example task", b -> b).blockingGet().third();

    Task mutatedTask =
        underTest.mutateTask(task, m -> m.setLabel("modified task")).blockingGet().third();

    assertThat(mutatedTask.label()).isEqualTo("modified task");
  }

  @Test
  public void mutateTask_whenReword_emitsNewState() {
    Task task = underTest.createTask("example task", b -> b).blockingGet().third();

    TestObserver<TaskStoreImpl> observer = underTest.observe().test();

    Task mutatedTask =
        underTest.mutateTask(task, m -> m.setLabel("modified task")).blockingGet().third();

    observer.assertValueCount(2)
        .assertValueAt(
            1,
            store -> store.lookUpById(mutatedTask.id())
                .orElseThrow()
                .label()
                .equals("modified task"));
  }

  @Test
  public void mutateTask_whenBlocksSelf_emitsError() {
    Task task = underTest.createTask("example task", b -> b).blockingGet().third();

    underTest.mutateTask(task, m -> m.addBlockingTask(task))
        .test()
        .assertError(CyclicalDependencyException.class);
  }

  @Test
  public void mutateTask_whenCyclical_emitsError() {
    Task task1 = underTest.createTask("example task", b -> b).blockingGet().third();
    Task task2 = underTest.createTask("example task", b -> b).blockingGet().third();

    underTest.mutateTask(task2, m -> m.addBlockingTask(task1).addBlockedTask(task1))
        .test()
        .assertError(CyclicalDependencyException.class);
  }

  @Test
  public void mutateTask_whenCyclical_doesNotBreakObservers() {
    Task task1 = underTest.createTask("example task", b -> b).blockingGet().third();
    Task task2 = underTest.createTask("example task", b -> b).blockingGet().third();

    TestObserver<TaskStoreImpl> testObserver = underTest.observe().test();

    underTest.mutateTask(task2, m -> m.addBlockingTask(task1).addBlockedTask(task1)).test()
        .awaitTerminalEvent();

    testObserver.assertValueCount(1).assertNoErrors().assertNotComplete();
  }

  @Test
  public void mutateTask_emitsBeforeAndAfterStates() {
    Task originalTask = underTest.createTask("example task", b -> b).blockingGet().third();

    TaskStoreImpl originalStore = underTest.observe().blockingFirst();

    Triple<TaskStoreImpl, TaskStoreImpl, TaskImpl> mutationResult =
        underTest.mutateTask(originalTask, m -> m.setLabel("modified task")).blockingGet();

    assertThat(mutationResult.first()).isEqualTo(originalStore);
    assertThat(mutationResult.second()).isEqualTo(underTest.observe().blockingFirst());
    assertThat(mutationResult.second()).isNotEqualTo(mutationResult.first());
    assertThat(mutationResult.second().allTasks()).contains(mutationResult.third());
    assertThat(mutationResult.second().allTasks()).doesNotContain(originalTask);
  }
}