package me.strangepan.tasks.cli.parser

import io.reactivex.rxjava3.core.Observable
import java.util.Optional
import java.util.OptionalInt
import java.util.stream.Collectors.joining
import omnia.data.cache.Memoized
import omnia.data.stream.Collectors.toList
import omnia.data.structure.Collection
import omnia.data.structure.List
import omnia.data.structure.List.Companion.masking
import omnia.data.structure.immutable.ImmutableList
import omnia.data.structure.tuple.Couple
import omnia.data.structure.tuple.Tuple
import org.apache.commons.cli.CommandLine
import me.strangepan.tasks.cli.command.FlagOption
import me.strangepan.tasks.cli.command.Option
import me.strangepan.tasks.cli.command.Parameter
import me.strangepan.tasks.engine.model.ObservableTaskStore
import me.strangepan.tasks.engine.model.Task
import me.strangepan.tasks.engine.model.TaskStore

object ParserUtil {
  fun taskListParser(
      taskStore: Memoized<out ObservableTaskStore>): Parser<List<ParseResult<out Task>>> {
    return object : Parser<List<ParseResult<out Task>>> {
      override fun parse(commandLine: List<out String>): List<ParseResult<out Task>> {
        return parseTaskIds(ImmutableList.copyOf(commandLine), taskStore.value())
      }
    }
  }

  fun parseTaskIds(
      userInputs: List<String>, observableTaskStore: ObservableTaskStore): List<ParseResult<out Task>> {
    return observableTaskStore.observe()
        .firstOrError()
        .map { taskStore ->
          userInputs.stream().map { parseTaskId(it, taskStore) }.collect(toList())
        }
        .blockingGet()
  }

  private fun parseTaskId(userInput: String, taskStore: TaskStore): ParseResult<out Task> {
    val matchingTasks = taskStore.allTasksMatchingCliPrefix(userInput)
    return if (matchingTasks.count() > 1) {
      ParseResult.failure("Ambiguous task ID: multiple me.strangepan.tasks.engine.tasks match '$userInput'")
    } else matchingTasks.stream()
        .findFirst()
        .map { ParseResult.success(it) }
        .orElseGet { ParseResult.failure("Unknown task ID: no me.strangepan.tasks.engine.tasks match '$userInput'") }
  }

  fun <T : Any> extractSuccessfulResultOrThrow(parseResult: ParseResult<out T>): T {
    return extractSuccessfulResultsOrThrow(ImmutableList.of(parseResult)).itemAt(0)
  }

  fun <T : Any> extractSuccessfulResultsOrThrow(
      parseResults: Collection<out ParseResult<out T>>): ImmutableList<T> {
    return Observable.fromIterable(parseResults)
        .collect<Couple<ImmutableList.Builder<T>, ImmutableList.Builder<String>>>(
            { Tuple.of(ImmutableList.builder(), ImmutableList.builder()) },
            { couple, result ->
              couple
                  .mapFirst { maybeAdd(it, result.successResult) }
                  .mapSecond { maybeAdd(it, result.failureMessage) }
            })
        .map { couple ->
          couple
              .mapFirst { it.build() }
              .mapSecond { it.build() }
        }
        .doOnSuccess { throwIfPopulated(it.second()) }
        .map { it.first() }
        .blockingGet()
  }

  private fun <T> maybeAdd(
      builder: ImmutableList.Builder<T>, item: Optional<out T>): ImmutableList.Builder<T> {
    item.ifPresent { builder.add(it) }
    return builder
  }

  private fun throwIfPopulated(failureMessages: List<out String>) {
    if (failureMessages.isPopulated) {
      throw ParserException(
          "Unable to parse task IDs:" +
              " ${failureMessages.stream().collect(joining(",", "[", "]"))}")
    }
  }

  fun getFlagPresence(commandLine: CommandLine, flagOption: FlagOption): Boolean {
    return commandLine.hasOption(flagOption.shortName().orElse(flagOption.longName()))
  }

  fun getOptionValues(commandLine: CommandLine, option: Option): List<String> {
    return getOptionValues(commandLine, option.shortName().orElse(option.longName()))
  }

  fun getOptionValues(commandLine: CommandLine, opt: String): List<String> {
    return ImmutableList.copyOf(
        Optional.ofNullable(commandLine.getOptionValues(opt)).orElse(arrayOfNulls(0)))
  }

  fun getSingleOptionValue(commandLine: CommandLine, option: Option): Optional<String> {
    return getSingleOptionValue(commandLine, option.shortName().orElse(option.longName()))
  }

  fun getSingleOptionValue(commandLine: CommandLine, opt: String): Optional<String> {
    if (Optional.ofNullable(commandLine.getOptionValues(opt)).orElse(arrayOfNulls(0)).size > 1) {
      throw ParserException("Too many values provided for parameter '$opt'")
    }
    return Optional.ofNullable(commandLine.getOptionValue(opt))
  }

  fun assertNoExtraArgs(commandLine: CommandLine) {
    assertNoExtraArgs(masking(commandLine.argList), 0)
  }

  fun assertNoExtraArgs(
      commandLine: CommandLine, parameters: Collection<Parameter>) {
    assertNoExtraArgs(masking(commandLine.argList), parameters)
  }

  fun assertNoExtraArgs(
      args: List<out String>, parameters: Collection<Parameter>) {
    computeMaxNumberOfCommandParameters(parameters).ifPresent { assertNoExtraArgs(args, it) }
  }

  private fun assertNoExtraArgs(
      args: List<out String>, maxNumberOfCommandParameters: Int) {
    if (args.count() - 1 > maxNumberOfCommandParameters) {
      throw ParserException("Unexpected arguments given: ${
        args.stream()
            .skip(maxNumberOfCommandParameters.toLong())
            .map { "'$it'" }
            .collect(joining(", "))
      }")
    }
  }

  fun computeMaxNumberOfCommandParameters(
      parameters: Collection<Parameter>): OptionalInt {
    return if (parameters.stream().anyMatch(Parameter::isRepeatable)) OptionalInt.empty() else OptionalInt.of(parameters.count())
  }
}