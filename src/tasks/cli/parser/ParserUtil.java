package tasks.cli.parser;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static omnia.data.stream.Collectors.toImmutableList;
import static omnia.data.stream.Collectors.toList;

import io.reactivex.Observable;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import omnia.algorithm.ListAlgorithms;
import omnia.contract.Countable;
import omnia.data.cache.Memoized;
import omnia.data.structure.Collection;
import omnia.data.structure.List;
import omnia.data.structure.Set;
import omnia.data.structure.immutable.ImmutableList;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import tasks.cli.command.FlagOption;
import tasks.cli.command.Option;
import tasks.cli.command.Parameter;
import tasks.model.Task;
import tasks.model.TaskStore;

public final class ParserUtil {

  private ParserUtil() {}

  public static Parser<List<ParseResult<Task>>> taskListParser(
      Memoized<? extends TaskStore> taskStore) {
    return (args) -> parseTaskIds(ImmutableList.copyOf(args), taskStore.value());
  }

  public static List<ParseResult<Task>> parseTaskIds(List<String> userInputs, TaskStore taskStore) {
    return userInputs.stream()
        .map(userInput -> parseTaskId(userInput, taskStore))
        .collect(toList());
  }

  public static ParseResult<Task> parseTaskId(String userInput, TaskStore taskStore) {
    Set<Task> matchingTasks = getTasksMatching(userInput, taskStore);
    if (matchingTasks.count() > 1) {
      return ParseResult.failure(String.format("Ambiguous task ID: multiple tasks match '%s'", userInput));
    }
    return matchingTasks.stream()
        .findFirst()
        .map(ParseResult::success)
        .orElse(
            ParseResult.failure(String.format("Unknown task ID: no tasks match '%s'", userInput)));
  }

  private static Set<Task> getTasksMatching(String userInput, TaskStore taskStore) {
    return taskStore.allTasksMatchingCliPrefix(userInput).blockingFirst();
  }

  public static void validateParsedTasks(Collection<? extends ParseResult<?>> parseResults) {
    generateAggregateFailureMessage(parseResults)
        .ifPresent(message -> {
          throw new ArgumentFormatException("Unable to parse task IDs:\n" + message);
        });
  }

  static Optional<String> generateAggregateFailureMessage(
      Collection<? extends ParseResult<?>> parseResults) {
    return Optional.of(
        parseResults.stream()
            .map(ParseResult::failureMessage)
            .flatMap(Optional::stream)
            .collect(toImmutableList()))
        .filter(Countable::isPopulated)
        .map(failureMessages -> failureMessages.stream().collect(Collectors.joining("\n")));
  }

  public static <T> List<T> extractSuccessfulResults(
      Collection<? extends ParseResult<? extends T>> results) {
    return results.stream().flatMap(result -> result.successResult().stream()).collect(toList());
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
      throw new ArgumentFormatException(
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
      throw new ArgumentFormatException(
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
