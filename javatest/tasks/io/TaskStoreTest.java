package tasks.io;

import static org.junit.Assert.assertEquals;

import omnia.data.structure.Collection;
import omnia.data.structure.immutable.ImmutableSet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import tasks.Task;
import tasks.io.testing.FakeFile;

@RunWith(JUnit4.class)
public final class TaskStoreTest {

  private TaskStore underTest = new TaskStore(new FakeFile());

  @Test
  public void store_thenRetrieve_withSingleTask_didRetrieve() {
    Task task =
        Task.builder()
            .id(Task.Id.from(132))
            .label("this was a triumph")
            .isCompleted(false)
            .build();

    underTest.storeBlocking(ImmutableSet.<Task>builder().add(task).build());
    Collection<Task> retrievedTasks = underTest.retrieveBlocking();

    assertEquals(1, retrievedTasks.count());
    assertEquals(task, retrievedTasks.iterator().next());
  }

  @Test
  public void store_thenRetrieve_withTwoTasks_withDependencies_didRetrieve() {
    Task task1 =
        Task.builder()
            .id(Task.Id.from(132))
            .label("this was a triumph")
            .isCompleted(false)
            .build();
    Task task2 =
        Task.builder()
            .id(Task.Id.from(2))
            .label("i'm making a note here: huge success")
            .dependencies(
                ImmutableSet.<Task>builder().add(task1).build())
            .build();

    ImmutableSet<Task> tasks =
        ImmutableSet.<Task>builder()
            .add(task1)
            .add(task2)
            .build();

    underTest.storeBlocking(tasks);
    Collection<Task> retrievedTasks = underTest.retrieveBlocking();

    assertEquals(tasks, ImmutableSet.<Task>builder().addAll(retrievedTasks).build());
  }
}
