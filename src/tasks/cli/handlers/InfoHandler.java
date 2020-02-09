package tasks.cli.handlers;

import static java.util.stream.Collectors.joining;

import io.reactivex.Flowable;
import omnia.data.structure.Set;
import tasks.cli.arg.InfoArguments;
import tasks.model.Task;
import tasks.model.TaskStore;

public final class InfoHandler implements ArgumentHandler<InfoArguments> {
  @Override
  public void handle(InfoArguments arguments) {
    if (!arguments.tasks().isPopulated()) {
      throw new HandlerException("no tasks specified");
    }

    TaskStore taskStore = HandlerUtil.loadTaskStore();
    HandlerUtil.validateTasksIds(taskStore, arguments.tasks());

    String message =
        HandlerUtil.toTasks(taskStore, arguments.tasks())
            .map(InfoHandler::stringify)
            .reduce((first, second) -> first + "\n\n" + second)
            .blockingGet();

    System.out.println(message);
  }

  private static String stringify(Task task) {
    return task.toString()
        + stringify(task.query().tasksBlockingThis())
        + stringify(task.query().tasksBlockedByThis());
  }

  private static String stringify(Flowable<Set<Task>> tasks) {
    return tasks.firstOrError().map(InfoHandler::stringify).blockingGet();
  }

  private static String stringify(Set<Task> tasks) {
    return tasks.stream().map(Task::toString).map(line -> "\n  " + line).collect(joining());
  }
}
