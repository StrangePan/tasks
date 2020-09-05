package tasks.cli.bindings;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import java.util.function.Function;
import omnia.data.stream.Collectors;
import omnia.data.structure.Collection;
import omnia.data.structure.Map;
import omnia.data.structure.Set;
import omnia.data.structure.immutable.ImmutableMap;
import omnia.data.structure.immutable.ImmutableSet;
import omnia.data.structure.tuple.Couple;
import omnia.data.structure.tuple.Tuple;
import tasks.cli.command.Command;
import tasks.cli.command.Commands;
import tasks.cli.command.add.AddCommand;
import tasks.cli.command.blockers.BlockersCommand;
import tasks.cli.command.complete.CompleteCommand;
import tasks.cli.command.help.HelpCommand;
import tasks.cli.command.info.InfoCommand;
import tasks.cli.command.list.ListCommand;
import tasks.cli.command.remove.RemoveCommand;
import tasks.cli.command.reopen.ReopenCommand;
import tasks.cli.command.reword.RewordCommand;

public final class CommandsImpl implements Commands {
  private final Set<Command> commands =
      ImmutableSet.of(
          AddCommand.registration(),
          BlockersCommand.registration(),
          CompleteCommand.registration(),
          HelpCommand.registration(),
          InfoCommand.registration(),
          ListCommand.registration(),
          RemoveCommand.registration(),
          ReopenCommand.registration(),
          RewordCommand.registration());

  private final Map<String, Command> commandsByNameAndAlias =
      groupByCommandNameAndAlias(commands, Function.identity());

  @Override
  public Collection<Command> getAllRegisteredCommands() {
    return commands;
  }

  @Override
  public Optional<Command> getCommandMatching(String userInput) {
    requireNonNull(userInput);
    return commandsByNameAndAlias.valueOf(userInput);
  }

  static <V> ImmutableMap<String, V> groupByCommandNameAndAlias(
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
}
