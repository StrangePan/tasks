package tasks.cli.command.graph;

import static omnia.data.cache.Memoized.memoize;

import omnia.data.cache.Memoized;
import omnia.data.structure.immutable.ImmutableList;
import tasks.cli.command.Command;
import tasks.cli.command.FlagOption;
import tasks.cli.command.list.ListCommand;

public final class GraphCommand {
  private GraphCommand() {}

  public static Command registration() {
    return COMMAND.value();
  }

  private static final Memoized<Command> COMMAND =
      memoize(
          () -> Command.builder()
              .canonicalName("graph")
              .aliases("xl")
              .parameters(ListCommand.registration().parameters())
              .options(ImmutableList.empty())
              .helpDocumentation("Prints all tasks in graph format."));
}
