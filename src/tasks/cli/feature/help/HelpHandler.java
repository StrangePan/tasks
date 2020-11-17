package tasks.cli.feature.help;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static omnia.data.stream.Collectors.toImmutableSet;

import io.reactivex.Observable;
import io.reactivex.Single;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;
import omnia.cli.out.Output;
import omnia.data.cache.Memoized;
import omnia.data.structure.immutable.ImmutableList;
import tasks.cli.command.Command;
import tasks.cli.command.Option;
import tasks.cli.command.Parameter;
import tasks.cli.command.common.CommonArguments;
import tasks.cli.feature.help.CommandDocumentation.OptionDocumentation;
import tasks.cli.command.Commands;
import tasks.cli.command.common.CommonOptions;
import tasks.cli.handler.ArgumentHandler;

/** Business logic for the Help command. */
public final class HelpHandler implements ArgumentHandler<HelpArguments> {
  private final Memoized<? extends Commands> commands;

  public HelpHandler(Memoized<? extends Commands> commands) {
    this.commands = requireNonNull(commands);
  }

  @Override
  public Single<Output> handle(CommonArguments<? extends HelpArguments> arguments) {
    return Single.just(arguments)
        .map(CommonArguments::specificArguments)
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
    return Single.fromCallable(commands::value)
        .map(Commands::getAllCommands)
        .flatMapObservable(Observable::fromIterable)
        .map(HelpHandler::toCommandDocumentation)
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
    return Single.fromCallable(commands::value)
        .map(commands -> commands.getMatchingCommand(mode))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .map(HelpHandler::toCommandDocumentation)
        .map(HelpHandler::generateOutputFor)
        .switchIfEmpty(getHelpOutputForSelf());
  }

  private static CommandDocumentation toCommandDocumentation(Command registration) {
    return new CommandDocumentation(
        registration.canonicalName(),
        ImmutableList.copyOf(registration.aliases()),
        toParameterRepresentation(registration),
        registration.description(),
        registration.options().stream()
            .map(HelpHandler::toOptionDocumentation)
            .collect(toImmutableSet()));
  }

  private static Optional<String> toParameterRepresentation(Command registration) {
    return registration.parameters().isPopulated()
        ? Optional.of(
        registration.parameters().stream()
            .map(HelpHandler::toParameterRepresentation)
            .collect(joining(" ")))
        : Optional.empty();
  }

  private static String toParameterRepresentation(Parameter parameter) {
    return "<" + parameter.description() + (parameter.isRepeatable() ? "..." : "") + ">";
  }

  private static Optional<String> toParameterRepresentation(Option option) {
    return option.parameterRepresentation().map(rep -> "<" + rep + ">");
  }

  private static OptionDocumentation toOptionDocumentation(Option option) {
    return new OptionDocumentation(
        option.longName(),
        option.shortName(),
        option.description(),
        option.isRepeatable(),
        toParameterRepresentation(option));
  }

  private static Output generateOutputFor(CommandDocumentation documentation) {
    return Output.builder()
        .appendLine(headerLine(documentation))
        .appendLine(aliasesLine(documentation))
        .appendLine(usageLine(documentation))
        .appendLine(documentation.description(), 2)
        .appendLine(prependWithBlankLine(parameterLines(documentation)))
        .appendLine(prependWithBlankLine(documentationCommonToAllCommands()))
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

  private static Output documentationCommonToAllCommands() {
    return Output.builder()
        .appendLine("Options common to all commands")
        .appendLine()
        .appendLine(
            Single.fromCallable(CommonOptions.OPTIONS::value)
                .flattenAsObservable(options -> options)
                .map(HelpHandler::toOptionDocumentation)
                .map(HelpHandler::parameterLine)
                .collectInto(Output.builder(), Output.Builder::appendLine)
                .map(Output.Builder::build)
                .blockingGet())
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
                documentation.shortFlag().stream().map(shortFlag -> "-" + shortFlag))
            .collect(joining(","))
                + (documentation.parameterRepresentation().map(r -> " " + r).orElse(""))
                + (documentation.isRepeatable() ? " [+]" : ""),
            2)
        .appendLine(documentation.description(), 4)
        .build();
  }
}
