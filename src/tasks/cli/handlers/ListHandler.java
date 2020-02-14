package tasks.cli.handlers;

import io.reactivex.Flowable;
import omnia.data.structure.Set;
import tasks.cli.arg.ListArguments;
import tasks.model.Task;
import tasks.model.TaskStore;

public final class ListHandler implements ArgumentHandler<ListArguments> {
  @Override
  public void handle(ListArguments arguments) {
    TaskStore taskStore = HandlerUtil.loadTaskStore();

    print("unblocked tasks:", stringify(taskStore.unblockedTasks()));
    print("blocked tasks:", arguments.isBlockedSet() ? stringify(taskStore.blockedTasks()) : "");
    print("completed tasks:", arguments.isCompletedSet() ? stringify(taskStore.completedTasks()) : "");
  }

  private static String stringify(Flowable<Set<Task>> tasks) {
    return HandlerUtil.stringify(tasks.blockingFirst());
  }

  private static void print(String prefix, String message) {
    if (!message.isEmpty()) {
      System.out.println(prefix + message);
    }
  }
}
