package tasks.cli.handlers;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import omnia.cli.out.Output;
import omnia.data.structure.immutable.ImmutableList;
import tasks.cli.arg.HelpArguments;

public final class HelpHandler implements ArgumentHandler<HelpArguments> {

  HelpHandler() {}

  @Override
  public Completable handle(HelpArguments arguments) {
    return Single.just(arguments)
        .map(HelpArguments::mode)
        .flatMap(mode -> mode.map(this::getHelpOutputForMode).orElseGet(this::getHelpOutputForSelf))
        .map(Output::renderForTerminal)
        .doOnSuccess(System.out::print)
        .ignoreElement()
        .cache();

  }

  private Single<Output> getHelpOutputForSelf() {
    return Single.just(
        ImmutableList.<String>builder()
            .add("add")
            .add("amend")
            .add("complete")
            .add("help")
            .add("list, ls")
            .add("remove, rm")
            .add("reopen")
            .build())
        .flatMap(
            commands ->
                Observable.fromIterable(commands)
                    .collectInto(Output.builder(), Output.Builder::appendLine)
                    .map(Output.Builder::build))
        .map(
            commandsOutput ->
                Output.builder()
                    .appendLine("Commands:")
                    .appendLine(commandsOutput, 2)
                    .build());
  }

  private Single<Output> getHelpOutputForMode(String mode) {
    return Single.just(Output.empty());
  }
}
