package tasks.cli.feature.add;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static tasks.cli.handler.testing.HandlerTestUtils.commonArgs;

import omnia.data.cache.Memoized;
import omnia.data.structure.immutable.ImmutableList;
import omnia.data.structure.immutable.ImmutableSet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import tasks.cli.command.common.CommonArguments;
import tasks.cli.handler.HandlerException;
import tasks.model.ObservableTaskStore;
import tasks.model.Task;
import tasks.model.TaskStore;
import tasks.model.impl.ObservableTaskStoreImpl;

@RunWith(JUnit4.class)
public class AddHandlerTest {

  private final ObservableTaskStore taskStore = ObservableTaskStoreImpl.createInMemoryStorage();

  private final AddHandler underTest = new AddHandler(Memoized.just(taskStore));

  @Test
  public void handle_withEmptyDescription_throwsHandlerException() {
    assertThrows(HandlerException.class, () -> underTest.handle(addArgs("")));
  }

  @Test
  public void handle_butNotSubscribed_doesNotCreateTask() {
    underTest.handle(addArgs("example task"));

    assertThat(taskStore.observe().blockingFirst().allTasks().isPopulated()).isFalse();
  }

  @Test
  public void handle_plainTask_addsTask() {
    underTest.handle(addArgs("example task")).ignoreElement().blockingAwait();

    ImmutableSet<? extends Task> allTasks = taskStore.observe().blockingFirst().allTasks();
    assertThat(allTasks.count()).isEqualTo(1);

    Task addedTask = allTasks.iterator().next();
    assertThat(addedTask.label()).isEqualTo("example task");
    assertThat(addedTask.blockingTasks()).isEmpty();
    assertThat(addedTask.blockedTasks()).isEmpty();
    assertThat(addedTask.status()).isEqualTo(Task.Status.OPEN);
  }

  @Test
  public void handle_plainTask_outputsTask() {
    String output = underTest.handle(addArgs("example task")).blockingGet().toString();

    assertThat(output).contains("task created");
    assertThat(output).contains("example task");
  }

  @Test
  public void handle_taskWithBlocker_addsTask() {
    Task blockingTask = taskStore.createTask("blocking task", b -> b).blockingGet().third();

    underTest.handle(argsWithBlockingTasks("new task", blockingTask))
        .ignoreElement()
        .blockingAwait();

    TaskStore taskStoreState = taskStore.observe().blockingFirst();
    ImmutableSet<? extends Task> allTasks = taskStoreState.allTasks();
    assertThat(allTasks.count()).isEqualTo(2);

    ImmutableSet<? extends Task> tasksWithBlockers = taskStoreState.allOpenTasksWithOpenBlockers();
    assertThat(tasksWithBlockers.count()).isEqualTo(1);

    Task addedTask = tasksWithBlockers.iterator().next();
    assertThat(addedTask.label()).isEqualTo("new task");
    assertThat(addedTask.blockingTasks().count()).isEqualTo(1);
    assertThat(addedTask.blockingTasks().iterator().next().id()).isEqualTo(blockingTask.id());
    assertThat(addedTask.blockedTasks()).isEmpty();
    assertThat(addedTask.status()).isEqualTo(Task.Status.OPEN);
  }

  @Test
  public void handle_taskWithBlockee_addsTask() {
    Task blockedTask = taskStore.createTask("blocked task", b -> b).blockingGet().third();

    underTest.handle(argsWithBlockedTasks("new task", blockedTask))
        .ignoreElement()
        .blockingAwait();

    TaskStore taskStoreState = taskStore.observe().blockingFirst();
    ImmutableSet<? extends Task> allTasks = taskStoreState.allTasks();
    assertThat(allTasks.count()).isEqualTo(2);

    ImmutableSet<? extends Task> tasksWithoutBlockers =
        taskStoreState.allOpenTasksWithoutOpenBlockers();
    assertThat(tasksWithoutBlockers.count()).isEqualTo(1);

    Task addedTask = tasksWithoutBlockers.iterator().next();
    assertThat(addedTask.label()).isEqualTo("new task");
    assertThat(addedTask.blockingTasks()).isEmpty();
    assertThat(addedTask.blockedTasks().count()).isEqualTo(1);
    assertThat(addedTask.blockedTasks().iterator().next().id()).isEqualTo(blockedTask.id());
    assertThat(addedTask.status()).isEqualTo(Task.Status.OPEN);
  }

  @Test
  public void handle_taskWithBlockerAndBlockee_addsTask() {
    Task blockingTask = taskStore.createTask("blocking task", b -> b).blockingGet().third();
    Task blockedTask = taskStore.createTask("blocked task", b -> b).blockingGet().third();

    underTest.handle(
        commonArgs(new AddArguments(
            "new task", ImmutableList.of(blockingTask), ImmutableList.of(blockedTask))))
        .ignoreElement()
        .blockingAwait();

    TaskStore taskStoreState = taskStore.observe().blockingFirst();
    ImmutableSet<? extends Task> allTasks = taskStoreState.allTasks();
    assertThat(allTasks.count()).isEqualTo(3);

    ImmutableSet<? extends Task> tasksWithoutBlockers =
        taskStoreState.allOpenTasksWithoutOpenBlockers();
    assertThat(tasksWithoutBlockers.count()).isEqualTo(1);

    Task firstTask = tasksWithoutBlockers.iterator().next();
    assertThat(firstTask.id()).isEqualTo(blockingTask.id());

    Task addedTask = firstTask.blockedTasks().iterator().next();
    assertThat(addedTask.label()).isEqualTo("new task");
    assertThat(addedTask.blockingTasks().count()).isEqualTo(1);
    assertThat(addedTask.blockedTasks().count()).isEqualTo(1);
    assertThat(addedTask.status()).isEqualTo(Task.Status.OPEN);

    Task thirdTask = addedTask.blockedTasks().iterator().next();
    assertThat(thirdTask.id()).isEqualTo(blockedTask.id());
  }

  @Test
  public void handle_whenCreatingCircularDependency_fails() {
    Task blockingTask = taskStore.createTask("blocking task", b -> b).blockingGet().third();
    Task blockedTask = taskStore.createTask("blocked task", b -> b.addBlockedTask(blockingTask))
        .blockingGet().third();

    underTest.handle(
        commonArgs(new AddArguments(
            "new task", ImmutableList.of(blockingTask), ImmutableList.of(blockedTask))))
        .ignoreElement()
        .test()
        .assertError(IllegalStateException.class);
  }

  private static CommonArguments<AddArguments> addArgs(String label) {
    return commonArgs(new AddArguments(label, ImmutableList.empty(), ImmutableList.empty()));
  }

  private static CommonArguments<AddArguments> argsWithBlockedTasks(
      String label, Task... blockees) {
    return commonArgs(
        new AddArguments(label, ImmutableList.empty(), ImmutableList.copyOf(blockees)));
  }

  private static CommonArguments<AddArguments> argsWithBlockingTasks(
      String label, Task... blockers) {
    return commonArgs(
        new AddArguments(label, ImmutableList.copyOf(blockers), ImmutableList.empty()));
  }

}