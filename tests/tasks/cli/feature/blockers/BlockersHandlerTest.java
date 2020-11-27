package tasks.cli.feature.blockers;

import static com.google.common.truth.Truth.assertThat;
import static java.util.regex.Pattern.DOTALL;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.function.Function;
import java.util.regex.Pattern;
import omnia.cli.out.Output;
import omnia.data.cache.Memoized;
import omnia.data.structure.immutable.ImmutableList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import tasks.cli.command.common.CommonArguments;
import tasks.cli.handler.HandlerException;
import tasks.model.ObservableTaskStore;
import tasks.model.Task;
import tasks.model.TaskBuilder;
import tasks.model.impl.ObservableTaskStoreImpl;

@RunWith(JUnit4.class)
public final class BlockersHandlerTest {

  private final ObservableTaskStore taskStore = ObservableTaskStoreImpl.createInMemoryStorage();

  private final BlockersHandler underTest = new BlockersHandler(Memoized.just(taskStore));

  @Test
  public void handle_whenBlockingSelf_throwsException() {
    Task existingTask = createTask("existing task");

    assertThrows(
        HandlerException.class,
        () -> underTest.handle(
            commonArgs(
                new BlockersArguments(
                    existingTask,
                    ImmutableList.of(existingTask),
                    ImmutableList.empty(),
                    /* clearAllBlockers= */ false))));
  }

  @Test
  public void handle_whenRemovingAndAddingSameTask_throwsException() {
    Task existingTask = createTask("existing task");
    Task targetTask = createTask("target task");

    assertThrows(
        HandlerException.class,
        () -> underTest.handle(
            commonArgs(
                new BlockersArguments(
                    targetTask,
                    ImmutableList.of(existingTask),
                    ImmutableList.of(existingTask),
                    /* clearAllBlockers= */ false))));
  }

  @Test
  public void handle_noChanges_outputsNothing() {
    Task targetTask = createTask("target task");

    underTest.handle(
        commonArgs(
            new BlockersArguments(
                targetTask,
                ImmutableList.empty(),
                ImmutableList.empty(),
                /* clearAllBlockers= */ false)))
        .test()
        .assertValue(Output.empty());
  }

  @Test
  public void handle_clearAllBlockers_clearsExistingBlockers() {
    Task existingTask = createTask("existing task");
    Task targetTask = createTask("target task", b -> b.addBlockingTask(existingTask));

    underTest.handle(
        commonArgs(
            new BlockersArguments(
                targetTask,
                ImmutableList.empty(),
                ImmutableList.empty(),
                /* clearAllBlockers= */ true)))
        .ignoreElement()
        .blockingAwait();

    assertThat(
        taskStore.observe()
            .firstOrError()
            .blockingGet()
            .lookUpById(targetTask.id())
            .orElseThrow()
            .blockingTasks()
            .isPopulated())
        .isFalse();
  }

  @Test
  public void handle_clearAllBlockers_outputsRemovedBlockers() {
    Task existingTask1 = createTask("existing task");
    Task existingTask2 = createTask("existing task");
    Task targetTask =
        createTask(
            "target task", b -> b.addBlockingTask(existingTask1).addBlockingTask(existingTask2));

    String output =
        underTest.handle(
            commonArgs(
                new BlockersArguments(
                    targetTask,
                    ImmutableList.empty(),
                    ImmutableList.empty(),
                    /* clearAllBlockers= */ true)))
            .blockingGet()
            .toString();

    assertThat(output).contains(targetTask.label());
    assertThat(output).containsMatch(
        Pattern.compile("removed blockers:.*" + existingTask1.label(), DOTALL));
    assertThat(output).containsMatch(
        Pattern.compile("removed blockers:.*" + existingTask2.label(), DOTALL));
    assertThat(output).doesNotContain("current blockers");
  }

  @Test
  public void handle_removeBlocker_blockerIsRemoved() {
    Task existingTask = createTask("existing task");
    Task targetTask = createTask("target task", b -> b.addBlockingTask(existingTask));

    underTest.handle(
        commonArgs(
            new BlockersArguments(
                targetTask,
                ImmutableList.empty(),
                ImmutableList.of(existingTask),
                /* clearAllBlockers= */ false)))
        .ignoreElement()
        .blockingAwait();

    assertThat(
        taskStore.observe()
            .firstOrError()
            .blockingGet()
            .lookUpById(targetTask.id())
            .orElseThrow()
            .blockingTasks()
            .isPopulated())
        .isFalse();
  }

  @Test
  public void handle_removeBlocker_outputsRemovedBlocker() {
    Task existingTask = createTask("existing task");
    Task targetTask = createTask("target task", b -> b.addBlockingTask(existingTask));

    String output =
        underTest.handle(
            commonArgs(
                new BlockersArguments(
                    targetTask,
                    ImmutableList.empty(),
                    ImmutableList.of(existingTask),
                    /* clearAllBlockers= */ false)))
            .blockingGet()
            .toString();

    assertThat(output).contains(targetTask.label());
    assertThat(output).containsMatch(
        Pattern.compile("removed blockers:.*" + existingTask.label(), DOTALL));
    assertThat(output).doesNotContain("current blockers");
  }

