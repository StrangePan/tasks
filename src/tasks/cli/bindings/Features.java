package tasks.cli.bindings;

import static omnia.data.cache.Memoized.memoize;

import omnia.data.cache.Memoized;
import omnia.data.structure.List;
import omnia.data.structure.Map;
import omnia.data.structure.Set;
import omnia.data.structure.immutable.ImmutableSet;
import tasks.cli.command.add.AddCommand;
import tasks.cli.command.add.AddHandler;
import tasks.cli.command.add.AddParser;
import tasks.cli.command.blockers.BlockersCommand;
import tasks.cli.command.blockers.BlockersHandler;
import tasks.cli.command.blockers.BlockersParser;
import tasks.cli.command.complete.CompleteCommand;
import tasks.cli.command.complete.CompleteHandler;
import tasks.cli.command.complete.CompleteParser;
import tasks.cli.command.graph.GraphCommand;
import tasks.cli.command.graph.GraphHandler;
import tasks.cli.command.graph.GraphParser;
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

public final class Features {

  private final Memoized<CommandsImpl> commands = memoize(CommandsImpl::new);
  private final Feature<?> helpFeature =
      new Feature<>(HelpCommand.registration(), HelpParser::new, () -> new HelpHandler(commands));
  private final Map<String, Feature<?>> featuresByNameAndAliases;

  public Features(Memoized<? extends TaskStore> taskStore) {
    Memoized<Parser<List<ParseResult<Task>>>> taskListParser =
        memoize(() -> ParserUtil.taskListParser(taskStore));

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

    featuresByNameAndAliases = CommandsImpl.groupByCommandNameAndAlias(features, Feature::command);
  }

  public Feature<?> getMatchingOrFallbackFeature(String userInput) {
    return featuresByNameAndAliases.valueOf(userInput).orElse(helpFeature);
  }

  public Feature<?> getFallbackFeature() {
    return helpFeature;
  }
}
