package tasks.cli.command.help;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

import io.reactivex.Observable;
import io.reactivex.Single;
import java.util.Comparator;
import java.util.stream.Stream;
import omnia.cli.out.Output;
import omnia.data.cache.Memoized;
import omnia.data.structure.Set;
import omnia.data.structure.immutable.ImmutableList;
import omnia.data.structure.immutable.ImmutableSet;
import tasks.cli.arg.CommandDocumentation;
import tasks.cli.arg.CommandDocumentation.OptionDocumentation;
import tasks.cli.handlers.ArgumentHandler;

/** Business logic for the Help command. */
public final class HelpHandler implements ArgumentHandler<HelpArguments> {
  private final Memoized<Set<CommandDocumentation>> documentation;

  public HelpHandler(Memoized<Set<CommandDocumentation>> documentation) {
    this.documentation = requireNonNull(documentation);
  }

  @Override
  public Single<Output> handle(HelpArguments arguments) {
    return Single.just(arguments)
        .map(HelpArguments::mode)
        .flatMap(mode -> mode.map(this::getHelpOutputForMode).orElseGet(this::getHelpOutputForSelf));
  }

  private Single<Output> getHelpOutputForSelf() {
    return getHelpOutputForCommandListing()
        .map(
            commandsOutput ->
                Output.builder()
                    .appendLine("Commands:")
                    .appendLine(commandsOutput, 2)
                    .build());
  }

  private Single<Output> getHelpOutputForCommandListing() {
    return Single.fromCallable(documentation::value)
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
    return Single.just(documentation)
        .map(Memoized::value)
        .flatMapObservable(Observable::fromIterable)
        .filter(
            documentation ->
                ImmutableSet.builder()
                    .add(documentation.canonicalName())
                    .addAll(documentation.aliases())
                    .build()
                    .contains(mode))
        .firstElement()
        .map(HelpHandler::generateOutputFor)
        .switchIfEmpty(getHelpOutputForSelf());
  }

  private static Output generateOutputFor(CommandDocumentation documentation) {
    return Output.builder()
        .appendLine(headerLine(documentation))
        .appendLine(aliasesLine(documentation))
        .appendLine(usageLine(documentation))
        .appendLine(documentation.description(), 2)
        .appendLine(prependWithBlankLine(parameterLines(documentation)))
        .build();
  }

  private static Output prependWithBlankLine(Output output) {
    return output.isPopulated()
        ? Output.builder().appendLine().appendLine(output).build()
        : Output.empty();
  }

  private static Output headerLine(CommandDocumentation documentation) {
    return Output.builder().append("Command: ").append(documentation.canonicalName()).build();
  }

  private static Output aliasesLine(CommandDocumentation documentation) {
    return documentation.aliases().isPopulated()
        ? Output.builder()
            .append("Aliases: ")
            .append(
                documentation.aliases()
                    .stream()
                    .collect(joining(", ")))
            .build()
        : Output.empty();
  }

  private static Output usageLine(CommandDocumentation documentation) {
    return Output.builder().append("Usage:   ")
        .append(documentation.canonicalName())
        .append(documentation.parameterRepresentation().map(r -> " " + r).orElse(""))
        .append(documentation.options().isPopulated() ? " [options...]" : "")
        .build();
  }

  private static Output parameterLines(CommandDocumentation documentation) {
    return Observable.fromIterable(documentation.options())
        .sorted(Comparator.comparing(OptionDocumentation::canonicalName))
        .map(HelpHandler::parameterLine)
        .collectInto(Output.builder(), Output.Builder::appendLine)
        .map(Output.Builder::build)
        .blockingGet();
  }

  private static Output parameterLine(OptionDocumentation documentation) {
    return Output.builder()
        .appendLine(
            Stream.concat(
                Stream.of("--" + documentation.canonicalName()),
                Stream.of("-" + documentation.shortFlag()))
            .collect(joining(","))
                + (documentation.parameterRepresentation().map(r -> " " + r).orElse(""))
                + (documentation.isRepeatable() ? " [+]" : ""),
            2)
        .appendLine(documentation.description(), 4)
        .build();
  }
}
