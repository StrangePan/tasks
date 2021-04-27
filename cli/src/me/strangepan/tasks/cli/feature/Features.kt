package me.strangepan.tasks.cli.feature

import java.util.Objects
import java.util.Optional
import java.util.function.Function
import omnia.data.cache.Memoized
import omnia.data.cache.Memoized.Companion.just
import omnia.data.cache.Memoized.Companion.memoize
import omnia.data.stream.Collectors.toImmutableMap
import omnia.data.stream.Collectors.toImmutableSet
import omnia.data.structure.Collection
import omnia.data.structure.List
import omnia.data.structure.Map
import omnia.data.structure.Set
import omnia.data.structure.immutable.ImmutableMap
import omnia.data.structure.immutable.ImmutableSet
import omnia.data.structure.tuple.Tuple
import me.strangepan.tasks.cli.command.Command
import me.strangepan.tasks.cli.command.Commands
import me.strangepan.tasks.cli.feature.add.AddCommand
import me.strangepan.tasks.cli.feature.add.AddHandler
import me.strangepan.tasks.cli.feature.add.AddParser
import me.strangepan.tasks.cli.feature.blockers.BlockersCommand
import me.strangepan.tasks.cli.feature.blockers.BlockersHandler
import me.strangepan.tasks.cli.feature.blockers.BlockersParser
import me.strangepan.tasks.cli.feature.complete.CompleteCommand
import me.strangepan.tasks.cli.feature.complete.CompleteHandler
import me.strangepan.tasks.cli.feature.complete.CompleteParser
import me.strangepan.tasks.cli.feature.graph.GraphCommand
import me.strangepan.tasks.cli.feature.graph.GraphHandler
import me.strangepan.tasks.cli.feature.graph.GraphParser
import me.strangepan.tasks.cli.feature.help.HelpCommand
import me.strangepan.tasks.cli.feature.help.HelpHandler
import me.strangepan.tasks.cli.feature.help.HelpParser
import me.strangepan.tasks.cli.feature.info.InfoCommand
import me.strangepan.tasks.cli.feature.info.InfoHandler
import me.strangepan.tasks.cli.feature.info.InfoParser
import me.strangepan.tasks.cli.feature.list.ListCommand
import me.strangepan.tasks.cli.feature.list.ListHandler
import me.strangepan.tasks.cli.feature.list.ListParser
import me.strangepan.tasks.cli.feature.remove.RemoveCommand
import me.strangepan.tasks.cli.feature.remove.RemoveHandler
import me.strangepan.tasks.cli.feature.remove.RemoveParser
import me.strangepan.tasks.cli.feature.reopen.ReopenCommand
import me.strangepan.tasks.cli.feature.reopen.ReopenHandler
import me.strangepan.tasks.cli.feature.reopen.ReopenParser
import me.strangepan.tasks.cli.feature.reword.RewordCommand
import me.strangepan.tasks.cli.feature.reword.RewordHandler
import me.strangepan.tasks.cli.feature.reword.RewordParser
import me.strangepan.tasks.cli.feature.start.StartCommand
import me.strangepan.tasks.cli.feature.start.StartHandler
import me.strangepan.tasks.cli.feature.start.StartParser
import me.strangepan.tasks.cli.feature.stop.StopCommand
import me.strangepan.tasks.cli.feature.stop.StopHandler
import me.strangepan.tasks.cli.feature.stop.StopParser
import me.strangepan.tasks.cli.input.Reader
import me.strangepan.tasks.cli.output.Printer
import me.strangepan.tasks.cli.parser.ParseResult
import me.strangepan.tasks.cli.parser.Parser
import me.strangepan.tasks.cli.parser.ParserUtil
import me.strangepan.tasks.engine.model.ObservableTaskStore
import me.strangepan.tasks.engine.model.Task

class Features(taskStore: Memoized<out ObservableTaskStore>) : Commands {
  val fallbackFeature: Feature<*>
  private val featuresByNameAndAliases: Map<String, Feature<*>>
  override val allCommands: ImmutableSet<Command>
  private val printerFactory = memoize { Printer.Factory() }
  private val reader: Memoized<Reader> = memoize { Reader.forInputStream(System.`in`) }

  override fun getMatchingCommand(userInput: String): Optional<Command> {
    return getMatchingFeature(userInput).map { it.command }
  }

  fun getMatchingOrFallbackFeature(userInput: String): Feature<*> {
    return getMatchingFeature(userInput).orElseGet { fallbackFeature }
  }

  private fun getMatchingFeature(userInput: String): Optional<Feature<*>> {
    Objects.requireNonNull(userInput)
    return featuresByNameAndAliases.valueOf(userInput)
  }

  companion object {
    private fun <V> groupByCommandNameAndAlias(
        collection: Collection<out V>, commandExtractor: Function<in V, out Command>): ImmutableMap<String, V> {
      return collection.stream()
          .flatMap { value ->
            commandExtractor.apply(value)
                .canonicalNameAndAliases()
                .stream()
                .map { alias -> Tuple.of(alias, value) }
          }
          .collect(toImmutableMap())
    }
  }

  init {
    val taskListParser: Memoized<Parser<List<ParseResult<out Task>>>> = memoize { ParserUtil.taskListParser(taskStore) }
    fallbackFeature = Feature(
        HelpCommand.registration(),
        { HelpParser() },
        { HelpHandler(just(this)) })
    val features: Set<Feature<*>> = ImmutableSet.of(
        Feature(
            AddCommand.registration(),
            { AddParser(taskListParser) },
            { AddHandler(taskStore) }),
        Feature(
            BlockersCommand.registration(),
            { BlockersParser(taskListParser) },
            { BlockersHandler(taskStore) }),
        Feature(
            CompleteCommand.registration(),
            { CompleteParser(taskListParser) },
            { CompleteHandler(taskStore) }),
        Feature(
            GraphCommand.registration(), { GraphParser(taskListParser) }, { GraphHandler(taskStore) }),
        fallbackFeature,
        Feature(
            InfoCommand.registration(),
            { InfoParser(taskListParser) },
            { InfoHandler() }),
        Feature(
            ListCommand.registration(), { ListParser() }, { ListHandler(taskStore) }),
        Feature(
            RemoveCommand.registration(),
            { RemoveParser(taskListParser) },
            { RemoveHandler(taskStore, printerFactory.value(), reader) }),
        Feature(
            ReopenCommand.registration(),
            { ReopenParser(taskListParser) },
            { ReopenHandler(taskStore) }),
        Feature(
            RewordCommand.registration(),
            { RewordParser(taskListParser) },
            { RewordHandler(taskStore) }),
        Feature(
            StartCommand.registration(),
            { StartParser(taskListParser) },
            { StartHandler(taskStore) }),
        Feature(
            StopCommand.registration(),
            { StopParser(taskListParser) },
            { StopHandler(taskStore) }))
    allCommands = features.stream().map(Feature<*>::command).collect(toImmutableSet())
    featuresByNameAndAliases = groupByCommandNameAndAlias(features, Feature<*>::command)
  }
}