package tasks.cli.handlers;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import java.util.Comparator;
import omnia.cli.out.Output;
import omnia.data.cache.Memoized;
import omnia.data.structure.Set;
import omnia.data.structure.immutable.ImmutableList;
import tasks.cli.arg.CommandDocumentation;
import tasks.cli.arg.HelpArguments;

public final class HelpHandler implements ArgumentHandler<HelpArguments> {
  private final Memoized<Set<CommandDocumentation>> documentation;

  HelpHandler(Memoized<Set<CommandDocumentation>> documentation) {
    this.documentation = requireNonNull(documentation);
  }

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
    return getOutputForCommandListing()
        .map(
            commandsOutput ->
                Output.builder()
                    .appendLine("Commands:")
                    .appendLine(commandsOutput, 2)
                    .build());
  }

  private Single<Output> getOutputForCommandListing() {
    return Single.just(documentation)
        .map(Memoized::value)
        .flatMapObservable(Observable::fromIterable)
        .sorted(Comparator.comparing(CommandDocumentation::canonicalName))
        .map(
            documentation ->
                ImmutableList.<String>builder()
                    .add(documentation.canonicalName())
                    .addAll(documentation.aliases())
                    .build())
        .map(list -> list.stream().collect(joining(", ")))
        .collectInto(Output.builder(), Output.Builder::appendLine)
        .map(Output.Builder::build);
  }

  private Single<Output> getHelpOutputForMode(String mode) {
    return Single.just(Output.empty());
  }

}
