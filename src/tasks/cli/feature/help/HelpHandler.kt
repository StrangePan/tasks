package tasks.cli.feature.help

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.util.Comparator
import java.util.Optional
import java.util.stream.Collectors.joining
import java.util.stream.Stream
import omnia.cli.out.Output
import omnia.cli.out.Output.Companion.builder
import omnia.cli.out.Output.Companion.empty
import omnia.data.cache.Memoized
import omnia.data.stream.Collectors.toImmutableSet
import omnia.data.structure.immutable.ImmutableList
import tasks.cli.command.Command
import tasks.cli.command.Commands
import tasks.cli.command.Option
import tasks.cli.command.Parameter
import tasks.cli.command.common.CommonArguments
import tasks.cli.command.common.CommonOptions
import tasks.cli.feature.help.CommandDocumentation.OptionDocumentation
import tasks.cli.handler.ArgumentHandler

/** Business logic for the Help command.  */
class HelpHandler(private val commands: Memoized<out Commands>) : ArgumentHandler<HelpArguments> {

  override fun handle(arguments: CommonArguments<out HelpArguments>): Single<Output> {
    return Single.just(arguments)
        .map { it.specificArguments().mode() }
        .flatMap { mode -> mode.map(::getHelpOutputForMode).orElseGet { helpOutputForSelf } }
  }

  private val helpOutputForSelf: Single<Output>
    get() = helpOutputForCommandListing
        .map { commandsOutput ->
          builder()
              .appendLine("Commands:")
              .appendLine(commandsOutput, 2)
              .build()
        }

  private val helpOutputForCommandListing: Single<Output>
    get() = Single.fromCallable(commands::value)
        .map(Commands::allCommands)
        .flatMapObservable { source -> Observable.fromIterable(source) }
        .map(::toCommandDocumentation)
        .sorted(Comparator.comparing(CommandDocumentation::canonicalName))
        .map {
          ImmutableList.builder<String>()
              .add(it.canonicalName())
              .addAll(it.aliases())
              .build()
        }
        .map { it.stream().collect(joining(", ")) }
        .collect(Output::builder, Output.Builder::appendLine)
        .map(Output.Builder::build)

  private fun getHelpOutputForMode(mode: String): Single<Output> {
    return Single.fromCallable { commands.value() }
        .map { it.getMatchingCommand(mode) }
        .filter { it.isPresent }
        .map { it.get() }
        .map(::toCommandDocumentation)
        .map(::generateOutputFor)
        .switchIfEmpty(helpOutputForSelf)
  }

  companion object {
    private fun toCommandDocumentation(registration: Command): CommandDocumentation {
      return CommandDocumentation(
          registration.canonicalName(),
          ImmutableList.copyOf(registration.aliases()),
          toParameterRepresentation(registration),
          registration.description(),
          registration.options().stream().map(::toOptionDocumentation).collect(toImmutableSet()))
    }

    private fun toParameterRepresentation(registration: Command): Optional<String> {
      return if (registration.parameters().isPopulated) Optional.of(
          registration.parameters().stream()
              .map(::toParameterRepresentation)
              .collect(joining(" "))) else Optional.empty()
    }

    private fun toParameterRepresentation(parameter: Parameter): String {
      return "<${parameter.description()}${if (parameter.isRepeatable()) "..." else ""}>"
    }

    private fun toParameterRepresentation(option: Option): Optional<String> {
      return option.parameterRepresentation().map { "<$it>" }
    }

    private fun toOptionDocumentation(option: Option): OptionDocumentation {
      return OptionDocumentation(
          option.longName(),
          option.shortName(),
          option.description(),
          option.isRepeatable(),
          toParameterRepresentation(option))
    }

    private fun generateOutputFor(documentation: CommandDocumentation): Output {
      return builder()
          .appendLine(headerLine(documentation))
          .appendLine(aliasesLine(documentation))
          .appendLine(usageLine(documentation))
          .appendLine(documentation.description(), 2)
          .appendLine(prependWithBlankLine(parameterLines(documentation)))
          .appendLine(prependWithBlankLine(documentationCommonToAllCommands()))
          .build()
    }

    private fun prependWithBlankLine(output: Output): Output {
      return if (output.isPopulated) builder().appendLine().appendLine(output).build() else empty()
    }

    private fun headerLine(documentation: CommandDocumentation): Output {
      return builder().append("Command: ").append(documentation.canonicalName()).build()
    }

    private fun aliasesLine(documentation: CommandDocumentation): Output {
      return if (documentation.aliases().isPopulated) builder()
          .append("Aliases: ")
          .append(documentation.aliases().stream().collect(joining(", ")))
          .build() else empty()
    }

    private fun usageLine(documentation: CommandDocumentation): Output {
      return builder().append("Usage:   ")
          .append(documentation.canonicalName())
          .append(documentation.parameterRepresentation().map { " $it" }.orElse(""))
          .append(if (documentation.options().isPopulated) " [options...]" else "")
          .build()
    }

    private fun documentationCommonToAllCommands(): Output {
      return builder()
          .appendLine("Options common to all commands")
          .appendLine()
          .appendLine(
              Single.fromCallable(CommonOptions.OPTIONS::value)
                  .flattenAsObservable { it }
                  .map(::toOptionDocumentation)
                  .map(::parameterLine)
                  .collect(Output::builder, Output.Builder::appendLine)
                  .map(Output.Builder::build)
                  .blockingGet())
          .build()
    }

    private fun parameterLines(documentation: CommandDocumentation): Output {
      return Observable.fromIterable(documentation.options())
          .sorted(Comparator.comparing(OptionDocumentation::canonicalName))
          .map(::parameterLine)
          .collect(Output::builder, Output.Builder::appendLine)
          .map(Output.Builder::build)
          .blockingGet()
    }

    private fun parameterLine(documentation: OptionDocumentation): Output {
      return builder()
          .appendLine(
              Stream.concat(
                  Stream.of("--" + documentation.canonicalName()),
                  documentation.shortFlag().stream().map { "-$it" })
                  .collect(joining(","))
                  + documentation.parameterRepresentation().map { " $it" }.orElse("")
                  + if (documentation.isRepeatable) " [+]" else "",
              2)
          .appendLine(documentation.description(), 4)
          .build()
    }
  }

}