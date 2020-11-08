package tasks.cli.bindings;

import static java.util.Objects.requireNonNull;
import static omnia.data.cache.Memoized.memoize;
import static omnia.data.stream.Collectors.toImmutableSet;

import java.util.Optional;
import java.util.function.Function;
import omnia.data.cache.Memoized;
import omnia.data.stream.Collectors;
import omnia.data.structure.Collection;
import omnia.data.structure.List;
import omnia.data.structure.Map;
import omnia.data.structure.Set;
import omnia.data.structure.immutable.ImmutableMap;
import omnia.data.structure.immutable.ImmutableSet;
import omnia.data.structure.tuple.Couple;
import omnia.data.structure.tuple.Tuple;
import tasks.cli.command.Command;
import tasks.cli.command.Commands;
import tasks.cli.feature.add.AddCommand;
import tasks.cli.feature.add.AddHandler;
import tasks.cli.feature.add.AddParser;
import tasks.cli.feature.blockers.BlockersCommand;
import tasks.cli.feature.blockers.BlockersHandler;
import tasks.cli.feature.blockers.BlockersParser;
import tasks.cli.feature.complete.CompleteCommand;
import tasks.cli.feature.complete.CompleteHandler;
import tasks.cli.feature.complete.CompleteParser;
import tasks.cli.feature.graph.GraphCommand;
import tasks.cli.feature.graph.GraphHandler;
import tasks.cli.feature.graph.GraphParser;
import tasks.cli.command.help.HelpCommand;
import tasks.cli.command.help.HelpHandler;
import tasks.cli.command.help.HelpParser;
import tasks.cli.command.info.InfoCommand;
import tasks.cli.command.info.InfoHandler;
import tasks.cli.command.info.InfoParser;
import tasks.cli.command.list.ListCommand;
import tasks.cli.command.list.ListHandler;
import tasks.cli.command.list.ListParser;
import tasks.cli.command.remove.RemoveCommand;
import tasks.cli.command.remove.RemoveHandler;
import tasks.cli.command.remove.RemoveParser;
import tasks.cli.command.reopen.ReopenCommand;
import tasks.cli.command.reopen.ReopenHandler;
import tasks.cli.command.reopen.ReopenParser;
import tasks.cli.command.reword.RewordCommand;
import tasks.cli.command.reword.RewordHandler;
import tasks.cli.command.reword.RewordParser;
import tasks.cli.parser.Parser;
import tasks.cli.parser.ParserUtil;
import tasks.cli.parser.ParseResult;
import tasks.model.Task;
import tasks.model.TaskStore;

public final class Features implements Commands {

  private final Feature<?> helpFeature;
  private final Map<String, Feature<?>> featuresByNameAndAliases;
  private final ImmutableSet<Command> commands;

  public Features(Memoized<? extends TaskStore> taskStore) {
    Memoized<Parser<List<ParseResult<Task>>>> taskListParser =
        memoize(() -> ParserUtil.taskListParser(taskStore));

    helpFeature =
        new Feature<>(
            HelpCommand.registration(),
            HelpParser::new,
            () -> new HelpHandler(Memoized.just(this)));

    Set<Feature<?>> features =
        ImmutableSet.of(
            new Feature<>(
                AddCommand.registration(),
                () -> new AddParser(taskListParser),
                () -> new AddHandler(taskStore)),
            new Feature<>(
                BlockersCommand.registration(),
                () -> new BlockersParser(taskListParser),
                () -> new BlockersHandler(taskStore)),
            new Feature<>(
                CompleteCommand.registration(),
                () -> new CompleteParser(taskListParser),
                () -> new CompleteHandler(taskStore)),
            new Feature<>(
                GraphCommand.registration(),
                GraphParser::new,
                () -> new GraphHandler(taskStore)),
            helpFeature,
            new Feature<>(
                InfoCommand.registration(),
                () -> new InfoParser(taskListParser),
                InfoHandler::new),
            new Feature<>(
                ListCommand.registration(),
                ListParser::new,
                () -> new ListHandler(taskStore)),
            new Feature<>(
                RemoveCommand.registration(),
                () -> new RemoveParser(taskListParser),
                () -> new RemoveHandler(taskStore)),
            new Feature<>(
                ReopenCommand.registration(),
                () -> new ReopenParser(taskListParser),
                () -> new ReopenHandler(taskStore)),
            new Feature<>(
                RewordCommand.registration(),
                () -> new RewordParser(taskListParser),
                () -> new RewordHandler(taskStore)));

    commands = features.stream().map(Feature::command).collect(toImmutableSet());

    featuresByNameAndAliases = groupByCommandNameAndAlias(features, Feature::command);
  }

  private static <V> ImmutableMap<String, V> groupByCommandNameAndAlias(
      Collection<? extends V> collection, Function<? super V, ? extends Command> commandExtractor) {
    return collection.stream()
        .flatMap(
            value ->
                commandExtractor.apply(value)
                    .canonicalNameAndAliases()
                    .stream()
                    .map(alias -> Tuple.of(alias, value)))
        .collect(Collectors.toImmutableMap(Couple::first, Couple::second));
  }

  @Override
  public ImmutableSet<Command> getAllCommands() {
    return commands;
  }

  @Override
  public Optional<Command> getMatchingCommand(String userInput) {
    return getMatchingFeature(userInput).map(Feature::command);
  }

  public Feature<?> getMatchingOrFallbackFeature(String userInput) {
    return getMatchingFeature(userInput).orElseGet(this::getFallbackFeature);
  }

  public Feature<?> getFallbackFeature() {
    return helpFeature;
  }

  public Optional<Feature<?>> getMatchingFeature(String userInput) {
    requireNonNull(userInput);
    return featuresByNameAndAliases.valueOf(userInput);
  }
}
