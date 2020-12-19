package tasks.cli.feature.reword;

import static com.google.common.truth.Truth.assertThat;
import static java.util.regex.Pattern.DOTALL;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static tasks.cli.handler.testing.HandlerTestUtils.assertOutputContainsGroupedTasks;
import static tasks.cli.handler.testing.HandlerTestUtils.commonArgs;
import static tasks.cli.handler.testing.HandlerTestUtils.getUpdatedVersionOf;

import java.util.regex.Pattern;
import omnia.data.cache.Memoized;
import omnia.data.structure.immutable.ImmutableList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import tasks.cli.command.common.CommonArguments;
import tasks.cli.handler.HandlerException;
import tasks.cli.handler.testing.HandlerTestUtils;
import tasks.model.ObservableTaskStore;
import tasks.model.Task;
import tasks.model.impl.ObservableTaskStoreImpl;

@RunWith(JUnit4.class)
public final class RewordHandlerTest {

  private final ObservableTaskStore taskStore = ObservableTaskStoreImpl.createInMemoryStorage();

  private final RewordHandler underTest = new RewordHandler(Memoized.just(taskStore));

  @Test
  public void handle_withEmptyDescription_throwsException_doesNotModifyLabel() {
    Task task = createTask("example task");

    assertThrows(HandlerException.class, () -> underTest.handle(rewordArgs(task, "  ")));

    assertThat(getUpdatedVersionOf(task).label()).isEqualTo("example task");
  }

  @Test
  public void handle_withNewDescription_renamesTask() {
    Task task = createTask("example task");

    underTest.handle(rewordArgs(task, "reworded task")).ignoreElement().blockingAwait();

    assertThat(getUpdatedVersionOf(task).label()).isEqualTo("reworded task");
  }

  @Test
  public void handle_withNewDescription_outputsRewordedTask() {
    Task task = createTask("example task");

    String output = underTest.handle(rewordArgs(task, "reworded task")).blockingGet().toString();

    assertOutputContainsGroupedTasks(
        output, "Updated description:", ImmutableList.of(getUpdatedVersionOf(task)));
    assertThat(output).doesNotContain("example task");
  }

  private Task createTask(String label) {
    return HandlerTestUtils.createTask(taskStore, label);
  }

  private Task getUpdatedVersionOf(Task task) {
    return HandlerTestUtils.getUpdatedVersionOf(taskStore, task);
  }

  private static CommonArguments<RewordArguments> rewordArgs(Task task, String label) {
    return commonArgs(new RewordArguments(task, label));
  }
}