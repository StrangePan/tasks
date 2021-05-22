package me.strangepan.tasks.cli.parser

import java.util.stream.Collectors.joining
import java.util.stream.Stream
import omnia.data.cache.Memoized
import omnia.data.stream.Collectors.toList
import omnia.data.structure.Collection
import omnia.data.structure.List
import omnia.data.structure.List.Companion.masking
import omnia.data.structure.immutable.ImmutableList
import org.apache.commons.cli.CommandLine
import me.strangepan.tasks.cli.command.FlagOption
import me.strangepan.tasks.cli.command.Option
import me.strangepan.tasks.cli.command.Parameter
import me.strangepan.tasks.cli.model.allTasksMatchingCliPrefix
import me.strangepan.tasks.engine.model.ObservableTaskStore
import me.strangepan.tasks.engine.model.Task
import me.strangepan.tasks.engine.model.TaskStore
import omnia.data.stream.Collectors.toImmutableList

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
        .map {
            taskStore ->
                userInputs.stream().map { parseTaskId(it, taskStore) }.collect(toList())
        }
        .blockingGet()
  }

  private fun parseTaskId(userInput: String, taskStore: TaskStore): ParseResult<out Task> {
    val matchingTasks = taskStore.allTasksMatchingCliPrefix(userInput)
    return if (matchingTasks.count() > 1) {
      ParseResult.failure("Ambiguous task ID: multiple tasks match '$userInput'")
    } else matchingTasks.stream()
        .findFirst()
        .map { ParseResult.success(it) }
        .orElseGet { ParseResult.failure("Unknown task ID: no tasks match '$userInput'") }
  }

  fun <T : Any> extractSuccessfulResultOrThrow(parseResult: ParseResult<out T>): T {
    return extractSuccessfulResultsOrThrow(ImmutableList.of(parseResult)).itemAt(0)
  }

  fun <T : Any> extractSuccessfulResultsOrThrow(
      parseResults: Collection<out ParseResult<out T>>): ImmutableList<T> {
    throwIfPopulated(
      parseResults.stream()
        .flatMap{ result -> result.failureMessage?.let { Stream.of(it) } ?: Stream.empty() }
        .collect(toImmutableList()))

    return parseResults.stream()
      .map { it.successResult!! }
      .collect(toImmutableList())
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

  private fun getOptionValues(commandLine: CommandLine, opt: String): List<String> {
    return ImmutableList.copyOf(commandLine.getOptionValues(opt) ?: emptyArray())
  }

  fun assertNoExtraArgs(commandLine: CommandLine) {
    assertNoExtraArgs(masking(commandLine.argList), 0)
  }

  fun assertNoExtraArgs(
      commandLine: CommandLine, parameters: Collection<Parameter>) {
    assertNoExtraArgs(masking(commandLine.argList), parameters)
  }

  private fun assertNoExtraArgs(
      args: List<out String>, parameters: Collection<Parameter>) {
    computeMaxNumberOfCommandParameters(parameters)?.let { assertNoExtraArgs(args, it) }
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

  private fun computeMaxNumberOfCommandParameters(parameters: Collection<Parameter>): Int? {
    return if (parameters.stream().anyMatch(Parameter::isRepeatable)) null else parameters.count()
  }
}