  @Test
  public void handle_addBlocker_blockerIsAdded() {
    Task existingTask = createTask("existing task");
    Task targetTask = createTask("target task");

    underTest.handle(
        commonArgs(
            new BlockersArguments(
                targetTask,
                ImmutableList.of(existingTask),
                ImmutableList.empty(),
                /* clearAllBlockers= */ false)))
        .ignoreElement()
        .blockingAwait();

    Task updatedTask =
        taskStore.observe()
            .firstOrError()
            .blockingGet()
            .lookUpById(targetTask.id())
            .orElseThrow();
    assertThat(updatedTask.blockingTasks().count()).isEqualTo(1);
    assertThat(updatedTask.blockingTasks().iterator().next().id()).isEqualTo(existingTask.id());
  }

  @Test
  public void handle_addBlocker_outputsNewBlocker() {
    Task existingTask = createTask("existing task");
    Task targetTask = createTask("target task");

    String output =
        underTest.handle(
            commonArgs(
                new BlockersArguments(
                    targetTask,
                    ImmutableList.of(existingTask),
                    ImmutableList.empty(),
                    /* clearAllBlockers= */ false)))
            .blockingGet()
            .toString();

    assertThat(output).contains(targetTask.label());
    assertThat(output).containsMatch(
        Pattern.compile("current blockers:.*" + existingTask.label(), DOTALL));
    assertThat(output).doesNotContain("removed blockers");
  }

  @Test
  public void handle_addBlocker_removeBlocker_blockersAreAddedAndRemoved() {
    Task existingBlocker = createTask("existing blocker");
    Task blockerToAdd = createTask("blocker to add");
    Task targetTask = createTask("target task", b -> b.addBlockingTask(existingBlocker));

    underTest.handle(
        commonArgs(
            new BlockersArguments(
                targetTask,
                ImmutableList.of(blockerToAdd),
                ImmutableList.of(existingBlocker),
                /* clearAllBlockers= */ false)))
        .ignoreElement()
        .blockingAwait();

    Task updatedTask =
        taskStore.observe()
            .firstOrError()
            .blockingGet()
            .lookUpById(targetTask.id())
            .orElseThrow();
    assertThat(updatedTask.blockingTasks().count()).isEqualTo(1);
    assertThat(updatedTask.blockingTasks().iterator().next().id()).isEqualTo(blockerToAdd.id());
  }

  @Test
  public void handle_addBlocker_removeBlocker_outputsAllTasks() {
    Task existingBlocker = createTask("existing blocker");
    Task blockerToAdd = createTask("blocker to add");
    Task targetTask = createTask("target task", b -> b.addBlockingTask(existingBlocker));

    String output =
        underTest.handle(
            commonArgs(
                new BlockersArguments(
                    targetTask,
                    ImmutableList.of(blockerToAdd),
                    ImmutableList.of(existingBlocker),
                    /* clearAllBlockers= */ false)))
            .blockingGet()
            .toString();

    assertThat(output).contains(targetTask.label());
    assertThat(output).containsMatch(
        Pattern.compile("removed blockers:.*" + existingBlocker.label(), DOTALL));
    assertThat(output).containsMatch(
        Pattern.compile("current blockers:.*" + blockerToAdd.label(), DOTALL));
  }

  @Test
  public void handle_addBlocker_removeBlocker_withoutSubscribing_doesNothing() {
    Task existingBlocker = createTask("existing blocker");
    Task blockerToAdd = createTask("blocker to add");
    Task targetTask = createTask("target task", b -> b.addBlockingTask(existingBlocker));

    underTest.handle(
        commonArgs(
            new BlockersArguments(
                targetTask,
                ImmutableList.of(blockerToAdd),
                ImmutableList.of(existingBlocker),
                /* clearAllBlockers= */ false)));

    Task unchangedTask =
        taskStore.observe()
            .firstOrError()
            .blockingGet()
            .lookUpById(targetTask.id())
            .orElseThrow();
    assertThat(unchangedTask.blockingTasks().count()).isEqualTo(1);
    assertThat(unchangedTask.blockingTasks().iterator().next().id())
        .isEqualTo(existingBlocker.id());
  }

  @Test
  public void handle_addBlocker_whenResultsInCircularDependency_emitsError() {
    Task blockerToAdd = createTask("blocker to add");
    Task blockee = createTask("existing blocker", b -> b.addBlockedTask(blockerToAdd));
    Task targetTask = createTask("target task", b -> b.addBlockedTask(blockee));

    underTest.handle(
        commonArgs(
            new BlockersArguments(
                targetTask,
                ImmutableList.of(blockerToAdd),
                ImmutableList.empty(),
                /* clearAllBlockers= */ false)))
        .test()
        .assertError(IllegalStateException.class);
  }

  private Task createTask(String label) {
    return createTask(label, b -> b);
  }

  private Task createTask(String label, Function<TaskBuilder, TaskBuilder> builderFunction) {
    return taskStore.createTask(label, builderFunction).blockingGet().third();
  }

  private static <T> CommonArguments<T> commonArgs(T args) {
    return new CommonArguments<>(args, /* enableColorOutput= */ true);
  }
}