package tasks.cli.feature.list;

import static com.google.common.truth.Truth.assertThat;
import static java.util.regex.Pattern.DOTALL;
import static tasks.cli.handler.testing.HandlerTestUtils.assertOutputContainsGroupedTasks;
import static tasks.model.Task.Status.COMPLETED;
import static tasks.model.Task.Status.STARTED;

import java.util.function.Function;
import java.util.regex.Pattern;
import omnia.data.cache.Memoized;
import omnia.data.structure.immutable.ImmutableList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import tasks.cli.handler.testing.HandlerTestUtils;
import tasks.cli.command.common.CommonArguments;
import tasks.model.ObservableTaskStore;
import tasks.model.Task;
import tasks.model.TaskBuilder;
import tasks.model.impl.ObservableTaskStoreImpl;

@RunWith(JUnit4.class)
public final class ListHandlerTest {

  public static final String UNBLOCKED_TASKS_HEADER = "unblocked tasks:";
  public static final String BLOCKED_TASKS_HEADER = "blocked tasks:";
  public static final String COMPLETED_TASKS_HEADER = "completed tasks:";

  private final ObservableTaskStore taskStore = ObservableTaskStoreImpl.createInMemoryStorage();

  private final ListHandler underTest = new ListHandler(Memoized.just(taskStore));

  @Test
  public void list_withEmptyGraph_printsNothing() {
    assertThat(underTest.handle(listArgsAll()).blockingGet().toString()).isEmpty();
  }

  @Test
  public void list_withOneItem_printsItem() {
    Task task = createTask("example");

    String output = underTest.handle(listArgsUnblockedOnly()).blockingGet().renderWithoutCodes();

    assertOutputContainsGroupedTasks(output, UNBLOCKED_TASKS_HEADER, ImmutableList.of(task));
  }

  @Test
  public void list_withThreeItems_printsItems() {
    Task task1 = createTask("example 1");
    Task task2 = createTask("example 2");
    Task task3 = createTask("example 3");

    String output = underTest.handle(listArgsUnblockedOnly()).blockingGet().renderWithoutCodes();

    assertOutputContainsGroupedTasks(output, UNBLOCKED_TASKS_HEADER, ImmutableList.of(task1, task2, task3));
  }

  @Test
  public void list_withBlockedItems_withUnblockedOnlyArgs_doesNotPrintBlockedItems() {
    Task task1 = createTask("example task");
    Task blockedTask1 = createTask("blocked task 1", b -> b.addBlockingTask(task1));
    Task blockedTask2 = createTask("blocked task 2", b -> b.addBlockingTask(task1));

    String output = underTest.handle(listArgsUnblockedOnly()).blockingGet().renderWithoutCodes();

    assertOutputContainsGroupedTasks(output, UNBLOCKED_TASKS_HEADER, ImmutableList.of(task1));
    assertThat(output).doesNotContain("^" + Pattern.quote(BLOCKED_TASKS_HEADER));
    assertThat(output).doesNotContain(blockedTask1.render().renderWithoutCodes());
    assertThat(output).doesNotContain(blockedTask2.render().renderWithoutCodes());
  }

  @Test
  public void list_withBlockedItems_withBlockedOnlyArgs_printsOnlyBlockedItems() {
    Task task1 = createTask("example task");
    Task blockedTask1 = createTask("blocked task 1", b -> b.addBlockingTask(task1));
    Task blockedTask2 = createTask("blocked task 2", b -> b.addBlockingTask(task1));

    String output = underTest.handle(listArgsBlockedOnly()).blockingGet().renderWithoutCodes();

    assertOutputContainsGroupedTasks(
        output, BLOCKED_TASKS_HEADER, ImmutableList.of(blockedTask1, blockedTask2));
    assertThat(output).doesNotContainMatch(UNBLOCKED_TASKS_HEADER);
    assertThat(output).doesNotContain(task1.render().renderWithoutCodes());
  }

  @Test
  public void list_withCompletedItems_withCompletedOnlyArgs_printsOnlyCompletedItems() {
    Task completedTask1 = createTask("completed task 1", b -> b.setStatus(COMPLETED));
    Task completedTask2 = createTask("completed task 2", b -> b.setStatus(COMPLETED));

    String output = underTest.handle(listArgsCompletedOnly()).blockingGet().renderWithoutCodes();

    assertOutputContainsGroupedTasks(
        output, COMPLETED_TASKS_HEADER, ImmutableList.of(completedTask1, completedTask2));
    assertThat(output).doesNotContain(UNBLOCKED_TASKS_HEADER);
    assertThat(output).doesNotContain(BLOCKED_TASKS_HEADER);
  }

  @Test
  public void list_withCompletedItems_withUnblockedOnlyArgs_doesNotPrintCompletedItems() {
    Task completedTask1 = createTask("completed task 1", b -> b.setStatus(COMPLETED));
    Task completedTask2 = createTask("completed task 2", b -> b.setStatus(COMPLETED));

    String output = underTest.handle(listArgsBlockedOnly()).blockingGet().renderWithoutCodes();

    assertThat(output).doesNotContain(COMPLETED_TASKS_HEADER);
    assertThat(output).doesNotContain(completedTask1.render().renderWithoutCodes());
    assertThat(output).doesNotContain(completedTask2.render().renderWithoutCodes());
  }

