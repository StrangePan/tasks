package tasks.cli.feature

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
import tasks.cli.command.Command
import tasks.cli.command.Commands
import tasks.cli.feature.add.AddCommand
import tasks.cli.feature.add.AddHandler
import tasks.cli.feature.add.AddParser
import tasks.cli.feature.blockers.BlockersCommand
import tasks.cli.feature.blockers.BlockersHandler
import tasks.cli.feature.blockers.BlockersParser
import tasks.cli.feature.complete.CompleteCommand
import tasks.cli.feature.complete.CompleteHandler
import tasks.cli.feature.complete.CompleteParser
import tasks.cli.feature.graph.GraphCommand
import tasks.cli.feature.graph.GraphHandler
import tasks.cli.feature.graph.GraphParser
import tasks.cli.feature.help.HelpCommand
import tasks.cli.feature.help.HelpHandler
import tasks.cli.feature.help.HelpParser
import tasks.cli.feature.info.InfoCommand
import tasks.cli.feature.info.InfoHandler
import tasks.cli.feature.info.InfoParser
import tasks.cli.feature.list.ListCommand
import tasks.cli.feature.list.ListHandler
import tasks.cli.feature.list.ListParser
import tasks.cli.feature.remove.RemoveCommand
import tasks.cli.feature.remove.RemoveHandler
import tasks.cli.feature.remove.RemoveParser
import tasks.cli.feature.reopen.ReopenCommand
import tasks.cli.feature.reopen.ReopenHandler
import tasks.cli.feature.reopen.ReopenParser
import tasks.cli.feature.reword.RewordCommand
import tasks.cli.feature.reword.RewordHandler
import tasks.cli.feature.reword.RewordParser
import tasks.cli.feature.start.StartCommand
import tasks.cli.feature.start.StartHandler
import tasks.cli.feature.start.StartParser
import tasks.cli.feature.stop.StopCommand
import tasks.cli.feature.stop.StopHandler
import tasks.cli.feature.stop.StopParser
import tasks.cli.input.Reader
import tasks.cli.output.Printer
import tasks.cli.parser.ParseResult
import tasks.cli.parser.Parser
import tasks.cli.parser.ParserUtil
import tasks.model.ObservableTaskStore
import tasks.model.Task

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