package tasks.io;

import static org.junit.Assert.assertTrue;

import omnia.data.structure.DirectedGraph;
import omnia.data.structure.immutable.ImmutableDirectedGraph;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import tasks.Task;
import tasks.cli.CliTaskId;
import tasks.io.testing.FakeFile;

@RunWith(JUnit4.class)
public final class TaskStoreTest {

  private TaskStore underTest = new TaskStore(new FakeFile());

  @Test
  public void store_thenRetrieve_withSingleTask_didRetrieve() {
    Task task =
        Task.builder()
            .id(CliTaskId.from(132))
            .label("this was a triumph")
            .isCompleted(false)
            .build();
    DirectedGraph<Task> tasks = ImmutableDirectedGraph.<Task>builder().addNode(task).build();

    underTest.storeBlocking(tasks);
    DirectedGraph<Task> retrievedTasks = underTest.retrieveBlocking();

    assertTrue(DirectedGraph.areEqual(tasks, retrievedTasks));
  }

  @Test
  public void store_thenRetrieve_withTwoTasks_withDependencies_didRetrieve() {
    Task task1 =
        Task.builder()
            .id(CliTaskId.from(132))
            .label("this was a triumph")
            .isCompleted(false)
            .build();
    Task task2 =
        Task.builder()
            .id(CliTaskId.from(2))
            .label("i'm making a note here: huge success")
            .build();

    ImmutableDirectedGraph<Task> tasks =
        ImmutableDirectedGraph.<Task>builder()
            .addNode(task1)
            .addNode(task2)
            .addEdge(task2, task1)
            .build();

    underTest.storeBlocking(tasks);
    DirectedGraph<Task> retrievedTasks = underTest.retrieveBlocking();

    assertTrue(DirectedGraph.areEqual(tasks, retrievedTasks));
  }
}