  @Test
  public void list_withAllTypes_withAllArgs_printsAllItems() {
    Task unblockedTask = createTask("unblocked task");
    Task blockedTask = createTask("blocked task", b -> b.addBlockingTask(unblockedTask));
    Task unblockedStartedTask = createTask("unblocked started task", b -> b.setStatus(STARTED));
    Task blockedStartedTask =
        createTask(
            "blocked started task",
            b -> b.addBlockingTask(unblockedStartedTask).setStatus(STARTED));
    Task unblockedCompletedTask =
        createTask("unblocked completed task", b -> b.setStatus(COMPLETED));
    Task blockedCompletedTask =
        createTask(
            "blocked completed task", b -> b.addBlockingTask(unblockedTask).setStatus(COMPLETED));

    String output = underTest.handle(listArgsAll()).blockingGet().renderWithoutCodes();

    assertOutputContainsGroupedTasks(
        output, UNBLOCKED_TASKS_HEADER, ImmutableList.of(unblockedTask, unblockedStartedTask));
    assertOutputContainsGroupedTasks(
        output, BLOCKED_TASKS_HEADER, ImmutableList.of(blockedTask, blockedStartedTask));
    assertOutputContainsGroupedTasks(
        output,
        COMPLETED_TASKS_HEADER,
        ImmutableList.of(unblockedCompletedTask, blockedCompletedTask));
    assertThat(output).containsMatch(
        Pattern.compile(
            Pattern.quote(UNBLOCKED_TASKS_HEADER) +
                ".*" +
                Pattern.quote(BLOCKED_TASKS_HEADER) +
                ".*" +
                Pattern.quote(COMPLETED_TASKS_HEADER),
            DOTALL));
  }

  @Test
  public void list_withAllTypes_withAllArgs_withStartedOnlyFlag_printsOnlyStartedItems() {
    Task unblockedTask = createTask("unblocked task");
    Task blockedTask = createTask("blocked task", b -> b.addBlockingTask(unblockedTask));
    Task unblockedStartedTask = createTask("unblocked started task", b -> b.setStatus(STARTED));
    Task blockedStartedTask =
        createTask(
            "blocked started task",
            b -> b.addBlockingTask(unblockedStartedTask).setStatus(STARTED));
    Task unblockedCompletedTask =
        createTask("unblocked completed task", b -> b.setStatus(COMPLETED));
    Task blockedCompletedTask =
        createTask(
            "blocked completed task", b -> b.addBlockingTask(unblockedTask).setStatus(COMPLETED));

    String output = underTest.handle(listArgsAllStartedOnly()).blockingGet().renderWithoutCodes();

    assertOutputContainsGroupedTasks(
        output, UNBLOCKED_TASKS_HEADER, ImmutableList.of(unblockedStartedTask));
    assertOutputContainsGroupedTasks(
        output, BLOCKED_TASKS_HEADER, ImmutableList.of(blockedStartedTask));
    assertThat(output).containsMatch(
        Pattern.compile(
            Pattern.quote(UNBLOCKED_TASKS_HEADER) +
                ".*" +
                Pattern.quote(BLOCKED_TASKS_HEADER),
            DOTALL));
    assertThat(output).doesNotContain(COMPLETED_TASKS_HEADER);
    assertThat(output).doesNotContain(unblockedTask.render().renderWithoutCodes());
    assertThat(output).doesNotContain(blockedTask.render().renderWithoutCodes());
    assertThat(output).doesNotContain(blockedCompletedTask.render().renderWithoutCodes());
    assertThat(output).doesNotContain(unblockedCompletedTask.render().renderWithoutCodes());
  }

  private static CommonArguments<ListArguments> listArgsAll() {
    return commonArgs(
        new ListArguments(
            /* isUnblockedSet= */ true,
            /* isBlockedSet= */ true,
            /* isCompletedSet= */ true,
            /* isStartedSet= */ false));
  }

  private static CommonArguments<ListArguments> listArgsAllStartedOnly() {
    return commonArgs(
        new ListArguments(
            /* isUnblockedSet= */ true,
            /* isBlockedSet= */ true,
            /* isCompletedSet= */ true,
            /* isStartedSet= */ true));
  }

  private static CommonArguments<ListArguments> listArgsUnblockedOnly() {
    return commonArgs(
        new ListArguments(
            /* isUnblockedSet= */ true,
            /* isBlockedSet= */ false,
            /* isCompletedSet= */ false,
            /* isStartedSet= */ false));
  }

  private static CommonArguments<ListArguments> listArgsBlockedOnly() {
    return commonArgs(
        new ListArguments(
            /* isUnblockedSet= */ false,
            /* isBlockedSet= */ true,
            /* isCompletedSet= */ false,
            /* isStartedSet= */ false));
  }

  private static CommonArguments<ListArguments> listArgsCompletedOnly() {
    return commonArgs(
        new ListArguments(
            /* isUnblockedSet= */ false,
            /* isBlockedSet= */ false,
            /* isCompletedSet= */ true,
            /* isStartedSet= */ false));
  }

  private Task createTask(String label) {
    return HandlerTestUtils.createTask(taskStore, label);
  }

  private Task createTask(String label, Function<TaskBuilder, TaskBuilder> builderFunction) {
    return HandlerTestUtils.createTask(taskStore, label, builderFunction);
  }

  private static <T> CommonArguments<T> commonArgs(T specificArgs) {
    return new CommonArguments<>(specificArgs, /* enableColorOutput= */ false);
  }
}