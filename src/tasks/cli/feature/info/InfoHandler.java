package tasks.cli.feature.info;

import io.reactivex.Observable;
import io.reactivex.Single;
import omnia.cli.out.Output;
import omnia.data.structure.Set;
import tasks.cli.command.common.CommonArguments;
import tasks.cli.handler.ArgumentHandler;
import tasks.cli.handler.HandlerException;
import tasks.cli.handler.HandlerUtil;
import tasks.model.Task;

/** Business logic for the Info command. */
public final class InfoHandler implements ArgumentHandler<InfoArguments> {
  
  @Override
  public Single<Output> handle(CommonArguments<? extends InfoArguments> arguments) {
    if (!arguments.specificArguments().tasks().isPopulated()) {
      throw new HandlerException("no tasks specified");
    }

    return Observable.fromIterable(arguments.specificArguments().tasks())
        .map(InfoHandler::stringify)
        .flatMap(output -> Observable.just(Output.justNewline(), output))
        .skip(1)
        .collectInto(Output.builder(), Output.Builder::appendLine)
        .map(Output.Builder::build);
  }

  private static Output stringify(Task task) {
    return Output.builder()
        .appendLine(task.render())
        .appendLine(
            stringifyIfPopulated("tasks blocking this:", task.blockingTasks()))
        .appendLine(
            stringifyIfPopulated("tasks blocked by this:", task.blockedTasks()))
        .build();
  }

  private static Output stringifyIfPopulated(String prefix, Set<? extends Task> tasks) {
    return HandlerUtil.stringifyIfPopulated(prefix, tasks);
  }
}
