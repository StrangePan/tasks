package tasks.cli.handlers;

import static java.util.stream.Collectors.joining;

import io.reactivex.Flowable;
import omnia.data.structure.Set;
import tasks.cli.arg.ListArguments;
import tasks.model.Task;
import tasks.model.TaskStore;

public final class ListHandler implements ArgumentHandler<ListArguments> {
  @Override
  public void handle(ListArguments arguments) {
    TaskStore taskStore = HandlerUtil.loadTaskStore();

    String unblockedTasks = stringify(taskStore.unblockedTasks());
    String blockedTasks = arguments.isBlockedSet() ? stringify(taskStore.blockedTasks()) : "";
    String completedTasks = arguments.isCompletedSet() ? stringify(taskStore.completedTasks()) : "";

    print("unblocked tasks:", unblockedTasks);
    print("blocked tasks:", blockedTasks);
    print("completed tasks:", completedTasks);
  }

  private static String stringify(Flowable<Set<Task>> tasks) {
    return tasks.blockingFirst()
        .stream()
        .map(Task::toString)
        .map(line -> "\n  " + line)
        .collect(joining());
  }

  private static void print(String prefix, String message) {
    if (!message.isEmpty()) {
      System.out.println(prefix + message);
    }
  }
}
