package tasks.cli.command.info;

import static java.util.stream.Collectors.joining;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import java.util.stream.Collectors;
import omnia.data.structure.Set;
import tasks.cli.handlers.ArgumentHandler;
import tasks.cli.handlers.HandlerException;
import tasks.model.Task;

/** Business logic for the Info command. */
public final class InfoHandler implements ArgumentHandler<InfoArguments> {
  
  @Override
  public Completable handle(InfoArguments arguments) {
    if (!arguments.tasks().isPopulated()) {
      throw new HandlerException("no tasks specified");
    }

    System.out.println(
        arguments.tasks().stream()
            .map(InfoHandler::stringify)
            .collect(Collectors.joining("\n\n")));

    return Completable.complete();
  }

  private static String stringify(Task task) {
    return task.toString()
        + maybeAddPrefix("\ntasks blocking this:", stringify(task.query().tasksBlockingThis()))
        + maybeAddPrefix("\ntasks blocked by this:", stringify(task.query().tasksBlockedByThis()));
  }

  private static String stringify(Flowable<Set<Task>> tasks) {
    return tasks.firstOrError().map(InfoHandler::stringify).blockingGet();
  }

  private static String stringify(Set<Task> tasks) {
    return tasks.stream().map(Task::toString).map(line -> "\n  " + line).collect(joining());
  }

  private static String maybeAddPrefix(String prefix, String content) {
    return content.isEmpty() ? content : prefix + content;
  }
}
