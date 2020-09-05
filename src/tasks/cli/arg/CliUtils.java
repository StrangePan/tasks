package tasks.cli.arg;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static omnia.data.stream.Collectors.toImmutableList;
import static omnia.data.stream.Collectors.toList;

import io.reactivex.Observable;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;
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
import tasks.cli.arg.registration.FlagOption;
import tasks.cli.arg.registration.Option;
import tasks.cli.arg.registration.Parameter;
import tasks.model.Task;
import tasks.model.TaskStore;

public final class CliUtils {
  private CliUtils() {}

  public static CommandLine tryParse(List<? extends String> args, Options options) {
    try {
      return new DefaultParser().parse(options, toArray(args), /* stopAtNonOption= */ false);
    } catch (ParseException e) {
      throw new CliArguments.ArgumentFormatException("Unable to parse arguments: " + e.getMessage(), e);
    }
  }

  private static String[] toArray(List<? extends String> list) {
    String[] array = new String[list.count()];
    for (int i = 0; i < list.count(); i++) {
      array[i] = list.itemAt(i);
    }
    return array;
  }

  public static CliArguments.Parser<List<ParseResult<Task>>> taskListParser(Memoized<TaskStore> taskStore) {
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
          throw new CliArguments.ArgumentFormatException("Unable to parse task IDs:\n" + message);
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

  public static List<Task> extractTasksFrom(List<ParseResult<Task>> tasks) {
    return tasks.stream().flatMap(result -> result.successResult().stream()).collect(toList());
  }

  public static final class ParseResult<T> {
    private final Optional<T> successResult;
    private final Optional<String> failureMessage;

    private static <T> ParseResult<T> success(T result) {
      return new ParseResult<>(Optional.of(result), Optional.empty());
    }

    private static <T> ParseResult<T> failure(String message) {
      return new ParseResult<>(Optional.empty(), Optional.of(message));
    }

    private ParseResult(Optional<T> successResult, Optional<String> failureMessage) {
      this.successResult = requireNonNull(successResult);
      this.failureMessage = requireNonNull(failureMessage);
    }

    public Optional<T> successResult() {
      return successResult;
    }

    Optional<String> failureMessage() {
      return failureMessage;
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
      throw new CliArguments.ArgumentFormatException(
          String.format("Too many values provided for parameter '%s'", opt));
    }
    return Optional.ofNullable(commandLine.getOptionValue(opt));
  }

  public static Options toOptions(Iterable<? extends Option> options) {
    return Observable.fromIterable(options)
        .map(Option::toCliOption)
        .collect(Options::new, Options::addOption)
        .blockingGet();
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
      throw new CliArguments.ArgumentFormatException(
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
