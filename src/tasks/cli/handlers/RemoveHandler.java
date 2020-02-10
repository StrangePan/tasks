package tasks.cli.handlers;

import static java.util.stream.Collectors.joining;

import io.reactivex.Observable;
import omnia.data.structure.Set;
import omnia.data.structure.immutable.ImmutableSet;
import tasks.cli.arg.RemoveArguments;
import tasks.model.Task;
import tasks.model.TaskStore;

public final class RemoveHandler implements ArgumentHandler<RemoveArguments> {
  @Override
  public void handle(RemoveArguments arguments) {
    // Validate arguments
    if (!arguments.tasks().isPopulated()) {
      throw new HandlerException("no tasks specified");
    }

    TaskStore taskStore = HandlerUtil.loadTaskStore();

    HandlerUtil.validateTasksIds(taskStore, arguments.tasks());

    Set<Task> tasksToDelete =
        ImmutableSet.copyOf(HandlerUtil.toTasks(taskStore, arguments.tasks()).blockingIterable());

    String report = "tasks removed:" + stringify(tasksToDelete);

    Observable.fromIterable(tasksToDelete)
        .concatMapCompletable(taskStore::deleteTask)
        .blockingAwait();

    taskStore.writeToDisk().blockingAwait();

    System.out.println(report);
  }

  private static String stringify(Set<Task> tasks) {
    return tasks.stream().map(Task::toString).map(line -> "\n  " + line).collect(joining());
  }
}
