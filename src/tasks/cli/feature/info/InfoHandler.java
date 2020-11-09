package tasks.cli.feature.info;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import omnia.cli.out.Output;
import omnia.data.structure.Set;
import tasks.cli.handler.ArgumentHandler;
import tasks.cli.handler.HandlerException;
import tasks.cli.handler.HandlerUtil;
import tasks.model.ObservableTask;

/** Business logic for the Info command. */
public final class InfoHandler implements ArgumentHandler<InfoArguments> {
  
  @Override
  public Single<Output> handle(InfoArguments arguments) {
    if (!arguments.tasks().isPopulated()) {
      throw new HandlerException("no tasks specified");
    }

    return Observable.fromIterable(arguments.tasks())
        .map(InfoHandler::stringify)
        .flatMap(output -> Observable.just(Output.justNewline(), output))
        .skip(1)
        .collectInto(Output.builder(), Output.Builder::appendLine)
        .map(Output.Builder::build);
  }

  private static Output stringify(ObservableTask task) {
    return Output.builder()
        .appendLine(task.render())
        .appendLine(
            stringifyIfPopulated("tasks blocking this:", task.query().tasksBlockingThis()))
        .appendLine(
            stringifyIfPopulated("tasks blocked by this:", task.query().tasksBlockedByThis()))
        .build();
  }

  private static Output stringifyIfPopulated(String prefix, Flowable<Set<ObservableTask>> tasks) {
    return tasks.firstOrError()
        .map(t -> HandlerUtil.stringifyIfPopulated(prefix, t))
        .blockingGet();
  }
}
