package tasks.cli.feature.reword;

import static com.google.common.truth.Truth.assertThat;
import static java.util.regex.Pattern.DOTALL;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.regex.Pattern;
import omnia.data.cache.Memoized;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import tasks.cli.command.common.CommonArguments;
import tasks.cli.handler.HandlerException;
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

    assertThat(
        taskStore.observe()
            .firstOrError()
            .blockingGet()
            .lookUpById(task.id())
            .orElseThrow()
            .label())
        .isEqualTo("example task");
  }

  @Test
  public void handle_withNewDescription_renamesTask() {
    Task task = createTask("example task");

    underTest.handle(rewordArgs(task, "reworded task")).ignoreElement().blockingAwait();

    assertThat(
        taskStore.observe()
            .firstOrError()
            .blockingGet()
            .lookUpById(task.id())
            .orElseThrow()
            .label())
        .isEqualTo("reworded task");
  }

  @Test
  public void handle_withNewDescription_outputsRewordedTask() {
    Task task = createTask("example task");

    String output = underTest.handle(rewordArgs(task, "reworded task")).blockingGet().toString();

    assertThat(output).containsMatch(
        Pattern.compile("Updated description:.*reworded task", DOTALL));
    assertThat(output).doesNotContain("example task");
  }

  private Task createTask(String label) {
    return taskStore.createTask(label, b -> b).blockingGet().third();
  }

  private static CommonArguments<RewordArguments> rewordArgs(Task task, String label) {
    return commonArgs(new RewordArguments(task, label));
  }

  private static <T> CommonArguments<T> commonArgs(T args) {
    return new CommonArguments<>(args, /* enableColorOutput= */ true);
  }

}