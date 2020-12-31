package tasks.cli.parser;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static omnia.data.stream.Collectors.toList;

import io.reactivex.rxjava3.core.Observable;
import java.util.Optional;
import java.util.OptionalInt;
import omnia.data.cache.Memoized;
import omnia.data.structure.Collection;
import omnia.data.structure.List;
import omnia.data.structure.immutable.ImmutableList;
import omnia.data.structure.immutable.ImmutableSet;
import omnia.data.structure.tuple.Couple;
import omnia.data.structure.tuple.Tuple;
import org.apache.commons.cli.CommandLine;
import tasks.cli.command.FlagOption;
import tasks.cli.command.Option;
import tasks.cli.command.Parameter;
import tasks.model.ObservableTaskStore;
import tasks.model.Task;
import tasks.model.TaskStore;

public final class ParserUtil {

  private ParserUtil() {}

  public static Parser<List<ParseResult<? extends Task>>> taskListParser(
      Memoized<? extends ObservableTaskStore> taskStore) {
    return (args) -> parseTaskIds(ImmutableList.copyOf(args), taskStore.value());
  }

  public static List<ParseResult<? extends Task>> parseTaskIds(
      List<String> userInputs, ObservableTaskStore observableTaskStore) {
    return observableTaskStore.observe()
        .firstOrError()
        .<List<ParseResult<? extends Task>>>map(
            taskStore -> userInputs.stream()
                .map(userInput -> parseTaskId(userInput, taskStore))
                .collect(toList()))
        .blockingGet();
  }

  private static ParseResult<? extends Task> parseTaskId(String userInput, TaskStore taskStore) {
    ImmutableSet<? extends Task> matchingTasks = taskStore.allTasksMatchingCliPrefix(userInput);
    if (matchingTasks.count() > 1) {
      return ParseResult.failure(
          String.format("Ambiguous task ID: multiple tasks match '%s'", userInput));
    }
    return matchingTasks.stream()
        .findFirst()
        .map(ParseResult::success)
        .orElse(
            ParseResult.failure(String.format("Unknown task ID: no tasks match '%s'", userInput)));
  }

  public static <T> T extractSuccessfulResultOrThrow(ParseResult<? extends T> parseResult) {
    return extractSuccessfulResultsOrThrow(ImmutableList.of(parseResult)).itemAt(0);
  }

  public static <T> ImmutableList<T> extractSuccessfulResultsOrThrow(
      Collection<? extends ParseResult<? extends T>> parseResults) {
    return Observable.fromIterable(parseResults)
        .collectInto(
            Tuple.of(ImmutableList.<T>builder(), ImmutableList.<String>builder()),
            (couple, result) -> couple
                .mapFirst(b -> maybeAdd(b, result.successResult()))
                .mapSecond(b -> maybeAdd(b, result.failureMessage())))
        .map(
            couple -> couple
                .mapFirst(ImmutableList.Builder::build)
                .mapSecond(ImmutableList.Builder::build))
        .doOnSuccess(couple -> throwIfPopulated(couple.second()))
        .map(Couple::first)
        .blockingGet();
  }

  private static <T> ImmutableList.Builder<T> maybeAdd(
      ImmutableList.Builder<T> builder, Optional<? extends T> item) {
    item.ifPresent(builder::add);
    return builder;
  }

  private static void throwIfPopulated(List<? extends String> failureMessages) {
    if (failureMessages.isPopulated()) {
      throw new ParserException(
          "Unable to parse task IDs: " + failureMessages.stream().collect(joining(",", "[", "]")));
    }
  }

  public static boolean getFlagPresence(CommandLine commandLine, FlagOption flagOption) {
    return commandLine.hasOption(flagOption.shortName().orElse(flagOption.longName()));
  }

  public static List<String> getOptionValues(CommandLine commandLine, Option option) {
    return getOptionValues(commandLine, option.shortName().orElse(option.longName()));
  }

  public static List<String> getOptionValues(CommandLine commandLine, String opt) {
    return ImmutableList.copyOf(
        Optional.ofNullable(requireNonNull(commandLine).getOptionValues(requireNonNull(opt)))
            .orElse(new String[0]));
  }

  public static Optional<String> getSingleOptionValue(CommandLine commandLine, Option option) {
    return getSingleOptionValue(commandLine, option.shortName().orElse(option.longName()));
  }

  public static Optional<String> getSingleOptionValue(CommandLine commandLine, String opt) {
    if (Optional.ofNullable(commandLine.getOptionValues(opt)).orElse(new String[0]).length > 1) {
      throw new ParserException(
          String.format("Too many values provided for parameter '%s'", opt));
    }
    return Optional.ofNullable(commandLine.getOptionValue(opt));
  }

  public static void assertNoExtraArgs(CommandLine commandLine) {
    assertNoExtraArgs(List.masking(commandLine.getArgList()), 0);
  }

  public static void assertNoExtraArgs(
      CommandLine commandLine, Collection<Parameter> parameters) {
    assertNoExtraArgs(List.masking(commandLine.getArgList()), parameters);
  }

  public static void assertNoExtraArgs(
      List<? extends String> args, Collection<Parameter> parameters) {
    computeMaxNumberOfCommandParameters(parameters)
        .ifPresent(max -> assertNoExtraArgs(args, max));
  }

  private static void assertNoExtraArgs(
      List<? extends String> args, int maxNumberOfCommandParameters) {
    if (args.count() - 1 > maxNumberOfCommandParameters) {
      throw new ParserException(
          "Unexpected arguments given: "
              + args.stream()
                  .skip(maxNumberOfCommandParameters)
                  .map(s -> String.format("'%s'", s))
                  .collect(joining(", ")));
    }
  }

  public static OptionalInt computeMaxNumberOfCommandParameters(
      Collection<Parameter> parameters) {
    return parameters.stream().anyMatch(Parameter::isRepeatable)
        ? OptionalInt.empty()
        : OptionalInt.of(parameters.count());
  }
}
